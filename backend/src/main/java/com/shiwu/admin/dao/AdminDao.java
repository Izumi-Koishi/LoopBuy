package com.shiwu.admin.dao;

import com.shiwu.admin.model.Administrator;
import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 管理员数据访问对象 - MVC框架版本
 */
@Repository
public class AdminDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminDao.class);

    /**
     * 根据用户名查找管理员
     * @param username 用户名
     * @return 管理员对象，如果不存在则返回null
     */
    public Administrator findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("查询管理员失败: 用户名为空");
            return null;
        }

        String sql = "SELECT id, username, password, email, real_name, role, status, " +
                    "last_login_time, login_count, is_deleted, create_time, update_time " +
                    "FROM administrator WHERE username = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return null;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Administrator admin = new Administrator();
                admin.setId(rs.getLong("id"));
                admin.setUsername(rs.getString("username"));
                admin.setPassword(rs.getString("password"));
                admin.setEmail(rs.getString("email"));
                admin.setRealName(rs.getString("real_name"));
                admin.setRole(rs.getString("role"));
                admin.setStatus(rs.getInt("status"));
                
                Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
                if (lastLoginTime != null) {
                    admin.setLastLoginTime(lastLoginTime.toLocalDateTime());
                }
                
                admin.setLoginCount(rs.getInt("login_count"));
                admin.setDeleted(rs.getBoolean("is_deleted"));
                
                Timestamp createTime = rs.getTimestamp("create_time");
                if (createTime != null) {
                    admin.setCreateTime(createTime.toLocalDateTime());
                }
                
                Timestamp updateTime = rs.getTimestamp("update_time");
                if (updateTime != null) {
                    admin.setUpdateTime(updateTime.toLocalDateTime());
                }

                logger.info("成功查询到管理员: {}", username);
                return admin;
            } else {
                logger.warn("管理员不存在: {}", username);
                return null;
            }
        } catch (SQLException e) {
            logger.error("查询管理员时发生数据库异常: {}", e.getMessage(), e);
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 根据ID查找管理员
     * @param id 管理员ID
     * @return 管理员对象，如果不存在则返回null
     */
    public Administrator findById(Long id) {
        if (id == null) {
            logger.warn("查询管理员失败: ID为空");
            return null;
        }

        String sql = "SELECT id, username, password, email, real_name, role, status, " +
                    "last_login_time, login_count, is_deleted, create_time, update_time " +
                    "FROM administrator WHERE id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return null;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Administrator admin = new Administrator();
                admin.setId(rs.getLong("id"));
                admin.setUsername(rs.getString("username"));
                admin.setPassword(rs.getString("password"));
                admin.setEmail(rs.getString("email"));
                admin.setRealName(rs.getString("real_name"));
                admin.setRole(rs.getString("role"));
                admin.setStatus(rs.getInt("status"));
                
                Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
                if (lastLoginTime != null) {
                    admin.setLastLoginTime(lastLoginTime.toLocalDateTime());
                }
                
                admin.setLoginCount(rs.getInt("login_count"));
                admin.setDeleted(rs.getBoolean("is_deleted"));
                
                Timestamp createTime = rs.getTimestamp("create_time");
                if (createTime != null) {
                    admin.setCreateTime(createTime.toLocalDateTime());
                }
                
                Timestamp updateTime = rs.getTimestamp("update_time");
                if (updateTime != null) {
                    admin.setUpdateTime(updateTime.toLocalDateTime());
                }

                logger.info("成功查询到管理员: ID={}", id);
                return admin;
            } else {
                logger.warn("管理员不存在: ID={}", id);
                return null;
            }
        } catch (SQLException e) {
            logger.error("查询管理员时发生数据库异常: {}", e.getMessage(), e);
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 更新管理员最后登录时间和登录次数
     * @param adminId 管理员ID
     * @return 更新是否成功
     */
    public boolean updateLastLoginInfo(Long adminId) {
        if (adminId == null) {
            logger.warn("更新管理员登录信息失败: 管理员ID为空");
            return false;
        }

        String sql = "UPDATE administrator SET last_login_time = ?, login_count = login_count + 1, " +
                    "update_time = ? WHERE id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return false;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(3, adminId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("成功更新管理员登录信息: ID={}", adminId);
                return true;
            } else {
                logger.warn("更新管理员登录信息失败: 管理员不存在或已删除, ID={}", adminId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("更新管理员登录信息时发生数据库异常: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
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
