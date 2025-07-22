package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动装配注解
 *
 * 用于自动注入依赖对象。框架会在运行时自动查找匹配的Bean并注入到标记的字段、构造函数或方法中。
 * 这是实现控制反转(IoC)和依赖注入(DI)的核心注解。
 *
 * <h3>功能说明：</h3>
 * <ul>
 * <li>自动注入：框架自动查找并注入匹配类型的Bean</li>
 * <li>类型匹配：根据字段或参数的类型查找对应的Bean</li>
 * <li>单例管理：注入的Bean由IoC容器统一管理</li>
 * <li>循环依赖检测：框架会检测并处理简单的循环依赖</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 *
 * <h4>字段注入（推荐）：</h4>
 * <pre>
 * {@code
 * @Controller
 * public class UserController {
 *
 *     @Autowired
 *     private UserService userService;
 *
 *     @Autowired
 *     private EmailService emailService;
 *
 *     @RequestMapping(value = "/api/user/register", method = "POST")
 *     public Result register(@RequestParam String username, @RequestParam String email) {
 *         Result result = userService.register(username, email);
 *         if (result.isSuccess()) {
 *             emailService.sendWelcomeEmail(email);
 *         }
 *         return result;
 *     }
 * }
 * }
 * </pre>
 *
 * <h4>Service层依赖注入：</h4>
 * <pre>
 * {@code
 * @Service
 * public class UserServiceImpl implements UserService {
 *
 *     @Autowired
 *     private UserDao userDao;
 *
 *     @Autowired
 *     private PasswordEncoder passwordEncoder;
 *
 *     @Override
 *     public Result login(String username, String password) {
 *         User user = userDao.findByUsername(username);
 *         if (user != null && passwordEncoder.matches(password, user.getPassword())) {
 *             return Result.success(new UserVO(user));
 *         }
 *         return Result.error("用户名或密码错误");
 *     }
 * }
 * }
 * </pre>
 *
 * <h4>可选依赖注入：</h4>
 * <pre>
 * {@code
 * @Service
 * public class NotificationService {
 *
 *     @Autowired
 *     private EmailService emailService;
 *
 *     // 可选依赖，如果找不到对应的Bean，不会抛出异常
 *     @Autowired(required = false)
 *     private SmsService smsService;
 *
 *     public void sendNotification(String message, String contact) {
 *         // 邮件通知（必需）
 *         emailService.send(message, contact);
 *
 *         // 短信通知（可选）
 *         if (smsService != null) {
 *             smsService.send(message, contact);
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>注入规则：</h3>
 * <ul>
 * <li>按类型匹配：优先根据字段类型查找Bean</li>
 * <li>接口注入：如果字段是接口类型，查找实现该接口的Bean</li>
 * <li>唯一性检查：如果找到多个匹配的Bean，会抛出异常</li>
 * <li>必需性检查：如果required=true且找不到Bean，会抛出异常</li>
 * </ul>
 *
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>优先使用字段注入，代码简洁</li>
 * <li>依赖接口而不是具体实现类</li>
 * <li>避免循环依赖，合理设计类的依赖关系</li>
 * <li>对于可选依赖，设置required=false</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 * <li>被注入的字段不需要是public的</li>
 * <li>被注入的Bean必须标记了@Service、@Controller等注解</li>
 * <li>避免在构造函数中使用被注入的字段</li>
 * <li>注入发生在对象创建之后，构造函数执行之后</li>
 * </ul>
 *
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Service
 * @see Controller
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

    /**
     * 是否必需
     *
     * 指定依赖注入是否是必需的。如果设置为true，当找不到匹配的Bean时会抛出异常。
     * 如果设置为false，当找不到匹配的Bean时会忽略注入，字段值保持为null。
     *
     * <h4>使用场景：</h4>
     * <ul>
     * <li>required=true：核心依赖，必须存在</li>
     * <li>required=false：可选依赖，可以不存在</li>
     * </ul>
     *
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * @Service
     * public class UserService {
     *
     *     // 必需依赖，找不到会抛出异常
     *     @Autowired(required = true)
     *     private UserDao userDao;
     *
     *     // 可选依赖，找不到会保持null
     *     @Autowired(required = false)
     *     private CacheService cacheService;
     *
     *     public User getUser(Long id) {
     *         // 先尝试从缓存获取
     *         if (cacheService != null) {
     *             User cached = cacheService.get("user:" + id);
     *             if (cached != null) return cached;
     *         }
     *
     *         // 从数据库获取
     *         return userDao.findById(id);
     *     }
     * }
     * }
     * </pre>
     *
     * @return 是否必需，默认为true
     */
    boolean required() default true;
}
