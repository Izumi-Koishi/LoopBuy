package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求映射注解
 *
 * 用于映射HTTP请求到控制器的方法。可以应用在类级别和方法级别。
 * 支持RESTful风格的URL路径参数和多种HTTP方法。
 *
 * <h3>功能说明：</h3>
 * <ul>
 * <li>路径映射：将HTTP请求路径映射到具体的控制器方法</li>
 * <li>方法限制：指定允许的HTTP方法（GET、POST、PUT、DELETE等）</li>
 * <li>路径参数：支持{id}形式的路径参数</li>
 * <li>类级别映射：可以在类上定义基础路径</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 *
 * <h4>基本用法：</h4>
 * <pre>
 * {@code
 * @Controller
 * public class UserController {
 *
 *     // GET请求
 *     @RequestMapping(value = "/api/user/list", method = "GET")
 *     public Result getUserList() {
 *         return userService.getAllUsers();
 *     }
 *
 *     // POST请求
 *     @RequestMapping(value = "/api/user/login", method = "POST")
 *     public Result login(@RequestParam String username, @RequestParam String password) {
 *         return userService.login(username, password);
 *     }
 * }
 * }
 * </pre>
 *
 * <h4>路径参数用法：</h4>
 * <pre>
 * {@code
 * @Controller
 * public class ProductController {
 *
 *     // 单个路径参数
 *     @RequestMapping(value = "/api/products/{id}", method = "GET")
 *     public Result getProduct(@PathVariable("id") Long productId) {
 *         return productService.getById(productId);
 *     }
 *
 *     // 多个路径参数
 *     @RequestMapping(value = "/api/users/{userId}/orders/{orderId}", method = "GET")
 *     public Result getOrder(@PathVariable("userId") Long userId,
 *                           @PathVariable("orderId") Long orderId) {
 *         return orderService.getOrder(userId, orderId);
 *     }
 * }
 * }
 * </pre>
 *
 * <h4>类级别映射：</h4>
 * <pre>
 * {@code
 * @Controller
 * @RequestMapping("/api/admin")
 * public class AdminController {
 *
 *     // 实际路径为 /api/admin/users
 *     @RequestMapping(value = "/users", method = "GET")
 *     public Result getUsers() {
 *         return adminService.getAllUsers();
 *     }
 *
 *     // 实际路径为 /api/admin/products
 *     @RequestMapping(value = "/products", method = "GET")
 *     public Result getProducts() {
 *         return adminService.getAllProducts();
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>支持的HTTP方法：</h3>
 * <ul>
 * <li>GET - 获取资源</li>
 * <li>POST - 创建资源</li>
 * <li>PUT - 更新资源</li>
 * <li>DELETE - 删除资源</li>
 * </ul>
 *
 * <h3>路径匹配规则：</h3>
 * <ul>
 * <li>精确匹配：/api/user/login</li>
 * <li>参数匹配：/api/products/{id} 匹配 /api/products/123</li>
 * <li>类路径组合：类上的@RequestMapping + 方法上的@RequestMapping</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 * <li>路径必须以/开头</li>
 * <li>路径参数名必须与@PathVariable中的名称一致</li>
 * <li>同一个路径和方法的组合在整个应用中必须唯一</li>
 * <li>建议遵循RESTful API设计规范</li>
 * </ul>
 *
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Controller
 * @see PathVariable
 * @see RequestParam
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * 请求路径
     *
     * 指定HTTP请求的URL路径。支持静态路径和动态路径参数。
     *
     * <h4>路径格式：</h4>
     * <ul>
     * <li>静态路径：/api/user/login</li>
     * <li>动态路径：/api/products/{id}</li>
     * <li>多参数路径：/api/users/{userId}/orders/{orderId}</li>
     * </ul>
     *
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 静态路径
     * @RequestMapping("/api/user/login")
     *
     * // 动态路径
     * @RequestMapping("/api/products/{id}")
     *
     * // 复杂路径
     * @RequestMapping("/api/categories/{categoryId}/products/{productId}")
     * }
     * </pre>
     *
     * @return 请求路径
     */
    String value() default "";

    /**
     * HTTP请求方法
     *
     * 指定允许的HTTP请求方法。默认为GET方法。
     *
     * <h4>支持的方法：</h4>
     * <ul>
     * <li>GET - 用于获取数据，幂等操作</li>
     * <li>POST - 用于创建数据，非幂等操作</li>
     * <li>PUT - 用于更新数据，幂等操作</li>
     * <li>DELETE - 用于删除数据，幂等操作</li>
     * </ul>
     *
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // GET请求（默认）
     * @RequestMapping(value = "/api/users", method = "GET")
     *
     * // POST请求
     * @RequestMapping(value = "/api/users", method = "POST")
     *
     * // PUT请求
     * @RequestMapping(value = "/api/users/{id}", method = "PUT")
     *
     * // DELETE请求
     * @RequestMapping(value = "/api/users/{id}", method = "DELETE")
     * }
     * </pre>
     *
     * @return HTTP请求方法，默认为"GET"
     */
    String method() default "GET";
}
