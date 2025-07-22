package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 路径变量注解
 * 
 * 用于自动提取URL路径中的参数变量。支持RESTful风格的URL参数绑定。
 * 
 * 使用示例：
 * <pre>
 * {@code
 * // URL: /api/products/123
 * @RequestMapping(value = "/api/products/{id}", method = "GET")
 * public Result getProduct(@PathVariable("id") Long productId) {
 *     // productId 将自动获得值 123
 *     return productService.getById(productId);
 * }
 * 
 * // URL: /api/users/456/orders/789
 * @RequestMapping(value = "/api/users/{userId}/orders/{orderId}", method = "GET")
 * public Result getOrder(@PathVariable("userId") Long userId, 
 *                       @PathVariable("orderId") Long orderId) {
 *     return orderService.getOrder(userId, orderId);
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
public @interface PathVariable {
    
    /**
     * 路径变量的名称
     * 
     * 指定要绑定的路径变量名称。如果不指定，框架将尝试使用参数名称。
     * 
     * 示例：
     * <pre>
     * {@code
     * // 明确指定变量名
     * @PathVariable("id") Long productId
     * 
     * // 如果参数名与路径变量名相同，可以省略
     * @PathVariable Long id  // 等同于 @PathVariable("id") Long id
     * }
     * </pre>
     * 
     * @return 路径变量名称
     */
    String value() default "";
}
