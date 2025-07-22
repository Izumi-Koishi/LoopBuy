package com.shiwu.framework.exception;

/**
 * 业务异常基类
 * 
 * 用于表示业务逻辑中的异常情况，包含错误码和错误信息。
 * 业务异常通常是可预期的，可以直接返回给客户端，不需要隐藏异常信息。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>错误码：提供标准化的错误码，便于客户端处理</li>
 * <li>错误信息：提供用户友好的错误描述</li>
 * <li>异常链：支持异常链传递，保留原始异常信息</li>
 * <li>统一处理：配合GlobalExceptionHandler实现统一异常处理</li>
 * </ul>
 * 
 * <h3>常用错误码规范：</h3>
 * <ul>
 * <li>USER_NOT_FOUND - 用户不存在</li>
 * <li>USERNAME_EXISTS - 用户名已存在</li>
 * <li>INVALID_PASSWORD - 密码错误</li>
 * <li>PRODUCT_NOT_FOUND - 商品不存在</li>
 * <li>INSUFFICIENT_STOCK - 库存不足</li>
 * <li>ORDER_NOT_FOUND - 订单不存在</li>
 * <li>PAYMENT_FAILED - 支付失败</li>
 * <li>PERMISSION_DENIED - 权限不足</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 在Service层抛出业务异常
 * @Service
 * public class UserServiceImpl implements UserService {
 *     
 *     @Override
 *     public Result login(String username, String password) {
 *         User user = userRepository.findByUsername(username);
 *         if (user == null) {
 *             throw new BusinessException("USER_NOT_FOUND", "用户不存在");
 *         }
 *         
 *         if (!PasswordUtils.verify(password, user.getPassword())) {
 *             throw new BusinessException("INVALID_PASSWORD", "密码错误");
 *         }
 *         
 *         return Result.success(new UserVO(user));
 *     }
 *     
 *     @Override
 *     public Result register(String username, String password, String email) {
 *         if (userRepository.existsByUsername(username)) {
 *             throw new BusinessException("USERNAME_EXISTS", "用户名已存在");
 *         }
 *         
 *         if (userRepository.existsByEmail(email)) {
 *             throw new BusinessException("EMAIL_EXISTS", "邮箱已被注册");
 *         }
 *         
 *         // 注册逻辑...
 *         return Result.success("注册成功");
 *     }
 * }
 * 
 * // 在Controller层会被GlobalExceptionHandler自动捕获
 * // 返回统一格式的错误响应：
 * // {
 * //   "code": "USER_NOT_FOUND",
 * //   "message": "用户不存在",
 * //   "data": null
 * // }
 * }
 * </pre>
 * 
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>错误码使用大写字母和下划线，便于识别</li>
 * <li>错误信息使用用户友好的中文描述</li>
 * <li>在Service层抛出，不要在Controller层抛出</li>
 * <li>相同的业务场景使用相同的错误码</li>
 * <li>避免在异常信息中暴露敏感信息</li>
 * </ul>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see GlobalExceptionHandler
 */
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String code;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原始异常
     */
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 重写toString方法，便于日志记录
     * 
     * @return 异常的字符串表示
     */
    @Override
    public String toString() {
        return String.format("BusinessException{code='%s', message='%s'}", code, getMessage());
    }
    
    // 常用业务异常的静态工厂方法
    
    /**
     * 用户不存在异常
     * 
     * @return BusinessException
     */
    public static BusinessException userNotFound() {
        return new BusinessException("USER_NOT_FOUND", "用户不存在");
    }
    
    /**
     * 用户名已存在异常
     * 
     * @return BusinessException
     */
    public static BusinessException usernameExists() {
        return new BusinessException("USERNAME_EXISTS", "用户名已存在");
    }
    
    /**
     * 密码错误异常
     * 
     * @return BusinessException
     */
    public static BusinessException invalidPassword() {
        return new BusinessException("INVALID_PASSWORD", "密码错误");
    }
    
    /**
     * 商品不存在异常
     * 
     * @return BusinessException
     */
    public static BusinessException productNotFound() {
        return new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
    }
    
    /**
     * 库存不足异常
     * 
     * @return BusinessException
     */
    public static BusinessException insufficientStock() {
        return new BusinessException("INSUFFICIENT_STOCK", "库存不足");
    }
    
    /**
     * 订单不存在异常
     * 
     * @return BusinessException
     */
    public static BusinessException orderNotFound() {
        return new BusinessException("ORDER_NOT_FOUND", "订单不存在");
    }
    
    /**
     * 支付失败异常
     * 
     * @return BusinessException
     */
    public static BusinessException paymentFailed() {
        return new BusinessException("PAYMENT_FAILED", "支付失败");
    }
    
    /**
     * 权限不足异常
     * 
     * @return BusinessException
     */
    public static BusinessException permissionDenied() {
        return new BusinessException("PERMISSION_DENIED", "权限不足");
    }
    
    /**
     * 参数无效异常
     * 
     * @param paramName 参数名
     * @return BusinessException
     */
    public static BusinessException invalidParam(String paramName) {
        return new BusinessException("INVALID_PARAM", "参数无效: " + paramName);
    }
    
    /**
     * 操作失败异常
     * 
     * @param operation 操作名称
     * @return BusinessException
     */
    public static BusinessException operationFailed(String operation) {
        return new BusinessException("OPERATION_FAILED", operation + "失败");
    }
    
    /**
     * 资源不存在异常
     * 
     * @param resource 资源名称
     * @return BusinessException
     */
    public static BusinessException resourceNotFound(String resource) {
        return new BusinessException("RESOURCE_NOT_FOUND", resource + "不存在");
    }
    
    /**
     * 状态错误异常
     * 
     * @param currentStatus 当前状态
     * @param expectedStatus 期望状态
     * @return BusinessException
     */
    public static BusinessException invalidStatus(String currentStatus, String expectedStatus) {
        return new BusinessException("INVALID_STATUS", 
            String.format("状态错误，当前状态: %s，期望状态: %s", currentStatus, expectedStatus));
    }
}
