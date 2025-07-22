package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务层注解
 *
 * 用于标记一个类为业务逻辑服务组件，该类将被框架自动扫描并注册到IoC容器中。
 * 标记了此注解的类通常包含业务逻辑处理，是MVC架构中的Service层。
 *
 * <h3>功能说明：</h3>
 * <ul>
 * <li>自动扫描：框架启动时会自动扫描所有标记了@Service的类</li>
 * <li>IoC管理：服务实例由框架的IoC容器统一管理，默认为单例模式</li>
 * <li>依赖注入：可以被@Autowired注解自动注入到其他组件中</li>
 * <li>事务支持：服务层方法可以参与事务管理</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * @Service
 * public class UserServiceImpl implements UserService {
 *
 *     @Autowired
 *     private UserDao userDao;
 *
 *     @Override
 *     public Result login(String username, String password) {
 *         // 业务逻辑处理
 *         User user = userDao.findByUsername(username);
 *         if (user != null && PasswordUtils.verify(password, user.getPassword())) {
 *             String token = JwtUtils.generateToken(user);
 *             return Result.success(new UserVO(user, token));
 *         }
 *         return Result.error("用户名或密码错误");
 *     }
 *
 *     @Override
 *     public Result register(String username, String password, String email) {
 *         // 注册业务逻辑
 *         if (userDao.existsByUsername(username)) {
 *             return Result.error("用户名已存在");
 *         }
 *
 *         User user = new User();
 *         user.setUsername(username);
 *         user.setPassword(PasswordUtils.encode(password));
 *         user.setEmail(email);
 *         userDao.save(user);
 *
 *         return Result.success("注册成功");
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>服务类建议实现对应的接口，便于测试和解耦</li>
 * <li>服务类名建议以ServiceImpl结尾</li>
 * <li>业务逻辑应该放在Service层，Controller层只负责请求处理</li>
 * <li>复杂的业务逻辑可以拆分为多个Service</li>
 * <li>Service之间可以相互依赖注入</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 * <li>服务类必须是public的</li>
 * <li>服务类必须有无参构造函数</li>
 * <li>避免在Service中直接处理HTTP请求和响应</li>
 * <li>Service方法应该抛出业务异常而不是HTTP异常</li>
 * </ul>
 *
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Controller
 * @see Autowired
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {

    /**
     * 服务的名称
     *
     * 指定服务在IoC容器中的名称。如果不指定，框架将使用类名的首字母小写形式作为默认名称。
     *
     * <h4>命名规则：</h4>
     * <ul>
     * <li>如果value为空：UserServiceImpl -> userServiceImpl</li>
     * <li>如果value不为空：使用指定的名称</li>
     * </ul>
     *
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 使用默认名称 userServiceImpl
     * @Service
     * public class UserServiceImpl implements UserService { }
     *
     * // 使用自定义名称 userService
     * @Service("userService")
     * public class UserServiceImpl implements UserService { }
     * }
     * </pre>
     *
     * @return 服务名称，默认为空字符串
     */
    String value() default "";
}
