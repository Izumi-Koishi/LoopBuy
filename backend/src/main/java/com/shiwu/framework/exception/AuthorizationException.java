package com.shiwu.framework.exception;

/**
 * 授权异常
 * 
 * 用于表示用户权限不足的异常情况，如访问被拒绝、角色权限不够等。
 * 授权异常会被GlobalExceptionHandler捕获并返回403状态码。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>权限控制：表示用户权限不足</li>
 * <li>统一处理：配合GlobalExceptionHandler返回403状态码</li>
 * <li>安全保护：防止未授权的操作</li>
 * <li>用户友好：提供清晰的权限不足原因</li>
 * </ul>
 * 
 * <h3>常见授权失败场景：</h3>
 * <ul>
 * <li>普通用户访问管理员功能</li>
 * <li>用户访问其他用户的私有数据</li>
 * <li>角色权限不足</li>
 * <li>资源访问被拒绝</li>
 * <li>操作权限不够</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 在Service层检查权限
 * @Service
 * public class OrderServiceImpl implements OrderService {
 *     
 *     @Override
 *     public Result getOrder(Long orderId, Long currentUserId) {
 *         Order order = orderRepository.findById(orderId);
 *         if (order == null) {
 *             throw BusinessException.orderNotFound();
 *         }
 *         
 *         // 检查是否是订单所有者
 *         if (!order.getUserId().equals(currentUserId)) {
 *             throw new AuthorizationException("无权访问他人订单");
 *         }
 *         
 *         return Result.success(new OrderVO(order));
 *     }
 *     
 *     @Override
 *     public Result cancelOrder(Long orderId, Long currentUserId) {
 *         Order order = orderRepository.findById(orderId);
 *         if (order == null) {
 *             throw BusinessException.orderNotFound();
 *         }
 *         
 *         // 检查订单所有权
 *         if (!order.getUserId().equals(currentUserId)) {
 *             throw AuthorizationException.accessDenied("订单");
 *         }
 *         
 *         // 检查订单状态
 *         if (!"PENDING".equals(order.getStatus())) {
 *             throw new AuthorizationException("只能取消待支付的订单");
 *         }
 *         
 *         // 取消订单逻辑...
 *         return Result.success("订单已取消");
 *     }
 * }
 * 
 * // 在管理员Controller中检查管理员权限
 * @Controller
 * public class AdminController {
 *     
 *     @RequestMapping(value = "/api/admin/users", method = "GET")
 *     public Result getAllUsers(HttpServletRequest request) {
 *         User currentUser = getCurrentUser(request);
 *         
 *         if (!"ADMIN".equals(currentUser.getRole())) {
 *             throw AuthorizationException.insufficientRole("管理员");
 *         }
 *         
 *         return adminService.getAllUsers();
 *     }
 * }
 * 
 * // 异常会被GlobalExceptionHandler捕获，返回：
 * // HTTP 403 Forbidden
 * // {
 * //   "code": "FORBIDDEN",
 * //   "message": "权限不足: 无权访问他人订单",
 * //   "data": null
 * // }
 * }
 * </pre>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see GlobalExceptionHandler
 * @see AuthenticationException
 */
public class AuthorizationException extends RuntimeException {
    
    /**
     * 构造函数
     * 
     * @param message 权限不足信息
     */
    public AuthorizationException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 权限不足信息
     * @param cause 原始异常
     */
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // 常用授权异常的静态工厂方法
    
    /**
     * 访问被拒绝异常
     * 
     * @param resource 资源名称
     * @return AuthorizationException
     */
    public static AuthorizationException accessDenied(String resource) {
        return new AuthorizationException("无权访问" + resource);
    }
    
    /**
     * 角色权限不足异常
     * 
     * @param requiredRole 需要的角色
     * @return AuthorizationException
     */
    public static AuthorizationException insufficientRole(String requiredRole) {
        return new AuthorizationException("需要" + requiredRole + "权限");
    }
    
    /**
     * 操作权限不足异常
     * 
     * @param operation 操作名称
     * @return AuthorizationException
     */
    public static AuthorizationException operationDenied(String operation) {
        return new AuthorizationException("无权进行" + operation + "操作");
    }
    
    /**
     * 资源所有权验证失败异常
     * 
     * @return AuthorizationException
     */
    public static AuthorizationException notOwner() {
        return new AuthorizationException("只能操作自己的资源");
    }
    
    /**
     * 管理员权限不足异常
     * 
     * @return AuthorizationException
     */
    public static AuthorizationException adminRequired() {
        return new AuthorizationException("需要管理员权限");
    }
    
    /**
     * 功能权限不足异常
     * 
     * @param feature 功能名称
     * @return AuthorizationException
     */
    public static AuthorizationException featureAccessDenied(String feature) {
        return new AuthorizationException("无权使用" + feature + "功能");
    }
}
