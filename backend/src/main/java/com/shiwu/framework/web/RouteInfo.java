package com.shiwu.framework.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 路由信息类
 * 
 * 用于存储HTTP请求路径与控制器方法之间的映射关系。
 * 支持静态路径和动态路径参数（如：/api/products/{id}）的解析和匹配。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>路径存储：保存原始路径模式和编译后的正则表达式</li>
 * <li>参数提取：自动提取路径中的参数名称</li>
 * <li>路径匹配：支持动态路径参数的匹配</li>
 * <li>方法绑定：关联具体的控制器对象和方法</li>
 * </ul>
 * 
 * <h3>支持的路径格式：</h3>
 * <ul>
 * <li>静态路径：/api/user/login</li>
 * <li>单参数路径：/api/products/{id}</li>
 * <li>多参数路径：/api/users/{userId}/orders/{orderId}</li>
 * <li>混合路径：/api/categories/{categoryId}/products/{productId}/reviews</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 创建路由信息
 * Method method = UserController.class.getMethod("getUser", Long.class);
 * Object controller = new UserController();
 * RouteInfo routeInfo = new RouteInfo(controller, method, "GET", "/api/users/{id}");
 * 
 * // 检查路径是否匹配
 * boolean matches = routeInfo.matches("/api/users/123");  // true
 * boolean matches2 = routeInfo.matches("/api/users/abc"); // true (字符串也匹配)
 * boolean matches3 = routeInfo.matches("/api/products/123"); // false
 * 
 * // 提取路径参数
 * Map<String, String> params = routeInfo.extractPathParams("/api/users/123");
 * // params.get("id") = "123"
 * }
 * </pre>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class RouteInfo {
    
    /**
     * 控制器实例
     */
    private final Object controller;
    
    /**
     * 处理方法
     */
    private final Method method;
    
    /**
     * HTTP方法类型（GET、POST、PUT、DELETE等）
     */
    private final String httpMethod;
    
    /**
     * 原始路径模式（如：/api/products/{id}）
     */
    private final String pathPattern;
    
    /**
     * 编译后的正则表达式模式，用于路径匹配
     */
    private final Pattern compiledPattern;
    
    /**
     * 路径参数名称列表，按出现顺序排列
     */
    private final List<String> parameterNames;
    
    /**
     * 构造函数
     *
     * @param controller 控制器实例
     * @param method 处理方法
     * @param httpMethod HTTP方法类型
     * @param pathPattern 路径模式
     */
    public RouteInfo(Object controller, Method method, String httpMethod, String pathPattern) {
        this.controller = controller;
        this.method = method;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.parameterNames = new ArrayList<>();
        this.compiledPattern = compilePathPattern(pathPattern);
    }

    /**
     * 简化构造函数 - 用于BaseController
     *
     * @param method 处理方法
     * @param pathPattern 路径模式
     */
    public RouteInfo(Method method, String pathPattern) {
        this.controller = null; // BaseController中不需要controller实例
        this.method = method;
        this.httpMethod = ""; // 将在BaseController中设置
        this.pathPattern = pathPattern;
        this.parameterNames = new ArrayList<>();
        this.compiledPattern = compilePathPattern(pathPattern);
    }
    
    /**
     * 编译路径模式为正则表达式
     * 
     * 将路径模式（如：/api/products/{id}）转换为正则表达式模式，
     * 同时提取参数名称列表。
     * 
     * <h4>转换规则：</h4>
     * <ul>
     * <li>/api/products/{id} -> ^/api/products/([^/]+)$</li>
     * <li>/api/users/{userId}/orders/{orderId} -> ^/api/users/([^/]+)/orders/([^/]+)$</li>
     * <li>/api/user/login -> ^/api/user/login$</li>
     * </ul>
     * 
     * @param pathPattern 原始路径模式
     * @return 编译后的正则表达式模式
     */
    private Pattern compilePathPattern(String pathPattern) {
        // 清空参数名称列表
        parameterNames.clear();
        
        // 转义正则表达式特殊字符
        String regex = pathPattern;
        
        // 查找所有的路径参数 {paramName}
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = paramPattern.matcher(pathPattern);
        
        // 替换路径参数为正则表达式捕获组
        while (matcher.find()) {
            String paramName = matcher.group(1);
            parameterNames.add(paramName);
            
            // 将 {paramName} 替换为 ([^/]+)，匹配除了斜杠之外的任意字符
            regex = regex.replace(matcher.group(0), "([^/]+)");
        }
        
        // 添加开始和结束锚点，确保完全匹配
        regex = "^" + regex + "$";
        
        return Pattern.compile(regex);
    }
    
    /**
     * 检查给定路径是否匹配此路由
     * 
     * @param requestPath 请求路径
     * @return 如果匹配返回true，否则返回false
     */
    public boolean matches(String requestPath) {
        if (requestPath == null) {
            return false;
        }
        return compiledPattern.matcher(requestPath).matches();
    }
    
    /**
     * 从请求路径中提取参数值
     * 
     * 根据路径模式从实际请求路径中提取参数值，返回参数名到参数值的映射。
     * 
     * <h4>提取示例：</h4>
     * <ul>
     * <li>模式：/api/products/{id}，路径：/api/products/123 -> {id: "123"}</li>
     * <li>模式：/api/users/{userId}/orders/{orderId}，路径：/api/users/456/orders/789 -> {userId: "456", orderId: "789"}</li>
     * </ul>
     * 
     * @param requestPath 请求路径
     * @return 参数名到参数值的映射，如果路径不匹配返回空Map
     */
    public java.util.Map<String, String> extractPathParams(String requestPath) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        
        if (requestPath == null) {
            return params;
        }
        
        Matcher matcher = compiledPattern.matcher(requestPath);
        if (matcher.matches()) {
            // 提取所有捕获组的值
            for (int i = 0; i < parameterNames.size(); i++) {
                String paramName = parameterNames.get(i);
                String paramValue = matcher.group(i + 1); // 捕获组从1开始
                params.put(paramName, paramValue);
            }
        }
        
        return params;
    }
    
    /**
     * 获取路由的唯一标识
     * 
     * 用于在路由映射表中作为键值，格式为：HTTP方法:路径模式
     * 
     * @return 路由唯一标识
     */
    public String getRouteKey() {
        return httpMethod + ":" + pathPattern;
    }
    
    /**
     * 检查是否为静态路径（不包含路径参数）
     * 
     * @return 如果是静态路径返回true，否则返回false
     */
    public boolean isStaticPath() {
        return parameterNames.isEmpty();
    }
    
    /**
     * 获取参数数量
     * 
     * @return 路径参数的数量
     */
    public int getParameterCount() {
        return parameterNames.size();
    }
    
    // Getter方法
    
    /**
     * 获取控制器实例
     * 
     * @return 控制器实例
     */
    public Object getController() {
        return controller;
    }
    
    /**
     * 获取处理方法
     * 
     * @return 处理方法
     */
    public Method getMethod() {
        return method;
    }
    
    /**
     * 获取HTTP方法类型
     * 
     * @return HTTP方法类型
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    
    /**
     * 获取原始路径模式
     * 
     * @return 原始路径模式
     */
    public String getPathPattern() {
        return pathPattern;
    }
    
    /**
     * 获取编译后的正则表达式模式
     *
     * @return 编译后的正则表达式模式
     */
    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    /**
     * 获取编译后的正则表达式模式 - BaseController兼容方法
     *
     * @return 编译后的正则表达式模式
     */
    public Pattern getPattern() {
        return compiledPattern;
    }
    
    /**
     * 获取参数名称列表
     *
     * @return 参数名称列表的副本
     */
    public List<String> getParameterNames() {
        return new ArrayList<>(parameterNames);
    }

    /**
     * 获取参数名称列表 - BaseController兼容方法
     *
     * @return 参数名称列表
     */
    public List<String> getParamNames() {
        return parameterNames;
    }
    
    /**
     * 重写toString方法，便于调试
     * 
     * @return 路由信息的字符串表示
     */
    @Override
    public String toString() {
        return String.format("RouteInfo{httpMethod='%s', pathPattern='%s', controller=%s, method=%s, parameterNames=%s}",
                httpMethod, pathPattern, controller.getClass().getSimpleName(), method.getName(), parameterNames);
    }
    
    /**
     * 重写equals方法
     * 
     * @param obj 比较对象
     * @return 如果相等返回true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        RouteInfo routeInfo = (RouteInfo) obj;
        return httpMethod.equals(routeInfo.httpMethod) && pathPattern.equals(routeInfo.pathPattern);
    }
    
    /**
     * 重写hashCode方法
     * 
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(httpMethod, pathPattern);
    }
}
