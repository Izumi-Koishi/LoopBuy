package com.shiwu.framework.web;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.RequestUtil;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MVC框架基础控制器
 * 
 * 提供注解驱动的路由分发、参数自动注入、统一异常处理等功能
 * 所有业务Controller都应该继承此类
 * 
 * 核心功能：
 * 1. 自动扫描@RequestMapping注解的方法
 * 2. 支持路径参数(@PathVariable)和查询参数(@RequestParam)自动注入
 * 3. 统一的JSON响应处理
 * 4. 统一的异常处理
 * 5. 向后兼容传统Servlet方式
 */
public abstract class BaseController extends HttpServlet {
    protected static final Logger logger = LoggerFactory.getLogger(BaseController.class);
    
    private Map<String, RouteInfo> routes = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        scanRoutes();
        logger.info("BaseController初始化完成，扫描到{}个路由", routes.size());
    }
    
    /**
     * 扫描Controller中的@RequestMapping注解方法
     */
    private void scanRoutes() {
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                String path = mapping.value();
                String httpMethod = mapping.method();
                String key = httpMethod + ":" + path;
                routes.put(key, new RouteInfo(method, path));
                logger.debug("注册路由: {} -> {}.{}", key, this.getClass().getSimpleName(), method.getName());
            }
        }
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp, "GET");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp, "POST");
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp, "PUT");
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp, "DELETE");
    }
    
    /**
     * 统一的请求处理入口
     */
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, String method) 
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/";
        
        try {
            // 尝试匹配注解路由
            RouteInfo matchedRoute = findMatchingRoute(method, pathInfo);
            if (matchedRoute != null) {
                Object result = invokeAnnotatedMethod(matchedRoute, req, resp, pathInfo);
                sendJsonResponse(resp, result);
                return;
            }
            
            // 回退到传统路由处理
            handleTraditionalRouting(req, resp, method);
            
        } catch (Exception e) {
            logger.error("处理请求失败: {} {}, error: {}", method, pathInfo, e.getMessage(), e);
            sendErrorResponse(resp, 500, "系统错误，请稍后再试");
        }
    }
    
    /**
     * 查找匹配的路由
     */
    private RouteInfo findMatchingRoute(String method, String pathInfo) {
        // 精确匹配
        String exactKey = method + ":" + pathInfo;
        if (routes.containsKey(exactKey)) {
            return routes.get(exactKey);
        }
        
        // 模式匹配（支持路径参数）
        for (Map.Entry<String, RouteInfo> entry : routes.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(method + ":")) {
                RouteInfo routeInfo = entry.getValue();
                if (routeInfo.getPattern().matcher(pathInfo).matches()) {
                    return routeInfo;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 调用注解方法
     */
    private Object invokeAnnotatedMethod(RouteInfo routeInfo, HttpServletRequest req, 
                                       HttpServletResponse resp, String pathInfo) throws Exception {
        Method method = routeInfo.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        // 提取路径参数
        Map<String, String> pathParams = extractPathParams(routeInfo, pathInfo);
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            if (param.getType() == HttpServletRequest.class) {
                args[i] = req;
            } else if (param.getType() == HttpServletResponse.class) {
                args[i] = resp;
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVar = param.getAnnotation(PathVariable.class);
                String paramName = pathVar.value().isEmpty() ? param.getName() : pathVar.value();
                String value = pathParams.get(paramName);
                args[i] = convertParameter(value, param.getType());
            } else if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam reqParam = param.getAnnotation(RequestParam.class);
                String paramName = reqParam.value().isEmpty() ? param.getName() : reqParam.value();
                String value = req.getParameter(paramName);
                if (value == null && !reqParam.defaultValue().isEmpty()) {
                    value = reqParam.defaultValue();
                }
                if (value == null && reqParam.required()) {
                    throw new IllegalArgumentException("必填参数缺失: " + paramName);
                }
                args[i] = convertParameter(value, param.getType());
            }
        }
        
        return method.invoke(this, args);
    }
    
    /**
     * 提取路径参数
     */
    private Map<String, String> extractPathParams(RouteInfo routeInfo, String pathInfo) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = routeInfo.getPattern().matcher(pathInfo);
        
        if (matcher.matches()) {
            for (int i = 0; i < routeInfo.getParamNames().size(); i++) {
                params.put(routeInfo.getParamNames().get(i), matcher.group(i + 1));
            }
        }
        
        return params;
    }
    
    /**
     * 参数类型转换
     */
    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) return null;
        
        if (targetType == String.class) {
            return value;
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        }
        
        return value;
    }
    
    /**
     * 传统路由处理（子类可重写）
     */
    protected void handleTraditionalRouting(HttpServletRequest req, HttpServletResponse resp, String method) 
            throws ServletException, IOException {
        sendErrorResponse(resp, 404, "接口不存在");
    }
    
    /**
     * 发送JSON响应
     */
    protected void sendJsonResponse(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (data instanceof Result) {
            ((Result<?>) data).writeToResponse(resp);
        } else {
            resp.getWriter().write(JsonUtil.toJson(data));
        }
    }
    
    /**
     * 发送错误响应
     */
    protected void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        Result<Object> result = Result.fail(String.valueOf(status), message);
        sendJsonResponse(resp, result);
    }
    
    /**
     * 获取当前用户ID
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        return RequestUtil.getCurrentUserId(request);
    }
    
    /**
     * 读取请求体内容
     */
    protected String getRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        return requestBody.toString();
    }

    /**
     * 解析请求体为指定类型
     */
    protected <T> T parseRequestBody(HttpServletRequest request, Class<T> clazz) throws IOException {
        String requestBody = getRequestBody(request);
        if (requestBody == null || requestBody.trim().isEmpty()) {
            return null;
        }
        return JsonUtil.fromJson(requestBody, clazz);
    }
}
