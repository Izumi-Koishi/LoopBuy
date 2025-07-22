package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器增强注解
 * 
 * 用于标记一个类为全局控制器增强器，主要用于实现全局异常处理。
 * 标记了此注解的类中的@ExceptionHandler方法会对所有控制器生效，
 * 实现统一的异常处理机制。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>全局异常处理：为所有控制器提供统一的异常处理</li>
 * <li>自动扫描：框架启动时自动扫描并注册到IoC容器</li>
 * <li>优先级管理：与控制器内异常处理方法的优先级管理</li>
 * <li>统一响应：确保异常响应格式的一致性</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <h4>基本全局异常处理：</h4>
 * <pre>
 * {@code
 * @ControllerAdvice
 * public class GlobalExceptionHandler {
 *     
 *     private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 *     
 *     // 处理业务异常
 *     @ExceptionHandler(BusinessException.class)
 *     public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
 *         logger.warn("业务异常 - URL: {}, 异常: {}", request.getRequestURL(), e.getMessage());
 *         return Result.error(e.getCode(), e.getMessage());
 *     }
 *     
 *     // 处理参数验证异常
 *     @ExceptionHandler(IllegalArgumentException.class)
 *     public Result handleValidationException(IllegalArgumentException e) {
 *         logger.warn("参数验证异常: {}", e.getMessage());
 *         return Result.error("INVALID_PARAM", "参数错误: " + e.getMessage());
 *     }
 *     
 *     // 处理认证异常
 *     @ExceptionHandler(AuthenticationException.class)
 *     public Result handleAuthException(AuthenticationException e, HttpServletResponse response) {
 *         response.setStatus(401);
 *         return Result.error("UNAUTHORIZED", "认证失败: " + e.getMessage());
 *     }
 *     
 *     // 处理权限异常
 *     @ExceptionHandler(AuthorizationException.class)
 *     public Result handleAuthorizationException(AuthorizationException e, HttpServletResponse response) {
 *         response.setStatus(403);
 *         return Result.error("FORBIDDEN", "权限不足: " + e.getMessage());
 *     }
 *     
 *     // 处理数据库异常
 *     @ExceptionHandler({SQLException.class, DataAccessException.class})
 *     public Result handleDatabaseException(Exception e) {
 *         logger.error("数据库异常", e);
 *         return Result.error("DB_ERROR", "数据操作失败，请稍后重试");
 *     }
 *     
 *     // 处理所有未捕获的异常（兜底处理）
 *     @ExceptionHandler(Exception.class)
 *     public Result handleGenericException(Exception e, HttpServletRequest request) {
 *         logger.error("系统异常 - URL: {}, 异常: {}", request.getRequestURL(), e.getMessage(), e);
 *         return Result.error("SYSTEM_ERROR", "系统内部错误，请联系管理员");
 *     }
 * }
 * }
 * </pre>
 * 
 * <h4>分模块异常处理：</h4>
 * <pre>
 * {@code
 * // 用户模块异常处理
 * @ControllerAdvice
 * public class UserExceptionHandler {
 *     
 *     @ExceptionHandler(UserNotFoundException.class)
 *     public Result handleUserNotFound(UserNotFoundException e) {
 *         return Result.error("USER_NOT_FOUND", "用户不存在");
 *     }
 *     
 *     @ExceptionHandler(DuplicateUsernameException.class)
 *     public Result handleDuplicateUsername(DuplicateUsernameException e) {
 *         return Result.error("USERNAME_EXISTS", "用户名已存在");
 *     }
 *     
 *     @ExceptionHandler(InvalidPasswordException.class)
 *     public Result handleInvalidPassword(InvalidPasswordException e) {
 *         return Result.error("INVALID_PASSWORD", "密码格式不正确");
 *     }
 * }
 * 
 * // 商品模块异常处理
 * @ControllerAdvice
 * public class ProductExceptionHandler {
 *     
 *     @ExceptionHandler(ProductNotFoundException.class)
 *     public Result handleProductNotFound(ProductNotFoundException e) {
 *         return Result.error("PRODUCT_NOT_FOUND", "商品不存在");
 *     }
 *     
 *     @ExceptionHandler(InsufficientStockException.class)
 *     public Result handleInsufficientStock(InsufficientStockException e) {
 *         return Result.error("INSUFFICIENT_STOCK", "库存不足");
 *     }
 * }
 * }
 * </pre>
 * 
 * <h4>带条件的异常处理：</h4>
 * <pre>
 * {@code
 * @ControllerAdvice
 * public class ConditionalExceptionHandler {
 *     
 *     @ExceptionHandler(BusinessException.class)
 *     public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
 *         // 根据请求路径决定处理方式
 *         String requestPath = request.getRequestURI();
 *         
 *         if (requestPath.startsWith("/api/admin/")) {
 *             // 管理员接口，返回详细错误信息
 *             return Result.error(e.getCode(), e.getMessage() + " [详细信息: " + e.getDetailMessage() + "]");
 *         } else {
 *             // 普通用户接口，返回简化错误信息
 *             return Result.error(e.getCode(), e.getMessage());
 *         }
 *     }
 *     
 *     @ExceptionHandler(Exception.class)
 *     public Result handleGenericException(Exception e, HttpServletRequest request) {
 *         String userAgent = request.getHeader("User-Agent");
 *         
 *         // 记录详细的异常信息
 *         logger.error("系统异常 - URL: {}, UserAgent: {}, 参数: {}", 
 *                     request.getRequestURL(), 
 *                     userAgent, 
 *                     request.getParameterMap(), 
 *                     e);
 *         
 *         // 根据客户端类型返回不同格式
 *         if (userAgent != null && userAgent.contains("Mobile")) {
 *             return Result.error("SYSTEM_ERROR", "系统繁忙，请稍后重试");
 *         } else {
 *             return Result.error("SYSTEM_ERROR", "系统内部错误，请联系技术支持");
 *         }
 *     }
 * }
 * }
 * </pre>
 * 
 * <h3>异常处理优先级：</h3>
 * <ol>
 * <li>控制器内的@ExceptionHandler方法（最高优先级）</li>
 * <li>@ControllerAdvice类中的@ExceptionHandler方法</li>
 * <li>具体异常类型优先于父类异常类型</li>
 * <li>多个@ControllerAdvice的执行顺序不确定</li>
 * </ol>
 * 
 * <h3>生命周期：</h3>
 * <ul>
 * <li>框架启动时自动扫描@ControllerAdvice类</li>
 * <li>注册到IoC容器中，默认为单例</li>
 * <li>支持依赖注入其他Service</li>
 * <li>在请求处理过程中发生异常时被调用</li>
 * </ul>
 * 
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>建议只创建一个全局异常处理类</li>
 * <li>按异常类型分层处理，从具体到通用</li>
 * <li>记录详细的异常日志，便于问题排查</li>
 * <li>返回用户友好的错误信息</li>
 * <li>敏感信息不要暴露给客户端</li>
 * <li>考虑异常处理的性能影响</li>
 * </ul>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 * <li>@ControllerAdvice类必须是public的</li>
 * <li>异常处理方法不能再抛出异常</li>
 * <li>避免在异常处理中进行复杂的业务逻辑</li>
 * <li>确保异常处理方法的线程安全性</li>
 * <li>注意异常处理的性能开销</li>
 * </ul>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ExceptionHandler
 * @see Controller
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllerAdvice {
    
    /**
     * 增强器的名称
     * 
     * 指定控制器增强器在IoC容器中的名称。如果不指定，框架将使用类名的首字母小写形式作为默认名称。
     * 
     * <h4>命名规则：</h4>
     * <ul>
     * <li>如果value为空：GlobalExceptionHandler -> globalExceptionHandler</li>
     * <li>如果value不为空：使用指定的名称</li>
     * </ul>
     * 
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 使用默认名称 globalExceptionHandler
     * @ControllerAdvice
     * public class GlobalExceptionHandler { }
     * 
     * // 使用自定义名称 myExceptionHandler
     * @ControllerAdvice("myExceptionHandler")
     * public class GlobalExceptionHandler { }
     * }
     * </pre>
     * 
     * @return 增强器名称，默认为空字符串
     */
    String value() default "";
}
