package com.shiwu.user.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.user.vo.FeedItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 关注动态信息流数据访问对象 - MVC框架版本
 *
 * 用于Task4_2_1_3: 获取关注动态信息流API
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 * @since 2024-01-15
 */
@Repository
public class FeedDao {
    
    private static final Logger logger = LoggerFactory.getLogger(FeedDao.class);
    
    /**
     * 获取用户关注的卖家的商品动态
     * 
     * @param userId 用户ID
     * @param type 动态类型过滤（ALL, PRODUCT_APPROVED, PRODUCT_PUBLISHED）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 动态列表
     */
    public List<FeedItemVO> getFollowingFeed(Long userId, String type, int offset, int limit) {
        List<FeedItemVO> feeds = new ArrayList<>();

        if (userId == null) {
            logger.warn("获取关注动态失败: 用户ID为空");
            return feeds;
        }

        // 参数验证
        if (userId <= 0) {
            logger.warn("获取关注动态失败: 用户ID无效: {}", userId);
            return feeds;
        }
        if (offset < 0) {
            logger.warn("获取关注动态失败: offset无效: {}", offset);
            offset = 0;
        }
        if (limit <= 0) {
            logger.warn("获取关注动态失败: limit无效: {}", limit);
            limit = 10;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT n.id, n.title, n.content, n.notification_type as type, ");
        sql.append("n.source_id as product_id, n.related_user_id as seller_id, ");
        sql.append("n.related_user_name as seller_name, n.action_url, n.create_time, ");
        sql.append("p.title as product_title, p.price as product_price ");
        sql.append("FROM notification n ");
        sql.append("INNER JOIN user_follow uf ON uf.followed_id = n.related_user_id ");
        sql.append("LEFT JOIN product p ON p.id = n.source_id ");
        sql.append("WHERE uf.follower_id = ? AND uf.is_deleted = 0 ");
        sql.append("AND n.notification_type IN ('PRODUCT_APPROVED', 'PRODUCT_PUBLISHED') ");
        sql.append("AND n.is_deleted = 0 ");
        
        // 添加类型过滤
        if (type != null && !"ALL".equals(type)) {
            sql.append("AND n.notification_type = ? ");
        }
        
        sql.append("ORDER BY n.create_time DESC ");
        sql.append("LIMIT ? OFFSET ?");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            pstmt.setLong(paramIndex++, userId);
            
            if (type != null && !"ALL".equals(type)) {
                pstmt.setString(paramIndex++, type);
            }
            
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex, offset);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FeedItemVO feed = mapResultSetToFeedItem(rs);
                feeds.add(feed);
            }
            
            logger.info("获取关注动态成功: userId={}, type={}, count={}", userId, type, feeds.size());
            
        } catch (SQLException e) {
            logger.error("获取关注动态失败: userId={}, type={}, error={}", userId, type, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return feeds;
    }
    
    /**
     * 获取用户关注动态的总数量
     * 
     * @param userId 用户ID
     * @param type 动态类型过滤
     * @return 总数量
     */
    public long getFollowingFeedCount(Long userId, String type) {
        if (userId == null) {
            logger.warn("获取关注动态数量失败: 用户ID为空");
            return 0;
        }

        // 参数验证
        if (userId <= 0) {
            logger.warn("获取关注动态数量失败: 用户ID无效: {}", userId);
            return 0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM notification n ");
        sql.append("INNER JOIN user_follow uf ON uf.followed_id = n.related_user_id ");
        sql.append("WHERE uf.follower_id = ? AND uf.is_deleted = 0 ");
        sql.append("AND n.notification_type IN ('PRODUCT_APPROVED', 'PRODUCT_PUBLISHED') ");
        sql.append("AND n.is_deleted = 0 ");
        
        if (type != null && !"ALL".equals(type)) {
            sql.append("AND n.notification_type = ? ");
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            pstmt.setLong(paramIndex++, userId);
            
            if (type != null && !"ALL".equals(type)) {
                pstmt.setString(paramIndex, type);
            }
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                long count = rs.getLong(1);
                logger.debug("获取关注动态数量成功: userId={}, type={}, count={}", userId, type, count);
                return count;
            }
            
        } catch (SQLException e) {
            logger.error("获取关注动态数量失败: userId={}, type={}, error={}", userId, type, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return 0;
    }
    
    /**
     * 将ResultSet映射为FeedItemVO对象
     */
    private FeedItemVO mapResultSetToFeedItem(ResultSet rs) throws SQLException {
        FeedItemVO feed = new FeedItemVO();
        
        feed.setId(rs.getLong("id"));
        feed.setType(rs.getString("type"));
        feed.setTitle(rs.getString("title"));
        feed.setContent(rs.getString("content"));
        feed.setSellerId(rs.getLong("seller_id"));
        feed.setSellerName(rs.getString("seller_name"));
        feed.setProductId(rs.getLong("product_id"));
        feed.setProductTitle(rs.getString("product_title"));
        feed.setProductPrice(rs.getBigDecimal("product_price"));
        feed.setActionUrl(rs.getString("action_url"));
        feed.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
        
        // 设置默认的卖家头像和商品图片
        feed.setSellerAvatar("/uploads/avatars/" + feed.getSellerId() + ".jpg");
        feed.setProductImage("/uploads/products/" + feed.getProductId() + "_1.jpg");
        
        return feed;
    }
    
    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("关闭ResultSet失败: {}", e.getMessage());
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                logger.warn("关闭PreparedStatement失败: {}", e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("关闭Connection失败: {}", e.getMessage());
            }
        }
    }
}
