package com.shiwu.framework.exception;

import com.shiwu.framework.annotation.ControllerAdvice;
import com.shiwu.framework.annotation.ExceptionHandler;
import com.shiwu.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器
 * 
 * 统一处理应用中所有未被捕获的异常，确保API响应格式的一致性。
 * 提供友好的错误信息给客户端，同时记录详细的异常日志便于问题排查。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>统一异常处理：捕获所有控制器抛出的异常</li>
 * <li>分类处理：根据异常类型提供不同的处理策略</li>
 * <li>日志记录：记录详细的异常信息便于问题排查</li>
 * <li>响应格式化：统一的错误响应格式</li>
 * <li>安全保护：避免敏感信息泄露给客户端</li>
 * </ul>
 * 
 * <h3>异常处理优先级：</h3>
 * <ol>
 * <li>业务异常 - 返回具体的业务错误信息</li>
 * <li>参数验证异常 - 返回参数错误信息</li>
 * <li>认证授权异常 - 返回权限相关错误</li>
 * <li>数据访问异常 - 返回数据操作错误</li>
 * <li>系统异常 - 返回通用系统错误</li>
 * </ol>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 在控制器中抛出异常
 * @Controller
 * public class UserController {
 *     
 *     @RequestMapping(value = "/api/user/login", method = "POST")
 *     public Result login(@RequestParam String username, @RequestParam String password) {
 *         if (username == null || username.trim().isEmpty()) {
 *             throw new IllegalArgumentException("用户名不能为空");
 *         }
 *         
 *         User user = userService.findByUsername(username);
 *         if (user == null) {
 *             throw new BusinessException("USER_NOT_FOUND", "用户不存在");
 *         }
 *         
 *         return Result.success(user);
 *     }
 * }
 * 
 * // 异常会被GlobalExceptionHandler自动捕获并处理
 * // IllegalArgumentException -> 400 参数错误
 * // BusinessException -> 业务错误码和消息
 * }
 * </pre>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ControllerAdvice
 * @see ExceptionHandler
 * @see BusinessException
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     *
     * 业务异常通常包含具体的错误码和用户友好的错误信息，
     * 可以直接返回给客户端。
     *
     * @param e 业务异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();

        logger.warn("业务异常 - {} {} - 错误码: {}, 消息: {}",
                   method, requestUrl, e.getCode(), e.getMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数验证异常
     *
     * 包括@RequestParam、@PathVariable等参数绑定失败的异常。
     *
     * @param e 参数异常
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return 错误响应
     */
    @ExceptionHandler({
        IllegalArgumentException.class,
        NumberFormatException.class
    })
    public Result<Object> handleValidationException(Exception e,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();

        logger.warn("参数验证异常 - {} {} - 异常: {}",
                   method, requestUrl, e.getMessage());

        // 设置HTTP状态码
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        return Result.fail("INVALID_PARAM", "参数错误: " + e.getMessage());
    }
    
    /**
     * 处理认证异常
     *
     * 用户未登录或token无效时抛出的异常。
     *
     * @param e 认证异常
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Object> handleAuthenticationException(AuthenticationException e,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String requestUrl = request.getRequestURL().toString();
        String userAgent = request.getHeader("User-Agent");

        logger.warn("认证异常 - URL: {}, UserAgent: {}, 异常: {}",
                   requestUrl, userAgent, e.getMessage());

        // 设置HTTP状态码为401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        return Result.fail("UNAUTHORIZED", "认证失败: " + e.getMessage());
    }
    
    /**
     * 处理授权异常
     *
     * 用户已登录但权限不足时抛出的异常。
     *
     * @param e 授权异常
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return 错误响应
     */
    @ExceptionHandler(AuthorizationException.class)
    public Result<Object> handleAuthorizationException(AuthorizationException e,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();

        logger.warn("授权异常 - {} {} - 异常: {}",
                   method, requestUrl, e.getMessage());

        // 设置HTTP状态码为403
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        return Result.fail("FORBIDDEN", "权限不足: " + e.getMessage());
    }
    
    /**
     * 处理数据访问异常
     *
     * 数据库操作失败、连接超时等数据访问相关异常。
     *
     * @param e 数据访问异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler({
        DataAccessException.class,
        java.sql.SQLException.class
    })
    public Result<Object> handleDataAccessException(Exception e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();

        logger.error("数据访问异常 - {} {} - 异常: {}",
                    method, requestUrl, e.getMessage(), e);

        return Result.fail("DB_ERROR", "数据操作失败，请稍后重试");
    }
    
    /**
     * 处理空指针异常
     *
     * 通常是代码逻辑错误导致的，需要记录详细日志。
     *
     * @param e 空指针异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Object> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();

        logger.error("空指针异常 - {} {} - 异常位置: {}",
                    method, requestUrl, e.getStackTrace()[0], e);

        return Result.fail("SYSTEM_ERROR", "系统内部错误，请联系管理员");
    }
    
    /**
     * 处理所有未捕获的异常
     *
     * 兜底异常处理，确保所有异常都有统一的响应格式。
     *
     * @param e 未知异常
     * @param request HTTP请求对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleGenericException(Exception e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = getClientIpAddress(request);

        // 记录详细的异常信息
        logger.error("系统异常 - {} {} - IP: {}, UserAgent: {}, 参数: {}",
                    method, requestUrl, remoteAddr, userAgent,
                    request.getParameterMap(), e);

        return Result.fail("SYSTEM_ERROR", "系统内部错误，请稍后重试");
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
