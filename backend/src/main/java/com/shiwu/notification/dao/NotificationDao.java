package com.shiwu.notification.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知数据访问对象 - MVC框架版本
 *
 * 用于Task4_2_1_2: 商品审核通过粉丝通知功能
 * 提供通知的CRUD操作
 *
 * 使用MVC框架的@Repository注解标识数据访问层
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 * @since 2024-01-15
 */
@Repository
public class NotificationDao {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationDao.class);
    
    /**
     * 创建通知
     * 
     * @param notification 通知对象
     * @return 创建成功返回通知ID，失败返回null
     */
    public Long createNotification(Notification notification) {
        if (notification == null || notification.getRecipientId() == null) {
            logger.warn("创建通知失败: 通知对象或接收者ID为空");
            return null;
        }
        
        String sql = "INSERT INTO notification (recipient_id, title, content, notification_type, " +
                    "source_type, source_id, related_user_id, related_user_name, action_url, " +
                    "priority, expire_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            pstmt.setLong(1, notification.getRecipientId());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getContent());
            pstmt.setString(4, notification.getNotificationType());
            pstmt.setString(5, notification.getSourceType());
            
            if (notification.getSourceId() != null) {
                pstmt.setLong(6, notification.getSourceId());
            } else {
                pstmt.setNull(6, Types.BIGINT);
            }
            
            if (notification.getRelatedUserId() != null) {
                pstmt.setLong(7, notification.getRelatedUserId());
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }
            
            pstmt.setString(8, notification.getRelatedUserName());
            pstmt.setString(9, notification.getActionUrl());
            pstmt.setInt(10, notification.getPriority() != null ? notification.getPriority() : Notification.PRIORITY_NORMAL);
            
            if (notification.getExpireTime() != null) {
                pstmt.setTimestamp(11, Timestamp.valueOf(notification.getExpireTime()));
            } else {
                pstmt.setNull(11, Types.TIMESTAMP);
            }
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    Long notificationId = rs.getLong(1);
                    logger.info("创建通知成功: id={}, recipientId={}, type={}", 
                              notificationId, notification.getRecipientId(), notification.getNotificationType());
                    return notificationId;
                }
            }
            
            logger.warn("创建通知失败: 未生成ID");
            return null;
            
        } catch (SQLException e) {
            logger.error("创建通知失败: {}", e.getMessage(), e);
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    /**
     * 批量创建通知
     * 
     * @param notifications 通知列表
     * @return 成功创建的数量
     */
    public int batchCreateNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            logger.warn("批量创建通知失败: 通知列表为空");
            return 0;
        }
        
        String sql = "INSERT INTO notification (recipient_id, title, content, notification_type, " +
                    "source_type, source_id, related_user_id, related_user_name, action_url, " +
                    "priority, expire_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // 开启事务
            
            pstmt = conn.prepareStatement(sql);
            
            int batchCount = 0;
            for (Notification notification : notifications) {
                if (notification.getRecipientId() == null) {
                    logger.warn("跳过无效通知: 接收者ID为空");
                    continue;
                }
                
                pstmt.setLong(1, notification.getRecipientId());
                pstmt.setString(2, notification.getTitle());
                pstmt.setString(3, notification.getContent());
                pstmt.setString(4, notification.getNotificationType());
                pstmt.setString(5, notification.getSourceType());
                
                if (notification.getSourceId() != null) {
                    pstmt.setLong(6, notification.getSourceId());
                } else {
                    pstmt.setNull(6, Types.BIGINT);
                }
                
                if (notification.getRelatedUserId() != null) {
                    pstmt.setLong(7, notification.getRelatedUserId());
                } else {
                    pstmt.setNull(7, Types.BIGINT);
                }
                
                pstmt.setString(8, notification.getRelatedUserName());
                pstmt.setString(9, notification.getActionUrl());
                pstmt.setInt(10, notification.getPriority() != null ? notification.getPriority() : Notification.PRIORITY_NORMAL);
                
                if (notification.getExpireTime() != null) {
                    pstmt.setTimestamp(11, Timestamp.valueOf(notification.getExpireTime()));
                } else {
                    pstmt.setNull(11, Types.TIMESTAMP);
                }
                
                pstmt.addBatch();
                batchCount++;
                
                // 每1000条执行一次批处理
                if (batchCount % 1000 == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }
            
            // 执行剩余的批处理
            if (batchCount % 1000 != 0) {
                pstmt.executeBatch();
            }
            
            conn.commit(); // 提交事务
            
            logger.info("批量创建通知成功: 总数={}", batchCount);
            return batchCount;
            
        } catch (SQLException e) {
            logger.error("批量创建通知失败: {}", e.getMessage(), e);
            try {
                if (conn != null) {
                    conn.rollback(); // 回滚事务
                }
            } catch (SQLException rollbackEx) {
                logger.error("回滚事务失败: {}", rollbackEx.getMessage(), rollbackEx);
            }
            return 0;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // 恢复自动提交
                }
            } catch (SQLException e) {
                logger.error("恢复自动提交失败: {}", e.getMessage(), e);
            }
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 根据用户ID获取通知列表
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param onlyUnread 是否只获取未读通知
     * @return 通知列表
     */
    public List<Notification> findNotificationsByUserId(Long userId, int page, int size, boolean onlyUnread) {
        if (userId == null || page < 1 || size < 1) {
            logger.warn("获取通知列表失败: 参数无效 userId={}, page={}, size={}", userId, page, size);
            return new ArrayList<>();
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, recipient_id, title, content, notification_type, source_type, source_id, ");
        sql.append("related_user_id, related_user_name, action_url, is_read, read_time, priority, ");
        sql.append("expire_time, create_time, update_time ");
        sql.append("FROM notification ");
        sql.append("WHERE recipient_id = ? AND is_deleted = 0 ");
        sql.append("AND (expire_time IS NULL OR expire_time > NOW()) ");
        
        if (onlyUnread) {
            sql.append("AND is_read = 0 ");
        }
        
        sql.append("ORDER BY priority DESC, create_time DESC ");
        sql.append("LIMIT ? OFFSET ?");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());
            
            pstmt.setLong(1, userId);
            pstmt.setInt(2, size);
            pstmt.setInt(3, (page - 1) * size);
            
            rs = pstmt.executeQuery();
            
            List<Notification> notifications = new ArrayList<>();
            while (rs.next()) {
                Notification notification = mapResultSetToNotification(rs);
                notifications.add(notification);
            }
            
            logger.debug("获取通知列表成功: userId={}, count={}, onlyUnread={}", userId, notifications.size(), onlyUnread);
            return notifications;
            
        } catch (SQLException e) {
            logger.error("获取通知列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    /**
     * 获取用户未读通知数量
     * 
     * @param userId 用户ID
     * @return 未读通知数量
     */
    public int getUnreadNotificationCount(Long userId) {
        if (userId == null) {
            logger.warn("获取未读通知数量失败: 用户ID为空");
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM notification " +
                    "WHERE recipient_id = ? AND is_read = 0 AND is_deleted = 0 " +
                    "AND (expire_time IS NULL OR expire_time > NOW())";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.debug("获取未读通知数量成功: userId={}, count={}", userId, count);
                return count;
            }
            
            return 0;
            
        } catch (SQLException e) {
            logger.error("获取未读通知数量失败: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    /**
     * 标记通知为已读
     * 
     * @param notificationId 通知ID
     * @param userId 用户ID（用于权限验证）
     * @return 操作是否成功
     */
    public boolean markNotificationAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            logger.warn("标记通知已读失败: 参数为空");
            return false;
        }
        
        String sql = "UPDATE notification SET is_read = 1, read_time = NOW() " +
                    "WHERE id = ? AND recipient_id = ? AND is_read = 0";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setLong(1, notificationId);
            pstmt.setLong(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                logger.info("标记通知已读成功: notificationId={}, userId={}", notificationId, userId);
            } else {
                logger.warn("标记通知已读失败: 通知不存在或已读 notificationId={}, userId={}", notificationId, userId);
            }
            
            return success;
            
        } catch (SQLException e) {
            logger.error("标记通知已读失败: notificationId={}, userId={}, error={}", 
                        notificationId, userId, e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 批量标记通知为已读
     * 
     * @param userId 用户ID
     * @param notificationIds 通知ID列表（可选，为空则标记所有未读通知）
     * @return 标记成功的数量
     */
    public int batchMarkNotificationsAsRead(Long userId, List<Long> notificationIds) {
        if (userId == null) {
            logger.warn("批量标记通知已读失败: 用户ID为空");
            return 0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE notification SET is_read = 1, read_time = NOW() ");
        sql.append("WHERE recipient_id = ? AND is_read = 0 ");
        
        if (notificationIds != null && !notificationIds.isEmpty()) {
            sql.append("AND id IN (");
            for (int i = 0; i < notificationIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            pstmt.setLong(paramIndex++, userId);
            
            if (notificationIds != null && !notificationIds.isEmpty()) {
                for (Long notificationId : notificationIds) {
                    pstmt.setLong(paramIndex++, notificationId);
                }
            }
            
            int rowsAffected = pstmt.executeUpdate();
            
            logger.info("批量标记通知已读成功: userId={}, count={}", userId, rowsAffected);
            return rowsAffected;
            
        } catch (SQLException e) {
            logger.error("批量标记通知已读失败: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 将ResultSet映射为Notification对象
     */
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        
        notification.setId(rs.getLong("id"));
        notification.setRecipientId(rs.getLong("recipient_id"));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setNotificationType(rs.getString("notification_type"));
        notification.setSourceType(rs.getString("source_type"));
        
        Long sourceId = rs.getLong("source_id");
        if (!rs.wasNull()) {
            notification.setSourceId(sourceId);
        }
        
        Long relatedUserId = rs.getLong("related_user_id");
        if (!rs.wasNull()) {
            notification.setRelatedUserId(relatedUserId);
        }
        
        notification.setRelatedUserName(rs.getString("related_user_name"));
        notification.setActionUrl(rs.getString("action_url"));
        notification.setIsRead(rs.getBoolean("is_read"));
        
        Timestamp readTime = rs.getTimestamp("read_time");
        if (readTime != null) {
            notification.setReadTime(readTime.toLocalDateTime());
        }
        
        notification.setPriority(rs.getInt("priority"));
        
        Timestamp expireTime = rs.getTimestamp("expire_time");
        if (expireTime != null) {
            notification.setExpireTime(expireTime.toLocalDateTime());
        }
        
        notification.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        notification.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        
        return notification;
    }
    
    /**
     * 关闭数据库资源
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
