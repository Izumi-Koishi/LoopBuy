package com.shiwu.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据访问层注解
 * 
 * 用于标记一个类为数据访问层组件，该类将被框架自动扫描并注册到IoC容器中。
 * 标记了此注解的类通常负责数据持久化操作，是MVC架构中的Repository/DAO层。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>自动扫描：框架启动时会自动扫描所有标记了@Repository的类</li>
 * <li>IoC管理：Repository实例由框架的IoC容器统一管理，默认为单例模式</li>
 * <li>依赖注入：可以被@Autowired注解自动注入到其他组件中</li>
 * <li>异常转换：可以将数据访问异常转换为统一的业务异常</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * @Repository
 * public class UserRepository {
 *     
 *     private static final String JDBC_URL = "jdbc:mysql://localhost:3306/loopbuy";
 *     
 *     public User findById(Long id) {
 *         try (Connection conn = DriverManager.getConnection(JDBC_URL);
 *              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
 *             
 *             stmt.setLong(1, id);
 *             ResultSet rs = stmt.executeQuery();
 *             
 *             if (rs.next()) {
 *                 User user = new User();
 *                 user.setId(rs.getLong("id"));
 *                 user.setUsername(rs.getString("username"));
 *                 user.setEmail(rs.getString("email"));
 *                 return user;
 *             }
 *             return null;
 *         } catch (SQLException e) {
 *             throw new DataAccessException("查询用户失败", e);
 *         }
 *     }
 *     
 *     public void save(User user) {
 *         String sql = user.getId() == null ? 
 *             "INSERT INTO users (username, email, password) VALUES (?, ?, ?)" :
 *             "UPDATE users SET username=?, email=?, password=? WHERE id=?";
 *             
 *         try (Connection conn = DriverManager.getConnection(JDBC_URL);
 *              PreparedStatement stmt = conn.prepareStatement(sql)) {
 *             
 *             stmt.setString(1, user.getUsername());
 *             stmt.setString(2, user.getEmail());
 *             stmt.setString(3, user.getPassword());
 *             
 *             if (user.getId() != null) {
 *                 stmt.setLong(4, user.getId());
 *             }
 *             
 *             stmt.executeUpdate();
 *         } catch (SQLException e) {
 *             throw new DataAccessException("保存用户失败", e);
 *         }
 *     }
 *     
 *     public List<User> findByStatus(String status) {
 *         List<User> users = new ArrayList<>();
 *         String sql = "SELECT * FROM users WHERE status = ?";
 *         
 *         try (Connection conn = DriverManager.getConnection(JDBC_URL);
 *              PreparedStatement stmt = conn.prepareStatement(sql)) {
 *             
 *             stmt.setString(1, status);
 *             ResultSet rs = stmt.executeQuery();
 *             
 *             while (rs.next()) {
 *                 User user = new User();
 *                 user.setId(rs.getLong("id"));
 *                 user.setUsername(rs.getString("username"));
 *                 user.setEmail(rs.getString("email"));
 *                 user.setStatus(rs.getString("status"));
 *                 users.add(user);
 *             }
 *         } catch (SQLException e) {
 *             throw new DataAccessException("查询用户列表失败", e);
 *         }
 *         
 *         return users;
 *     }
 * }
 * }
 * </pre>
 * 
 * <h3>最佳实践：</h3>
 * <ul>
 * <li>Repository类建议以Repository结尾命名</li>
 * <li>每个实体对应一个Repository类</li>
 * <li>Repository只负责数据访问，不包含业务逻辑</li>
 * <li>使用连接池管理数据库连接</li>
 * <li>统一异常处理，将SQLException转换为业务异常</li>
 * </ul>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 * <li>Repository类必须是public的</li>
 * <li>Repository类必须有无参构造函数</li>
 * <li>避免在Repository中处理业务逻辑</li>
 * <li>确保数据库连接的正确关闭</li>
 * <li>注意SQL注入防护</li>
 * </ul>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see Service
 * @see Controller
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
    
    /**
     * Repository的名称
     * 
     * 指定Repository在IoC容器中的名称。如果不指定，框架将使用类名的首字母小写形式作为默认名称。
     * 
     * <h4>命名规则：</h4>
     * <ul>
     * <li>如果value为空：UserRepository -> userRepository</li>
     * <li>如果value不为空：使用指定的名称</li>
     * </ul>
     * 
     * <h4>使用示例：</h4>
     * <pre>
     * {@code
     * // 使用默认名称 userRepository
     * @Repository
     * public class UserRepository { }
     * 
     * // 使用自定义名称 userDao
     * @Repository("userDao")
     * public class UserRepository { }
     * }
     * </pre>
     * 
     * @return Repository名称，默认为空字符串
     */
    String value() default "";
}
