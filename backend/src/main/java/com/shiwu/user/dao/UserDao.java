package com.shiwu.user.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.user.model.User;
import com.shiwu.product.model.ProductCardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户数据访问对象 - MVC框架版本
 */
@Repository
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    public User findByUsername(String username) {
        // 参数验证
        if (username == null) {
            logger.warn("查询用户失败: 用户名为null");
            return null;
        }
        if (username.trim().isEmpty()) {
            logger.warn("查询用户失败: 用户名为空字符串");
            return null;
        }

        String sql = "SELECT id, username, password, email, phone, status, avatar_url, nickname, gender, bio, follower_count, average_rating, last_login_time, create_time FROM system_user WHERE username = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setStatus(rs.getInt("status"));
                user.setAvatarUrl(rs.getString("avatar_url"));
                user.setNickname(rs.getString("nickname"));
                user.setGender(rs.getInt("gender"));
                user.setBio(rs.getString("bio"));
                user.setFollowerCount(rs.getInt("follower_count"));
                user.setAverageRating(rs.getBigDecimal("average_rating"));
                user.setLastLoginTime(rs.getObject("last_login_time", LocalDateTime.class));
                user.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
            }
        } catch (SQLException e) {
            logger.error("查询用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return user;
    }
    
    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    public User findById(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("查询用户失败: 用户ID为null");
            return null;
        }
        if (userId <= 0) {
            logger.warn("查询用户失败: 用户ID无效: {}", userId);
            return null;
        }

        String sql = "SELECT id, username, password, email, phone, status, avatar_url, nickname, gender, bio, follower_count, average_rating, last_login_time, create_time FROM system_user WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setStatus(rs.getInt("status"));
                user.setAvatarUrl(rs.getString("avatar_url"));
                user.setNickname(rs.getString("nickname"));
                user.setGender(rs.getInt("gender"));
                user.setBio(rs.getString("bio"));
                user.setFollowerCount(rs.getInt("follower_count"));
                user.setAverageRating(rs.getBigDecimal("average_rating"));
                user.setLastLoginTime(rs.getObject("last_login_time", LocalDateTime.class));
                user.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
            }
        } catch (SQLException e) {
            logger.error("根据ID查询用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return user;
    }
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象，如果不存在则返回null
     */
    public User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        
        String sql = "SELECT id, username, password, email, phone, nickname, gender, bio, follower_count, average_rating, status, last_login_time FROM system_user WHERE email = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setNickname(rs.getString("nickname"));
                user.setGender(rs.getInt("gender"));
                user.setBio(rs.getString("bio"));
                user.setFollowerCount(rs.getInt("follower_count"));
                user.setAverageRating(rs.getBigDecimal("average_rating"));
                user.setStatus(rs.getInt("status"));
                user.setLastLoginTime(rs.getObject("last_login_time", LocalDateTime.class));
            }
        } catch (SQLException e) {
            logger.error("根据邮箱查询用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return user;
    }
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户对象，如果不存在则返回null
     */
    public User findByPhone(String phone) {
        if (phone == null) {
            return null;
        }
        
        String sql = "SELECT id FROM system_user WHERE phone = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, phone);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            logger.error("根据手机号查询用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return user;
    }
    
    /**
     * 创建新用户
     * @param user 用户对象
     * @return 创建成功返回用户ID，失败返回null
     */
    public Long createUser(User user) {
        String sql = "INSERT INTO system_user (username, password, email, phone, status, nickname) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long userId = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPhone());
            pstmt.setInt(5, user.getStatus() != null ? user.getStatus() : 0);
            pstmt.setString(6, user.getNickname());
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    userId = rs.getLong(1);
                    logger.info("创建用户成功, ID: {}", userId);
                }
            }
        } catch (SQLException e) {
            logger.error("创建用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return userId;
    }
    
    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码（已哈希）
     * @return 是否更新成功
     */
    public boolean updatePassword(Long userId, String newPassword) {
        // 参数验证
        if (userId == null) {
            logger.warn("更新用户密码失败: 用户ID为null");
            return false;
        }
        if (userId <= 0) {
            logger.warn("更新用户密码失败: 用户ID无效: {}", userId);
            return false;
        }
        if (newPassword == null) {
            logger.warn("更新用户密码失败: 新密码为null");
            return false;
        }

        String sql = "UPDATE system_user SET password = ? WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPassword);
            pstmt.setLong(2, userId);
            int rows = pstmt.executeUpdate();
            success = rows > 0;
            
            if (success) {
                logger.info("更新用户 {} 密码成功", userId);
            } else {
                logger.warn("更新用户 {} 密码失败: 用户不存在或已删除", userId);
            }
        } catch (SQLException e) {
            logger.error("更新用户密码失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }
        
        return success;
    }
    
    /**
     * 更新用户最后登录时间
     * @param userId 用户ID
     * @return 是否更新成功
     */
    public boolean updateLastLoginTime(Long userId) {
        String sql = "UPDATE system_user SET last_login_time = ? WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setObject(1, LocalDateTime.now());
            pstmt.setLong(2, userId);
            int rows = pstmt.executeUpdate();
            success = rows > 0;
            
            if (success) {
                logger.info("更新用户 {} 最后登录时间成功", userId);
            } else {
                logger.warn("更新用户 {} 最后登录时间失败: 用户不存在或已删除", userId);
            }
        } catch (SQLException e) {
            logger.error("更新用户最后登录时间失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }
        
        return success;
    }

    /**
     * 根据用户ID获取用户公开信息（不包含敏感信息）
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    public User findPublicInfoById(Long userId) {
        String sql = "SELECT id, username, nickname, avatar_url, status, follower_count, average_rating, create_time FROM system_user WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setNickname(rs.getString("nickname"));
                user.setAvatarUrl(rs.getString("avatar_url"));
                user.setStatus(rs.getInt("status"));
                user.setFollowerCount(rs.getInt("follower_count"));
                user.setAverageRating(rs.getBigDecimal("average_rating"));
                user.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
            }
        } catch (SQLException e) {
            logger.error("根据ID查询用户公开信息失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return user;
    }

    /**
     * 获取用户的在售商品列表
     * @param userId 用户ID
     * @return 在售商品列表
     */
    public List<ProductCardVO> findOnSaleProductsByUserId(Long userId) {
        // 根据模块解耦原则，UserDao不应该直接查询product表
        // 这个方法应该被移除，改为在UserService中调用ProductService
        logger.warn("UserDao.findOnSaleProductsByUserId方法已废弃，请使用ProductService.getProductsBySellerIdAndStatus方法");
        return new ArrayList<>();
    }

    // ====================================================================
    // 统计查询方法（用于管理员仪表盘）
    // ====================================================================

    /**
     * 获取用户总数
     * @return 用户总数
     */
    public Long getTotalUserCount() {
        String sql = "SELECT COUNT(*) FROM system_user WHERE is_deleted = 0";
        return executeCountQuery(sql);
    }

    /**
     * 获取活跃用户数（最近30天登录）
     * @return 活跃用户数
     */
    public Long getActiveUserCount() {
        String sql = "SELECT COUNT(*) FROM system_user WHERE is_deleted = 0 AND last_login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        return executeCountQuery(sql);
    }

    /**
     * 获取指定时间段内新增用户数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 新增用户数
     */
    public Long getNewUserCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM system_user WHERE is_deleted = 0 AND create_time >= ? AND create_time < ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long count = 0L;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setObject(1, startTime);
            pstmt.setObject(2, endTime);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("查询新增用户数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取指定状态的用户数
     * @param status 用户状态（0-正常，1-已封禁，2-已禁言）
     * @return 用户数
     */
    public Long getUserCountByStatus(Integer status) {
        String sql = "SELECT COUNT(*) FROM system_user WHERE is_deleted = 0 AND status = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long count = 0L;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("查询指定状态用户数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取平台平均评分
     * @return 平均评分
     */
    public Double getAverageRating() {
        String sql = "SELECT AVG(average_rating) FROM system_user WHERE is_deleted = 0 AND average_rating > 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Double avgRating = 0.0;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                avgRating = rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("查询平均评分失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return avgRating;
    }
    /**
     * 更新用户平均评分
     * @param userId 用户ID
     * @param averageRating 新的平均评分
     * @return 是否更新成功
     */
    public boolean updateAverageRating(Long userId, BigDecimal averageRating) {
        String sql = "UPDATE system_user SET average_rating = ?, update_time = NOW() WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setBigDecimal(1, averageRating);
            pstmt.setLong(2, userId);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                logger.info("更新用户平均评分成功: userId={}, averageRating={}", userId, averageRating);
                return true;
            }
        } catch (SQLException e) {
            logger.error("更新用户平均评分失败: userId={}, averageRating={}, error={}", userId, averageRating, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }

        return false;
    }

    /**
     * 计算用户的平均评分（基于所有评价）
     * @param userId 用户ID
     * @return 平均评分，如果没有评价则返回null
     */
    public BigDecimal calculateUserAverageRating(Long userId) {
        String sql = "SELECT AVG(r.rating) FROM review r " +
                    "INNER JOIN trade_order o ON r.order_id = o.id " +
                    "WHERE o.seller_id = ? AND r.is_deleted = 0 AND o.is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                BigDecimal avgRating = rs.getBigDecimal(1);
                if (avgRating != null) {
                    // 保留两位小数
                    return avgRating.setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (SQLException e) {
            logger.error("计算用户平均评分失败: userId={}, error={}", userId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return null;
    }

    /**
     * 获取用户增长趋势数据（按天统计）
     * @param days 统计天数
     * @return 趋势数据列表，每个元素包含日期和当天新增用户数
     */
    public List<Map<String, Object>> getUserGrowthTrend(int days) {
        String sql = "SELECT DATE(create_time) as date, COUNT(*) as count " +
                    "FROM system_user " +
                    "WHERE is_deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "GROUP BY DATE(create_time) " +
                    "ORDER BY date";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> trendData = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, days);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("date", rs.getDate("date").toString());
                data.put("count", rs.getLong("count"));
                trendData.add(data);
            }
        } catch (SQLException e) {
            logger.error("查询用户增长趋势失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return trendData;
    }

    /**
     * 执行计数查询的通用方法
     * @param sql SQL查询语句
     * @return 计数结果
     */
    private Long executeCountQuery(String sql) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long count = 0L;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("数据库连接为空");
                return count;
            }

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("执行计数查询失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("执行计数查询时发生未知异常: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("关闭ResultSet失败: {}", e.getMessage(), e);
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                logger.error("关闭PreparedStatement失败: {}", e.getMessage(), e);
            }
        }
        DBUtil.closeConnection(conn);
    }

    /**
     * 获取所有活跃用户ID列表
     * 用于Task4_2_1_2: 系统公告通知功能
     *
     * @return 活跃用户ID列表
     */
    public List<Long> getAllActiveUserIds() {
        String sql = "SELECT id FROM system_user WHERE status = 0 AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            List<Long> userIds = new ArrayList<>();
            while (rs.next()) {
                userIds.add(rs.getLong("id"));
            }

            logger.debug("获取活跃用户ID列表成功: count={}", userIds.size());
            return userIds;

        } catch (SQLException e) {
            logger.error("获取活跃用户ID列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                logger.error("关闭数据库资源失败: {}", e.getMessage(), e);
            }
            DBUtil.closeConnection(conn);
        }
    }
}