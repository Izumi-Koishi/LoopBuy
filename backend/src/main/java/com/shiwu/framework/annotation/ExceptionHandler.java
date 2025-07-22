package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常处理器注解
 * 
 * 用于标记一个方法为异常处理方法。当控制器方法抛出指定类型的异常时，
 * 框架会自动调用标记了此注解的方法来处理异常，实现统一的异常处理机制。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>异常捕获：自动捕获控制器方法抛出的异常</li>
 * <li>类型匹配：根据异常类型匹配对应的处理方法</li>
 * <li>统一处理：提供统一的异常处理和响应格式</li>
 * <li>优雅降级：避免异常直接暴露给用户</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <h4>基本用法：</h4>
 * <pre>
 * {@code
 * @ControllerAdvice
 * public class GlobalExceptionHandler {
 *     
 *     // 处理业务异常
 *     @ExceptionHandler(BusinessException.class)
 *     public Result handleBusinessException(BusinessException e) {
 *         return Result.error(e.getCode(), e.getMessage());
 *     }
 *     
 *     // 处理参数验证异常
 *     @ExceptionHandler(IllegalArgumentException.class)
 *     public Result handleIllegalArgumentException(IllegalArgumentException e) {
 *         return Result.error("400", "参数错误: " + e.getMessage());
 *     }
 *     
 *     // 处理所有未知异常
 *     @ExceptionHandler(Exception.class)
 *     public Result handleException(Exception e) {
 *         // 记录日志
 *         logger.error("系统异常", e);
 *         return Result.error("500", "系统内部错误");
 *     }
 * }
 * }
 * </pre>
 * 
 * <h4>控制器级别异常处理：</h4>
 * <pre>
 * {@code
 * @Controller
 * public class UserController {
 *     
 *     @RequestMapping(value = "/api/user/login", method = "POST")
 *     public Result login(@RequestParam String username, @RequestParam String password) {
 *         if (username == null || username.trim().isEmpty()) {
 *             throw new IllegalArgumentException("用户名不能为空");
 *         }
 *         return userService.login(username, password);
 *     }
 *     
 *     // 处理当前控制器的参数异常
 *     @ExceptionHandler(IllegalArgumentException.class)
 *     public Result handleIllegalArgument(IllegalArgumentException e) {
 *         return Result.error("INVALID_PARAM", e.getMessage());
 *     }
 * }
 * }
 * </pre>
 * 
 * <h4>多异常类型处理：</h4>
 * <pre>
 * {@code
 * @ControllerAdvice
 * public class GlobalExceptionHandler {
 *     
 *     // 处理多种异常类型
 *     @ExceptionHandler({
 *         UserNotFoundException.class,
 *         ProductNotFoundException.class,
 *         OrderNotFoundException.class
 *     })
 *     public Result handleNotFoundException(Exception e) {
 *         return Result.error("404", "资源不存在: " + e.getMessage());
 *     }
 *     
 *     // 处理数据库相关异常
 *     @ExceptionHandler({
 *         SQLException.class,
 *         DataAccessException.class
 *     })
 *     public Result handleDatabaseException(Exception e) {
 *         logger.error("数据库异常", e);
 *         return Result.error("DB_ERROR", "数据操作失败");
 *     }
 * }
 * }
 * </pre>
 * 
 * <h3>异常处理优先级：</h3>
 * <ol>
 * <li>控制器内的@ExceptionHandler方法</li>
 * <li>@ControllerAdvice类中的@ExceptionHandler方法</li>
 * <li>具体异常类型优先于父类异常类型</li>
 * <li>最后匹配Exception.class的处理方法</li>
 * </ol>
 * 
 * <h3>方法签名要求：</h3>
 * <ul>
 * <li>方法必须是public的</li>
 * <li>方法参数可以包含异常对象</li>
 * <li>方法参数可以包含HttpServletRequest、HttpServletResponse</li>
 * <li>方法返回值会被自动序列化为JSON响应</li>
 * </ul>
 * 
 * <h4>支持的方法参数：</h4>
 * <pre>
 * {@code
 * @ExceptionHandler(BusinessException.class)
 * public Result handleBusinessException(
 *     BusinessException e,                    // 异常对象
 *     HttpServletRequest request,             // 请求对象
 *     HttpServletResponse response            // 响应对象
 * ) {
 *     // 可以获取请求信息进行日志记录
 *     String requestUrl = request.getRequestURL().toString();
 *     String userAgent = request.getHeader("User-Agent");
 *     
 *     logger.warn("业务异常 - URL: {}, UserAgent: {}, 异常: {}", 
 *                 requestUrl, userAgent, e.getMessage());
 *     
 *     return Result.error(e.getCode(), e.getMessage());
 * }
 * }
 * </pre>
 * 
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>使用@ControllerAdvice实现全局异常处理</li>
 * <li>为不同类型的异常提供不同的处理逻辑</li>
 * <li>记录异常日志，便于问题排查</li>
 * <li>不要在异常处理方法中再抛出异常</li>
 * <li>返回用户友好的错误信息</li>
 * </ul>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 * <li>异常处理方法不能抛出异常</li>
 * <li>避免在异常处理中进行复杂的业务逻辑</li>
 * <li>敏感信息不要直接返回给客户端</li>
 * <li>确保异常处理方法的性能</li>
 * </ul>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ControllerAdvice
 * @see Controller
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionHandler {
    
    /**
     * 要处理的异常类型
     * 
     * 指定此方法能够处理的异常类型数组。当控制器方法抛出这些类型的异常时，
     * 框架会调用此方法进行处理。
     * 
     * <h4>匹配规则：</h4>
     * <ul>
     * <li>精确匹配：异常类型完全相同</li>
     * <li>继承匹配：异常是指定类型的子类</li>
     * <li>优先级：具体类型优先于父类类型</li>
     * </ul>
     * 
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 处理单个异常类型
     * @ExceptionHandler(BusinessException.class)
     * public Result handleBusinessException(BusinessException e) { ... }
     * 
     * // 处理多个异常类型
     * @ExceptionHandler({
     *     IllegalArgumentException.class,
     *     IllegalStateException.class
     * })
     * public Result handleIllegalException(Exception e) { ... }
     * 
     * // 处理所有异常（兜底处理）
     * @ExceptionHandler(Exception.class)
     * public Result handleAllException(Exception e) { ... }
     * }
     * </pre>
     * 
     * @return 异常类型数组
     */
    Class<? extends Throwable>[] value() default {};
}
