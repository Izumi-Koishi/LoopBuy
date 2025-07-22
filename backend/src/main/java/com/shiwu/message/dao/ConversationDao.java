package com.shiwu.message.dao;

import com.shiwu.framework.annotation.Repository;
import com.shiwu.message.model.Conversation;
import com.shiwu.common.util.DBUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话数据访问对象 - MVC框架版本
 *
 * 严格遵循项目规范：
 * 1. 使用DBUtil获取连接
 * 2. 不使用物理外键，在应用层管理关系
 * 3. 支持逻辑删除
 * 4. 所有方法都要处理异常
 *
 * 使用MVC框架的@Repository注解
 *
 * @author Shiwu Team
 * @version 2.0 (MVC Framework)
 */
@Repository
public class ConversationDao {

    private static final Logger logger = LoggerFactory.getLogger(ConversationDao.class);
    
    /**
     * 插入新会话
     */
    public Long insertConversation(Conversation conversation) {
        String sql = "INSERT INTO conversation (conversation_id, participant1_id, participant2_id, " +
                    "product_id, last_message, last_message_time, unread_count1, unread_count2, " +
                    "status, create_time, update_time, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime now = LocalDateTime.now();
            
            stmt.setString(1, conversation.getConversationId());
            stmt.setLong(2, conversation.getParticipant1Id());
            stmt.setLong(3, conversation.getParticipant2Id());
            if (conversation.getProductId() != null) {
                stmt.setLong(4, conversation.getProductId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            stmt.setString(5, conversation.getLastMessage());
            if (conversation.getLastMessageTime() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(conversation.getLastMessageTime()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setInt(7, conversation.getUnreadCount1() != null ? conversation.getUnreadCount1() : 0);
            stmt.setInt(8, conversation.getUnreadCount2() != null ? conversation.getUnreadCount2() : 0);
            stmt.setString(9, conversation.getStatus() != null ? conversation.getStatus() : "ACTIVE");
            stmt.setTimestamp(10, Timestamp.valueOf(now));
            stmt.setTimestamp(11, Timestamp.valueOf(now));
            stmt.setBoolean(12, conversation.getDeleted() != null ? conversation.getDeleted() : false);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("插入会话失败，没有行被影响");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("插入会话失败，无法获取生成的ID");
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("插入会话时发生数据库错误", e);
        }
    }
    
    /**
     * 根据会话ID查询会话
     */
    public Conversation findConversationById(String conversationId) {
        String sql = "SELECT id, conversation_id, participant1_id, participant2_id, product_id, " +
                    "last_message, last_message_time, unread_count1, unread_count2, status, " +
                    "create_time, update_time, is_deleted " +
                    "FROM conversation " +
                    "WHERE conversation_id = ? AND is_deleted = 0";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, conversationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConversation(rs);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("查询会话时发生数据库错误", e);
        }
        
        return null;
    }
    
    /**
     * 查询用户的会话列表（分页）
     */
    public List<Conversation> findConversationsByUserId(Long userId, String status, 
                                                       Boolean onlyUnread, Integer offset, Integer limit) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT id, conversation_id, participant1_id, participant2_id, product_id, ");
        sqlBuilder.append("last_message, last_message_time, unread_count1, unread_count2, status, ");
        sqlBuilder.append("create_time, update_time, is_deleted ");
        sqlBuilder.append("FROM conversation ");
        sqlBuilder.append("WHERE (participant1_id = ? OR participant2_id = ?) AND is_deleted = 0 ");
        
        if (status != null && !status.isEmpty()) {
            sqlBuilder.append("AND status = ? ");
        }
        
        if (onlyUnread != null && onlyUnread) {
            sqlBuilder.append("AND ((participant1_id = ? AND unread_count1 > 0) OR ");
            sqlBuilder.append("(participant2_id = ? AND unread_count2 > 0)) ");
        }
        
        sqlBuilder.append("ORDER BY last_message_time DESC ");
        sqlBuilder.append("LIMIT ? OFFSET ?");
        
        List<Conversation> conversations = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            int paramIndex = 1;
            stmt.setLong(paramIndex++, userId);
            stmt.setLong(paramIndex++, userId);
            
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
            
            if (onlyUnread != null && onlyUnread) {
                stmt.setLong(paramIndex++, userId);
                stmt.setLong(paramIndex++, userId);
            }
            
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("查询用户会话列表时发生数据库错误", e);
        }
        
        return conversations;
    }
    
    /**
     * 更新会话的最后消息信息
     */
    public boolean updateLastMessage(String conversationId, String lastMessage, LocalDateTime lastMessageTime) {
        // 参数验证
        if (conversationId == null || lastMessage == null || lastMessageTime == null) {
            return false;
        }

        String sql = "UPDATE conversation SET last_message = ?, last_message_time = ?, update_time = ? " +
                    "WHERE conversation_id = ? AND is_deleted = 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, lastMessage);
            stmt.setTimestamp(2, Timestamp.valueOf(lastMessageTime));
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, conversationId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("更新会话最后消息时发生数据库错误", e);
        }
    }
    
    /**
     * 更新会话的未读消息数量
     */
    public boolean updateUnreadCount(String conversationId, Long userId, Integer unreadCount) {
        // 参数验证
        if (conversationId == null || userId == null || unreadCount == null) {
            return false;
        }

        // 根据用户ID确定更新哪个未读计数字段
        String sql = "UPDATE conversation SET update_time = ?, " +
                    "unread_count1 = CASE WHEN participant1_id = ? THEN ? ELSE unread_count1 END, " +
                    "unread_count2 = CASE WHEN participant2_id = ? THEN ? ELSE unread_count2 END " +
                    "WHERE conversation_id = ? AND is_deleted = 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, userId);
            stmt.setInt(3, unreadCount);
            stmt.setLong(4, userId);
            stmt.setInt(5, unreadCount);
            stmt.setString(6, conversationId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("更新会话未读数量时发生数据库错误", e);
        }
    }
    
    /**
     * 增加会话的未读消息数量
     */
    public boolean incrementUnreadCount(String conversationId, Long receiverId) {
        // 参数验证
        if (conversationId == null || receiverId == null) {
            return false;
        }

        String sql = "UPDATE conversation SET update_time = ?, " +
                    "unread_count1 = CASE WHEN participant1_id = ? THEN unread_count1 + 1 ELSE unread_count1 END, " +
                    "unread_count2 = CASE WHEN participant2_id = ? THEN unread_count2 + 1 ELSE unread_count2 END " +
                    "WHERE conversation_id = ? AND is_deleted = 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, receiverId);
            stmt.setLong(3, receiverId);
            stmt.setString(4, conversationId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("增加会话未读数量时发生数据库错误", e);
        }
    }
    
    /**
     * 逻辑删除会话
     */
    public boolean deleteConversation(String conversationId) {
        String sql = "UPDATE conversation SET is_deleted = 1, update_time = ? WHERE conversation_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, conversationId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("删除会话时发生数据库错误", e);
        }
    }
    
    /**
     * 将ResultSet映射为Conversation对象
     */
    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
        Conversation conversation = new Conversation();
        conversation.setId(rs.getLong("id"));
        conversation.setConversationId(rs.getString("conversation_id"));
        conversation.setParticipant1Id(rs.getLong("participant1_id"));
        conversation.setParticipant2Id(rs.getLong("participant2_id"));
        
        Long productId = rs.getLong("product_id");
        if (!rs.wasNull()) {
            conversation.setProductId(productId);
        }
        
        conversation.setLastMessage(rs.getString("last_message"));
        
        Timestamp lastMessageTime = rs.getTimestamp("last_message_time");
        if (lastMessageTime != null) {
            conversation.setLastMessageTime(lastMessageTime.toLocalDateTime());
        }
        
        conversation.setUnreadCount1(rs.getInt("unread_count1"));
        conversation.setUnreadCount2(rs.getInt("unread_count2"));
        conversation.setStatus(rs.getString("status"));
        conversation.setDeleted(rs.getBoolean("is_deleted"));
        
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            conversation.setCreateTime(createTime.toLocalDateTime());
        }
        
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            conversation.setUpdateTime(updateTime.toLocalDateTime());
        }
        
        return conversation;
    }



    /**
     * 获取用户的总未读消息数量
     */
    public int getTotalUnreadCount(Long userId) {
        String sql = "SELECT " +
                    "SUM(CASE WHEN participant1_id = ? THEN unread_count1 ELSE 0 END) + " +
                    "SUM(CASE WHEN participant2_id = ? THEN unread_count2 ELSE 0 END) AS total_unread " +
                    "FROM conversation " +
                    "WHERE (participant1_id = ? OR participant2_id = ?) AND is_deleted = 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            stmt.setLong(4, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalUnread = rs.getInt("total_unread");
                    logger.debug("查询总未读数量成功: userId={}, count={}", userId, totalUnread);
                    return totalUnread;
                }
            }

        } catch (SQLException e) {
            logger.error("查询总未读数量时发生数据库错误: userId={}", userId, e);
            throw new RuntimeException("查询总未读数量时发生数据库错误", e);
        }

        return 0;
    }

    /**
     * 更新会话状态
     */
    public boolean updateStatus(String conversationId, String status) {
        String sql = "UPDATE conversation SET status = ?, update_time = CURRENT_TIMESTAMP " +
                    "WHERE conversation_id = ? AND is_deleted = 0";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, conversationId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("更新会话状态成功: conversationId={}, status={}", conversationId, status);
                return true;
            } else {
                logger.warn("更新会话状态失败: 会话不存在或已删除, conversationId={}", conversationId);
                return false;
            }

        } catch (SQLException e) {
            logger.error("更新会话状态时发生数据库错误: conversationId={}, status={}",
                        conversationId, status, e);
            throw new RuntimeException("更新会话状态时发生数据库错误", e);
        }
    }
}
