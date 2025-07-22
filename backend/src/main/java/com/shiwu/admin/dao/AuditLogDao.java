package com.shiwu.admin.dao;

import com.shiwu.admin.dto.AuditLogQueryDTO;
import com.shiwu.admin.model.AuditLog;
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
 * 审计日志数据访问对象 - MVC框架版本
 */
@Repository
public class AuditLogDao {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogDao.class);

    /**
     * 创建审计日志
     * @param auditLog 审计日志对象
     * @return 创建的日志ID，如果失败则返回null
     */
    public Long createAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            logger.warn("创建审计日志失败: 日志对象为空");
            return null;
        }

        if (auditLog.getAdminId() == null) {
            logger.warn("创建审计日志失败: 管理员ID为空");
            return null;
        }

        String sql = "INSERT INTO audit_log (admin_id, action, target_type, target_id, details, " +
                    "ip_address, user_agent, result, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return null;
            }

            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, auditLog.getAdminId());
            pstmt.setString(2, auditLog.getAction());
            pstmt.setString(3, auditLog.getTargetType());
            
            if (auditLog.getTargetId() != null) {
                pstmt.setLong(4, auditLog.getTargetId());
            } else {
                pstmt.setNull(4, Types.BIGINT);
            }
            
            pstmt.setString(5, auditLog.getDetails());
            pstmt.setString(6, auditLog.getIpAddress());
            pstmt.setString(7, auditLog.getUserAgent());
            pstmt.setInt(8, auditLog.getResult());
            pstmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    Long logId = rs.getLong(1);
                    logger.info("成功创建审计日志: ID={}, 管理员ID={}, 操作={}", 
                              logId, auditLog.getAdminId(), auditLog.getAction());
                    return logId;
                }
            }
            
            logger.warn("创建审计日志失败: 未能获取生成的ID");
            return null;
        } catch (SQLException e) {
            logger.error("创建审计日志时发生数据库异常: {}", e.getMessage(), e);
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 记录管理员登录日志
     * @param adminId 管理员ID
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param success 是否成功
     * @param details 详细信息
     * @return 日志ID，如果失败则返回null
     */
    public Long logAdminLogin(Long adminId, String ipAddress, String userAgent, 
                             boolean success, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setAction("ADMIN_LOGIN");
        auditLog.setTargetType("ADMIN");
        auditLog.setTargetId(adminId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setResult(success ? 1 : 0);
        
        return createAuditLog(auditLog);
    }

    // ====================================================================
    // 统计查询方法（用于管理员仪表盘）
    // ====================================================================

    /**
     * 获取指定时间段内的审计日志数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计日志数量
     */
    public Long getAuditLogCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE create_time >= ? AND create_time < ?";
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
            logger.error("查询审计日志数量失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取管理员登录次数统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录次数
     */
    public Long getAdminLoginCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE action LIKE '%登录%' AND result = 1 AND create_time >= ? AND create_time < ?";
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
            logger.error("查询管理员登录次数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取系统错误数量统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误数量
     */
    public Long getSystemErrorCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE result = 0 AND create_time >= ? AND create_time < ?";
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
            logger.error("查询系统错误数量失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取操作统计数据
     * @param days 统计天数
     * @return 统计数据
     */
    public Map<String, Object> getOperationStats(int days) {
        String sql = "SELECT " +
                    "COUNT(*) as totalOperations, " +
                    "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as successOperations, " +
                    "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as failedOperations, " +
                    "COUNT(DISTINCT admin_id) as activeAdmins, " +
                    "COUNT(DISTINCT action) as actionTypes " +
                    "FROM audit_log " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> stats = new HashMap<>();

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return stats;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, days);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("totalOperations", rs.getInt("totalOperations"));
                stats.put("successOperations", rs.getInt("successOperations"));
                stats.put("failedOperations", rs.getInt("failedOperations"));
                stats.put("activeAdmins", rs.getInt("activeAdmins"));
                stats.put("actionTypes", rs.getInt("actionTypes"));

                // 计算成功率
                int total = rs.getInt("totalOperations");
                int success = rs.getInt("successOperations");
                double successRate = total > 0 ? (double) success / total * 100 : 0.0;
                stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            }

            logger.info("获取操作统计数据成功: {}天内共{}次操作", days, stats.get("totalOperations"));
        } catch (SQLException e) {
            logger.error("获取操作统计数据时发生数据库异常: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return stats;
    }

    /**
     * 获取活动趋势数据（按天统计）
     * @param days 统计天数
     * @return 趋势数据列表，每个元素包含日期和当天活动数量
     */
    public List<Map<String, Object>> getActivityTrend(int days) {
        String sql = "SELECT DATE(create_time) as date, COUNT(*) as count " +
                    "FROM audit_log " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
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
            logger.error("查询活动趋势失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return trendData;
    }

    /**
     * 分页查询审计日志
     * @param queryDTO 查询条件
     * @return 审计日志列表
     */
    public List<AuditLog> findAuditLogs(AuditLogQueryDTO queryDTO) {
        if (queryDTO == null) {
            logger.warn("查询审计日志失败: 查询条件为空");
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, admin_id, action, target_type, target_id, details, ");
        sql.append("ip_address, user_agent, result, create_time ");
        sql.append("FROM audit_log WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        // 构建查询条件
        buildQueryConditions(sql, params, queryDTO);

        // 添加排序
        sql.append("ORDER BY ").append(queryDTO.getSortBy()).append(" ").append(queryDTO.getSortOrder()).append(" ");

        // 添加分页
        sql.append("LIMIT ? OFFSET ?");
        params.add(queryDTO.getPageSize());
        params.add(queryDTO.getOffset());

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<AuditLog> auditLogs = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return auditLogs;
            }

            pstmt = conn.prepareStatement(sql.toString());

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                AuditLog auditLog = mapResultSetToAuditLog(rs);
                auditLogs.add(auditLog);
            }

            logger.info("查询审计日志成功: 共{}条记录", auditLogs.size());
            return auditLogs;
        } catch (SQLException e) {
            logger.error("查询审计日志时发生数据库异常: {}", e.getMessage(), e);
            return auditLogs;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 统计审计日志总数
     * @param queryDTO 查询条件
     * @return 总数
     */
    public long countAuditLogs(AuditLogQueryDTO queryDTO) {
        if (queryDTO == null) {
            logger.warn("统计审计日志失败: 查询条件为空");
            return 0;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM audit_log WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        // 构建查询条件（不包括分页和排序）
        buildQueryConditions(sql, params, queryDTO);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("获取数据库连接失败");
                return 0;
            }

            pstmt = conn.prepareStatement(sql.toString());

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();
            if (rs.next()) {
                long count = rs.getLong(1);
                logger.info("统计审计日志成功: 共{}条记录", count);
                return count;
            }

            return 0;
        } catch (SQLException e) {
            logger.error("统计审计日志时发生数据库异常: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 构建查询条件
     * @param sql SQL构建器
     * @param params 参数列表
     * @param queryDTO 查询条件
     */
    private void buildQueryConditions(StringBuilder sql, List<Object> params, AuditLogQueryDTO queryDTO) {
        if (queryDTO.getAdminId() != null) {
            sql.append("AND admin_id = ? ");
            params.add(queryDTO.getAdminId());
        }

        if (queryDTO.getAction() != null && !queryDTO.getAction().trim().isEmpty()) {
            sql.append("AND action = ? ");
            params.add(queryDTO.getAction().trim());
        }

        if (queryDTO.getTargetType() != null && !queryDTO.getTargetType().trim().isEmpty()) {
            sql.append("AND target_type = ? ");
            params.add(queryDTO.getTargetType().trim());
        }

        if (queryDTO.getTargetId() != null) {
            sql.append("AND target_id = ? ");
            params.add(queryDTO.getTargetId());
        }

        if (queryDTO.getIpAddress() != null && !queryDTO.getIpAddress().trim().isEmpty()) {
            sql.append("AND ip_address = ? ");
            params.add(queryDTO.getIpAddress().trim());
        }

        if (queryDTO.getResult() != null) {
            sql.append("AND result = ? ");
            params.add(queryDTO.getResult());
        }

        if (queryDTO.getStartTime() != null) {
            sql.append("AND create_time >= ? ");
            params.add(Timestamp.valueOf(queryDTO.getStartTime()));
        }

        if (queryDTO.getEndTime() != null) {
            sql.append("AND create_time <= ? ");
            params.add(Timestamp.valueOf(queryDTO.getEndTime()));
        }

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            sql.append("AND details LIKE ? ");
            params.add("%" + queryDTO.getKeyword().trim() + "%");
        }
    }

    /**
     * 将ResultSet映射为AuditLog对象
     * @param rs ResultSet
     * @return AuditLog对象
     * @throws SQLException SQL异常
     */
    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(rs.getLong("id"));
        auditLog.setAdminId(rs.getLong("admin_id"));
        auditLog.setAction(rs.getString("action"));
        auditLog.setTargetType(rs.getString("target_type"));

        Long targetId = rs.getLong("target_id");
        if (!rs.wasNull()) {
            auditLog.setTargetId(targetId);
        }

        auditLog.setDetails(rs.getString("details"));
        auditLog.setIpAddress(rs.getString("ip_address"));
        auditLog.setUserAgent(rs.getString("user_agent"));
        auditLog.setResult(rs.getInt("result"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            auditLog.setCreateTime(createTime.toLocalDateTime());
        }

        return auditLog;
    }

    /**
     * 根据ID查找审计日志
     * @param id 日志ID
     * @return 审计日志，如果不存在则返回null
     */
    public AuditLog findById(Long id) {
        if (id == null) {
            logger.warn("查找审计日志失败: ID为空");
            return null;
        }

        String sql = "SELECT id, admin_id, action, target_type, target_id, details, " +
                    "ip_address, user_agent, result, create_time " +
                    "FROM audit_log WHERE id = ?";

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
                AuditLog auditLog = mapResultSetToAuditLog(rs);
                logger.info("查找审计日志成功: ID={}", id);
                return auditLog;
            }

            logger.warn("审计日志不存在: ID={}", id);
            return null;
        } catch (SQLException e) {
            logger.error("查找审计日志时发生数据库异常: {}", e.getMessage(), e);
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
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
