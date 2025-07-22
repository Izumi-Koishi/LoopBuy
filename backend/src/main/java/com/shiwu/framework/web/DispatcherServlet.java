package com.shiwu.framework.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shiwu.framework.annotation.*;
import com.shiwu.framework.core.ApplicationContext;
import com.shiwu.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求分发器 - MVC框架的核心，负责请求路由和方法调用
 *
 * 支持功能：
 * - 自动路由分发
 * - 路径参数提取 (@PathVariable)
 * - 请求参数绑定 (@RequestParam)
 * - JSON自动序列化/反序列化
 * - 统一异常处理
 */
public class DispatcherServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);

    private ApplicationContext applicationContext;
    private ObjectMapper objectMapper;

    // 路由映射表：RouteKey -> RouteInfo
    private final Map<String, RouteInfo> routeMappings = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // 初始化IoC容器
        applicationContext = new ApplicationContext("com.shiwu");
        
        // 初始化JSON处理器
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // 扫描并注册所有请求映射
        registerHandlerMappings();
        
        logger.info("✅ DispatcherServlet初始化完成，注册{}个路由", routeMappings.size());
    }

    /**
     * 扫描并注册所有Controller的请求映射
     */
    private void registerHandlerMappings() {
        Map<String, Object> controllers = applicationContext.getControllers();

        for (Object controller : controllers.values()) {
            Class<?> controllerClass = controller.getClass();

            // 获取类级别的@RequestMapping
            String baseUrl = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);
                baseUrl = classMapping.value();
            }

            // 扫描所有方法
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);

                    String fullUrl = baseUrl + methodMapping.value();
                    String httpMethod = methodMapping.method();

                    // 创建RouteInfo对象
                    RouteInfo routeInfo = new RouteInfo(controller, method, httpMethod, fullUrl);
                    String routeKey = routeInfo.getRouteKey();

                    routeMappings.put(routeKey, routeInfo);

                    logger.debug("注册路由: {} -> {}.{}", routeKey,
                        controllerClass.getSimpleName(), method.getName());
                }
            }
        }
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();

        logger.debug("处理请求: {} {}", httpMethod, requestURI);

        try {
            // 查找匹配的路由
            RouteInfo matchedRoute = findMatchingRoute(httpMethod, requestURI);

            if (matchedRoute != null) {
                // 找到对应的处理方法，执行调用
                Object result = invokeHandler(matchedRoute, request, response, requestURI);

                // 处理返回结果
                if (result != null) {
                    sendJsonResponse(response, result);
                }
            } else {
                // 没有找到对应的处理器
                sendErrorResponse(response, 404, "接口不存在");
            }

        } catch (Exception e) {
            logger.error("处理请求失败: {} {}", httpMethod, requestURI, e);
            sendErrorResponse(response, 500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 查找匹配的路由
     * 支持精确匹配和路径参数匹配
     */
    private RouteInfo findMatchingRoute(String httpMethod, String requestURI) {
        // 1. 先尝试精确匹配
        String exactKey = httpMethod + ":" + requestURI;
        RouteInfo exactMatch = routeMappings.get(exactKey);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 2. 尝试路径参数匹配
        for (RouteInfo routeInfo : routeMappings.values()) {
            if (routeInfo.getHttpMethod().equals(httpMethod) && routeInfo.matches(requestURI)) {
                return routeInfo;
            }
        }

        return null;
    }
    
    /**
     * 调用处理方法
     * 支持@PathVariable、@RequestParam注解的参数绑定
     */
    private Object invokeHandler(RouteInfo routeInfo, HttpServletRequest request,
                                HttpServletResponse response, String requestURI) throws Exception {

        Method method = routeInfo.getMethod();
        Object controller = routeInfo.getController();

        // 提取路径参数
        Map<String, String> pathParams = routeInfo.extractPathParams(requestURI);

        // 获取方法参数
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (paramType == HttpServletRequest.class) {
                args[i] = request;
            } else if (paramType == HttpServletResponse.class) {
                args[i] = response;
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                // 处理@PathVariable注解
                args[i] = handlePathVariable(param, pathParams);
            } else if (param.isAnnotationPresent(RequestParam.class)) {
                // 处理@RequestParam注解
                args[i] = handleRequestParam(param, request);
            } else {
                // 尝试从请求体解析JSON参数
                args[i] = parseRequestBody(request, paramType);
            }
        }

        method.setAccessible(true);
        return method.invoke(controller, args);
    }

    /**
     * 处理@PathVariable注解的参数
     */
    private Object handlePathVariable(Parameter param, Map<String, String> pathParams) {
        PathVariable pathVar = param.getAnnotation(PathVariable.class);
        String paramName = pathVar.value().isEmpty() ? param.getName() : pathVar.value();
        String value = pathParams.get(paramName);

        return convertParameter(value, param.getType());
    }

    /**
     * 处理@RequestParam注解的参数
     */
    private Object handleRequestParam(Parameter param, HttpServletRequest request) {
        RequestParam reqParam = param.getAnnotation(RequestParam.class);
        String paramName = reqParam.value().isEmpty() ? param.getName() : reqParam.value();
        String value = request.getParameter(paramName);

        // 处理默认值
        if (value == null && !reqParam.defaultValue().isEmpty()) {
            value = reqParam.defaultValue();
        }

        // 处理必需参数
        if (value == null && reqParam.required()) {
            throw new IllegalArgumentException("缺少必需参数: " + paramName);
        }

        return convertParameter(value, param.getType());
    }

    /**
     * 参数类型转换
     */
    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数类型转换失败: " + value + " -> " + targetType.getSimpleName());
        }
    }
    
    /**
     * 解析请求体为指定类型的对象
     */
    private Object parseRequestBody(HttpServletRequest request, Class<?> targetType) throws IOException {
        if (targetType.isPrimitive() || targetType == String.class) {
            return null; // 基本类型暂不支持
        }
        
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        if (requestBody.length() > 0) {
            return objectMapper.readValue(requestBody.toString(), targetType);
        }
        
        return null;
    }
    
    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> result = Result.success(data);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> result = Result.fail(String.valueOf(status), message, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
    
}
