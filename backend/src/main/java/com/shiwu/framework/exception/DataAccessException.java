package com.shiwu.framework.exception;

/**
 * 数据访问异常
 * 
 * 用于表示数据访问层的异常情况，如数据库连接失败、SQL执行错误、事务回滚等。
 * 数据访问异常会被GlobalExceptionHandler捕获并返回统一的数据操作错误信息。
 * 
 * <h3>功能说明：</h3>
 * <ul>
 * <li>数据层异常：封装数据访问相关的异常</li>
 * <li>统一处理：配合GlobalExceptionHandler统一处理</li>
 * <li>安全保护：避免暴露数据库结构信息</li>
 * <li>用户友好：提供简化的错误信息</li>
 * </ul>
 * 
 * <h3>常见数据访问异常场景：</h3>
 * <ul>
 * <li>数据库连接失败</li>
 * <li>SQL语法错误</li>
 * <li>约束违反（主键冲突、外键约束等）</li>
 * <li>事务超时</li>
 * <li>数据库锁等待超时</li>
 * <li>连接池耗尽</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 在Repository层捕获并转换SQLException
 * @Repository
 * public class UserRepository {
 *     
 *     public User findById(Long id) {
 *         String sql = "SELECT * FROM users WHERE id = ?";
 *         
 *         try (Connection conn = getConnection();
 *              PreparedStatement stmt = conn.prepareStatement(sql)) {
 *             
 *             stmt.setLong(1, id);
 *             ResultSet rs = stmt.executeQuery();
 *             
 *             if (rs.next()) {
 *                 return mapToUser(rs);
 *             }
 *             return null;
 *             
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
 *         try (Connection conn = getConnection();
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
 *             int affected = stmt.executeUpdate();
 *             if (affected == 0) {
 *                 throw new DataAccessException("保存用户失败，没有记录被更新");
 *             }
 *             
 *         } catch (SQLException e) {
 *             if (e.getErrorCode() == 1062) { // MySQL重复键错误
 *                 throw new DataAccessException("用户名或邮箱已存在", e);
 *             }
 *             throw new DataAccessException("保存用户失败", e);
 *         }
 *     }
 *     
 *     public void delete(Long id) {
 *         String sql = "DELETE FROM users WHERE id = ?";
 *         
 *         try (Connection conn = getConnection();
 *              PreparedStatement stmt = conn.prepareStatement(sql)) {
 *             
 *             stmt.setLong(1, id);
 *             int affected = stmt.executeUpdate();
 *             
 *             if (affected == 0) {
 *                 throw new DataAccessException("删除用户失败，用户不存在");
 *             }
 *             
 *         } catch (SQLException e) {
 *             if (e.getErrorCode() == 1451) { // MySQL外键约束错误
 *                 throw new DataAccessException("无法删除用户，存在关联数据", e);
 *             }
 *             throw new DataAccessException("删除用户失败", e);
 *         }
 *     }
 * }
 * 
 * // 异常会被GlobalExceptionHandler捕获，返回：
 * // {
 * //   "code": "DB_ERROR",
 * //   "message": "数据操作失败，请稍后重试",
 * //   "data": null
 * // }
 * }
 * </pre>
 * 
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 * @see GlobalExceptionHandler
 */
public class DataAccessException extends RuntimeException {
    
    /**
     * 构造函数
     * 
     * @param message 数据访问错误信息
     */
    public DataAccessException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 数据访问错误信息
     * @param cause 原始异常
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // 常用数据访问异常的静态工厂方法
    
    /**
     * 数据库连接失败异常
     * 
     * @return DataAccessException
     */
    public static DataAccessException connectionFailed() {
        return new DataAccessException("数据库连接失败");
    }
    
    /**
     * SQL执行失败异常
     * 
     * @param operation 操作类型
     * @return DataAccessException
     */
    public static DataAccessException sqlExecutionFailed(String operation) {
        return new DataAccessException(operation + "操作失败");
    }
    
    /**
     * 数据不存在异常
     * 
     * @param entity 实体名称
     * @return DataAccessException
     */
    public static DataAccessException dataNotFound(String entity) {
        return new DataAccessException(entity + "不存在");
    }
    
    /**
     * 数据重复异常
     * 
     * @param field 字段名称
     * @return DataAccessException
     */
    public static DataAccessException duplicateData(String field) {
        return new DataAccessException(field + "已存在");
    }
    
    /**
     * 外键约束异常
     * 
     * @return DataAccessException
     */
    public static DataAccessException foreignKeyConstraint() {
        return new DataAccessException("存在关联数据，无法删除");
    }
    
    /**
     * 事务超时异常
     * 
     * @return DataAccessException
     */
    public static DataAccessException transactionTimeout() {
        return new DataAccessException("事务超时");
    }
    
    /**
     * 连接池耗尽异常
     * 
     * @return DataAccessException
     */
    public static DataAccessException connectionPoolExhausted() {
        return new DataAccessException("数据库连接池耗尽");
    }
    
    /**
     * 数据库锁等待超时异常
     * 
     * @return DataAccessException
     */
    public static DataAccessException lockWaitTimeout() {
        return new DataAccessException("数据库锁等待超时");
    }
}
