package com.shiwu.framework.exception;

/**
 * 认证异常
 * 
 * 用于表示用户认证失败的异常情况，如未登录、token无效、token过期等。
 * 认证异常会被GlobalExceptionHandler捕获并返回401状态码。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>认证失败：表示用户身份验证失败</li>
 * <li>统一处理：配合GlobalExceptionHandler返回401状态码</li>
 * <li>安全保护：避免暴露敏感的认证信息</li>
 * <li>用户友好：提供清晰的认证失败原因</li>
 * </ul>
 * 
 * <h3>常见认证失败场景：</h3>
 * <ul>
 * <li>用户未登录</li>
 * <li>Token无效或格式错误</li>
 * <li>Token已过期</li>
 * <li>Token被篡改</li>
 * <li>用户账号被禁用</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 在JWT工具类中验证token
 * public class JwtUtils {
 *     
 *     public static Long getUserIdFromToken(String token) {
 *         if (token == null || token.isEmpty()) {
 *             throw new AuthenticationException("Token不能为空");
 *         }
 *         
 *         try {
 *             Claims claims = Jwts.parser()
 *                 .setSigningKey(SECRET_KEY)
 *                 .parseClaimsJws(token)
 *                 .getBody();
 *                 
 *             return claims.get("userId", Long.class);
 *         } catch (ExpiredJwtException e) {
 *             throw new AuthenticationException("Token已过期");
 *         } catch (JwtException e) {
 *             throw new AuthenticationException("Token无效");
 *         }
 *     }
 * }
 * 
 * // 在拦截器中验证用户登录状态
 * public class AuthInterceptor {
 *     
 *     public boolean preHandle(HttpServletRequest request) {
 *         String token = request.getHeader("Authorization");
 *         
 *         if (token == null) {
 *             throw new AuthenticationException("请先登录");
 *         }
 *         
 *         Long userId = JwtUtils.getUserIdFromToken(token);
 *         User user = userService.getById(userId);
 *         
 *         if (user == null) {
 *             throw new AuthenticationException("用户不存在");
 *         }
 *         
 *         if ("DISABLED".equals(user.getStatus())) {
 *             throw new AuthenticationException("账号已被禁用");
 *         }
 *         
 *         return true;
 *     }
 * }
 * 
 * // 异常会被GlobalExceptionHandler捕获，返回：
 * // HTTP 401 Unauthorized
 * // {
 * //   "code": "UNAUTHORIZED",
 * //   "message": "认证失败: Token已过期",
 * //   "data": null
 * // }
 * }
 * </pre>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see GlobalExceptionHandler
 * @see AuthorizationException
 */
public class AuthenticationException extends RuntimeException {
    
    /**
     * 构造函数
     * 
     * @param message 认证失败信息
     */
    public AuthenticationException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 认证失败信息
     * @param cause 原始异常
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // 常用认证异常的静态工厂方法
    
    /**
     * 未登录异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException notLoggedIn() {
        return new AuthenticationException("请先登录");
    }
    
    /**
     * Token无效异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Token无效");
    }
    
    /**
     * Token过期异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Token已过期，请重新登录");
    }
    
    /**
     * 账号被禁用异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("账号已被禁用");
    }
    
    /**
     * 登录失败异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException loginFailed() {
        return new AuthenticationException("用户名或密码错误");
    }
    
    /**
     * Token格式错误异常
     * 
     * @return AuthenticationException
     */
    public static AuthenticationException malformedToken() {
        return new AuthenticationException("Token格式错误");
    }
}
