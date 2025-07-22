package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求参数注解
 * 
 * 用于自动提取HTTP请求中的参数，支持查询参数(GET)和表单参数(POST)。
 * 
 * 使用示例：
 * <pre>
 * {@code
 * // GET /api/products?page=1&size=10&category=electronics
 * @RequestMapping(value = "/api/products", method = "GET")
 * public Result getProducts(
 *     @RequestParam(value = "page", defaultValue = "1") Integer page,
 *     @RequestParam(value = "size", defaultValue = "10") Integer size,
 *     @RequestParam(value = "category", required = false) String category
 * ) {
 *     return productService.getProducts(page, size, category);
 * }
 * 
 * // POST /api/user/login
 * // Form data: username=test&password=123456
 * @RequestMapping(value = "/api/user/login", method = "POST")
 * public Result login(
 *     @RequestParam("username") String username,
 *     @RequestParam("password") String password
 * ) {
 *     return userService.login(username, password);
 * }
 * }
 * </pre>
 * 
 * 支持的类型转换：
 * <ul>
 * <li>String - 直接使用</li>
 * <li>Long/long - 自动转换</li>
 * <li>Integer/int - 自动转换</li>
 * <li>Boolean/boolean - 自动转换</li>
 * </ul>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    
    /**
     * 参数名称
     * 
     * 指定要绑定的请求参数名称。如果不指定，框架将尝试使用参数名称。
     * 
     * 示例：
     * <pre>
     * {@code
     * // 明确指定参数名
     * @RequestParam("username") String username
     * 
     * // 如果参数名与请求参数名相同，可以省略
     * @RequestParam String username  // 等同于 @RequestParam("username") String username
     * }
     * </pre>
     * 
     * @return 参数名称
     */
    String value() default "";
    
    /**
     * 默认值
     * 
     * 当请求中不存在该参数时，使用此默认值。
     * 
     * 示例：
     * <pre>
     * {@code
     * @RequestParam(value = "page", defaultValue = "1") Integer page
     * }
     * </pre>
     * 
     * @return 默认值
     */
    String defaultValue() default "";
    
    /**
     * 是否必需
     * 
     * 如果设置为true，当请求中不存在该参数且没有设置默认值时，将抛出异常。
     * 如果设置为false，当请求中不存在该参数时，将使用null值。
     * 
     * 示例：
     * <pre>
     * {@code
     * // 必需参数
     * @RequestParam(value = "username", required = true) String username
     * 
     * // 可选参数
     * @RequestParam(value = "category", required = false) String category
     * }
     * </pre>
     * 
     * @return 是否必需
     */
    boolean required() default true;
}
