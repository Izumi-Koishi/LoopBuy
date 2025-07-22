package com.shiwu.user.dao;

import com.shiwu.admin.model.AdminUserQueryDTO;
import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户数据访问对象 - MVC框架版本
 */
@Repository
public class AdminUserDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserDao.class);

    /**
     * 查询用户列表（管理员视角）
     */
    public List<Map<String, Object>> findUsers(AdminUserQueryDTO queryDTO) {
        List<Map<String, Object>> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            
            // 构建查询SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id, username, email, phone, nickname, status, avatar_url, ");
            sql.append("gender, bio, follower_count, average_rating, last_login_time, ");
            sql.append("create_time, update_time ");
            sql.append("FROM system_user WHERE is_deleted = 0 ");
            
            List<Object> params = new ArrayList<>();

            // 参数验证
            if (queryDTO == null) {
                queryDTO = new AdminUserQueryDTO();
            }

            // 添加搜索条件
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                sql.append("AND (username LIKE ? OR nickname LIKE ? OR email LIKE ?) ");
                String keyword = "%" + queryDTO.getKeyword().trim() + "%";
                params.add(keyword);
                params.add(keyword);
                params.add(keyword);
            }
            
            // 添加状态筛选
            if (queryDTO.getStatus() != null) {
                sql.append("AND status = ? ");
                params.add(queryDTO.getStatus());
            }
            
            // 添加排序
            sql.append("ORDER BY ").append(queryDTO.getSortBy()).append(" ").append(queryDTO.getSortDirection()).append(" ");
            
            // 参数验证和修正
            int pageSize = queryDTO.getPageSize();
            int pageNum = queryDTO.getPageNum();
            if (pageSize <= 0 || pageSize > 1000) {
                pageSize = 20;
            }
            if (pageNum <= 0) {
                pageNum = 1;
            }

            // 防止offset溢出
            long offset = (long)(pageNum - 1) * pageSize;
            if (offset < 0 || offset > Integer.MAX_VALUE) {
                offset = 0;
                pageNum = 1;
            }

            // 添加分页
            sql.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((int)offset);
            
            pstmt = conn.prepareStatement(sql.toString());
            
            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getLong("id"));
                user.put("username", rs.getString("username"));
                user.put("email", rs.getString("email"));
                user.put("phone", rs.getString("phone"));
                user.put("nickname", rs.getString("nickname"));
                user.put("status", rs.getInt("status"));
                user.put("statusText", getStatusText(rs.getInt("status")));
                user.put("avatarUrl", rs.getString("avatar_url"));
                user.put("gender", rs.getInt("gender"));
                user.put("bio", rs.getString("bio"));
                user.put("followerCount", rs.getInt("follower_count"));
                user.put("averageRating", rs.getBigDecimal("average_rating"));
                user.put("lastLoginTime", rs.getTimestamp("last_login_time") != null ? 
                         rs.getTimestamp("last_login_time").toLocalDateTime() : null);
                user.put("createTime", rs.getTimestamp("create_time").toLocalDateTime());
                user.put("updateTime", rs.getTimestamp("update_time").toLocalDateTime());
                
                users.add(user);
            }
            
        } catch (SQLException e) {
            logger.error("查询用户列表失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return users;
    }

    /**
     * 统计用户数量
     */
    public int countUsers(AdminUserQueryDTO queryDTO) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;

        try {
            conn = DBUtil.getConnection();
            
            // 构建统计SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) FROM system_user WHERE is_deleted = 0 ");
            
            List<Object> params = new ArrayList<>();

            // 参数验证
            if (queryDTO == null) {
                queryDTO = new AdminUserQueryDTO();
            }

            // 添加搜索条件
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                sql.append("AND (username LIKE ? OR nickname LIKE ? OR email LIKE ?) ");
                String keyword = "%" + queryDTO.getKeyword().trim() + "%";
                params.add(keyword);
                params.add(keyword);
                params.add(keyword);
            }
            
            // 添加状态筛选
            if (queryDTO.getStatus() != null) {
                sql.append("AND status = ? ");
                params.add(queryDTO.getStatus());
            }
            
            pstmt = conn.prepareStatement(sql.toString());
            
            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt(1);
            }
            
        } catch (SQLException e) {
            logger.error("统计用户数量失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 更新用户状态
     */
    public boolean updateUserStatus(Long userId, Integer status, Long adminId) {
        if (userId == null || status == null || adminId == null) {
            logger.warn("更新用户状态失败: 参数为空");
            return false;
        }

        // 验证status范围（假设有效范围是0-2）
        if (status < 0 || status > 2) {
            logger.warn("更新用户状态失败: 状态值无效: {}", status);
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection();
            
            String sql = "UPDATE system_user SET status = ?, update_time = ? WHERE id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setObject(2, LocalDateTime.now());
            pstmt.setLong(3, userId);
            
            int rows = pstmt.executeUpdate();
            success = rows > 0;
            
            if (success) {
                logger.info("管理员 {} 更新用户 {} 状态为 {} 成功", adminId, userId, status);
            } else {
                logger.warn("管理员 {} 更新用户 {} 状态失败: 用户不存在或已删除", adminId, userId);
            }
            
        } catch (SQLException e) {
            logger.error("更新用户状态失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }

        return success;
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "正常";
            case 1: return "已封禁";
            case 2: return "已禁言";
            default: return "未知";
        }
    }

    /**
     * 关闭资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            logger.error("关闭数据库资源失败: {}", e.getMessage(), e);
        }
    }
}
