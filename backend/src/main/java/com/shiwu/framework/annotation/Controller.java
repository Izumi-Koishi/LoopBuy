package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器注解
 *
 * 用于标记一个类为MVC控制器，该类将被框架自动扫描并注册到IoC容器中。
 * 标记了此注解的类可以处理HTTP请求，配合@RequestMapping注解定义具体的请求映射。
 *
 * <h3>功能说明：</h3>
 * <ul>
 * <li>自动扫描：框架启动时会自动扫描所有标记了@Controller的类</li>
 * <li>IoC管理：控制器实例由框架的IoC容器统一管理</li>
 * <li>依赖注入：支持通过@Autowired注解自动注入依赖的Service</li>
 * <li>请求处理：配合@RequestMapping处理HTTP请求</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * @Controller
 * public class UserController {
 *
 *     @Autowired
 *     private UserService userService;
 *
 *     @RequestMapping(value = "/api/user/login", method = "POST")
 *     public Result login(@RequestParam String username, @RequestParam String password) {
 *         return userService.login(username, password);
 *     }
 *
 *     @RequestMapping(value = "/api/user/{id}", method = "GET")
 *     public Result getUser(@PathVariable("id") Long userId) {
 *         return userService.getUserById(userId);
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 * <li>控制器类必须是public的</li>
 * <li>控制器类必须有无参构造函数</li>
 * <li>建议控制器类以Controller结尾命名</li>
 * <li>控制器方法的返回值会被自动序列化为JSON</li>
 * </ul>
 *
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see RequestMapping
 * @see Autowired
 * @see Service
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {

    /**
     * 控制器的名称
     *
     * 指定控制器在IoC容器中的名称。如果不指定，框架将使用类名的首字母小写形式作为默认名称。
     *
     * <h4>命名规则：</h4>
     * <ul>
     * <li>如果value为空：UserController -> userController</li>
     * <li>如果value不为空：使用指定的名称</li>
     * </ul>
     *
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 使用默认名称 userController
     * @Controller
     * public class UserController { }
     *
     * // 使用自定义名称 myUserController
     * @Controller("myUserController")
     * public class UserController { }
     * }
     * </pre>
     *
     * @return 控制器名称，默认为空字符串
     */
    String value() default "";
}
