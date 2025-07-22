package com.shiwu.user.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户关注关系数据访问对象 - MVC框架版本
 */
@Repository
public class FollowDao {
    private static final Logger logger = LoggerFactory.getLogger(FollowDao.class);

    /**
     * 获取总关注关系数
     * @return 总关注关系数
     */
    public Long getTotalFollowCount() {
        String sql = "SELECT COUNT(*) FROM user_follow WHERE is_deleted = 0";
        return executeCountQuery(sql);
    }
    
    /**
     * 获取指定时间段内新增关注关系数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 新增关注关系数
     */
    public Long getNewFollowCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM user_follow WHERE is_deleted = 0 AND create_time >= ? AND create_time < ?";
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
            logger.error("查询新增关注关系数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }
    
    /**
     * 获取关注关系增长趋势数据（按天统计）
     * @param days 统计天数
     * @return 趋势数据列表，每个元素包含日期和当天新增关注关系数
     */
    public List<Map<String, Object>> getFollowGrowthTrend(int days) {
        // 参数验证
        if (days < 0) {
            logger.warn("获取关注增长趋势失败: days无效: {}", days);
            return new ArrayList<>();
        }

        String sql = "SELECT DATE(create_time) as date, COUNT(*) as count " +
                    "FROM user_follow " +
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
            logger.error("查询关注关系增长趋势失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return trendData;
    }
    
    /**
     * 获取最活跃的用户（按关注者数量排序）
     * @param limit 返回数量限制
     * @return 活跃用户列表
     */
    public List<Map<String, Object>> getMostFollowedUsers(int limit) {
        // 参数验证
        if (limit <= 0) {
            logger.warn("获取最受关注用户失败: limit无效: {}", limit);
            return new ArrayList<>();
        }

        String sql = "SELECT u.id, u.username, u.nickname, COUNT(f.follower_id) as follower_count " +
                    "FROM system_user u " +
                    "LEFT JOIN user_follow f ON u.id = f.followed_id AND f.is_deleted = 0 " +
                    "WHERE u.is_deleted = 0 " +
                    "GROUP BY u.id, u.username, u.nickname " +
                    "ORDER BY follower_count DESC " +
                    "LIMIT ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> users = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getLong("id"));
                user.put("username", rs.getString("username"));
                user.put("nickname", rs.getString("nickname"));
                user.put("followerCount", rs.getLong("follower_count"));
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("查询最受关注用户失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return users;
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
}
