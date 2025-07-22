package com.shiwu.review.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.review.model.Review;
import com.shiwu.review.model.ReviewVO;
import com.shiwu.user.model.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评价数据访问对象 - MVC框架版本
 *
 * 严格遵循项目规范：
 * 1. 使用DBUtil获取连接
 * 2. 不使用物理外键，在应用层管理关系
 * 3. 支持逻辑删除
 * 4. 所有方法都要处理异常
 *
 * @author Shiwu Team
 * @version 1.0
 */
@Repository
public class ReviewDao {

    private static final Logger logger = LoggerFactory.getLogger(ReviewDao.class);

    /**
     * 创建评价
     * @param review 评价对象
     * @return 创建的评价ID，失败返回null
     */
    public Long createReview(Review review) {
        // 参数验证
        if (review == null) {
            logger.warn("创建评价失败: 评价对象为空");
            return null;
        }

        // 详细字段验证
        if (review.getOrderId() == null || review.getProductId() == null ||
            review.getUserId() == null || review.getRating() == null) {
            logger.warn("创建评价失败: 必要字段为空 orderId={}, productId={}, userId={}, rating={}",
                       review.getOrderId(), review.getProductId(), review.getUserId(), review.getRating());
            return null;
        }

        String sql = "INSERT INTO review (order_id, product_id, user_id, rating, comment, " +
                    "is_deleted, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long reviewId = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, review.getOrderId());
            pstmt.setLong(2, review.getProductId());
            pstmt.setLong(3, review.getUserId());
            pstmt.setInt(4, review.getRating());
            pstmt.setString(5, review.getComment());

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    reviewId = rs.getLong(1);
                    logger.info("创建评价成功: reviewId={}, orderId={}, userId={}, rating={}", 
                               reviewId, review.getOrderId(), review.getUserId(), review.getRating());
                }
            }
        } catch (SQLException e) {
            logger.error("创建评价失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return reviewId;
    }

    /**
     * 根据订单ID检查是否已评价
     * @param orderId 订单ID
     * @return 是否已评价
     */
    public boolean isOrderReviewed(Long orderId) {
        // 参数验证
        if (orderId == null) {
            logger.warn("检查订单评价状态失败: 订单ID为空");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM review WHERE order_id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, orderId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            logger.error("检查订单是否已评价失败: orderId={}, error={}", orderId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return false;
    }

    /**
     * 根据评价ID查询评价
     * @param reviewId 评价ID
     * @return 评价对象，不存在返回null
     */
    public Review findById(Long reviewId) {
        // 参数验证
        if (reviewId == null) {
            logger.warn("查询评价失败: 评价ID为空");
            return null;
        }

        String sql = "SELECT id, order_id, product_id, user_id, rating, comment, " +
                    "is_deleted, create_time, update_time " +
                    "FROM review WHERE id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, reviewId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToReview(rs);
            }
        } catch (SQLException e) {
            logger.error("根据ID查询评价失败: reviewId={}, error={}", reviewId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return null;
    }

    /**
     * 根据商品ID查询评价列表
     * @param productId 商品ID
     * @return 评价列表
     */
    public List<ReviewVO> findReviewsByProductId(Long productId) {
        // 参数验证
        if (productId == null) {
            logger.warn("查询商品评价失败: 商品ID为空");
            return new ArrayList<>();
        }

        String sql = "SELECT r.id, r.order_id, r.product_id, r.user_id, r.rating, r.comment, r.create_time, " +
                    "u.username, u.nickname, u.avatar_url " +
                    "FROM review r " +
                    "LEFT JOIN system_user u ON r.user_id = u.id " +
                    "WHERE r.product_id = ? AND r.is_deleted = 0 " +
                    "ORDER BY r.create_time DESC";
        
        return executeReviewVOQuery(sql, productId);
    }

    /**
     * 根据用户ID查询评价列表
     * @param userId 用户ID
     * @return 评价列表
     */
    public List<ReviewVO> findReviewsByUserId(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("查询用户评价失败: 用户ID为空");
            return new ArrayList<>();
        }

        String sql = "SELECT r.id, r.order_id, r.product_id, r.user_id, r.rating, r.comment, r.create_time, " +
                    "u.username, u.nickname, u.avatar_url " +
                    "FROM review r " +
                    "LEFT JOIN system_user u ON r.user_id = u.id " +
                    "WHERE r.user_id = ? AND r.is_deleted = 0 " +
                    "ORDER BY r.create_time DESC";
        
        return executeReviewVOQuery(sql, userId);
    }

    /**
     * 执行ReviewVO查询的通用方法
     */
    private List<ReviewVO> executeReviewVOQuery(String sql, Long parameter) {
        List<ReviewVO> reviews = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, parameter);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                ReviewVO reviewVO = mapResultSetToReviewVO(rs);
                reviews.add(reviewVO);
            }
        } catch (SQLException e) {
            logger.error("查询评价列表失败: parameter={}, error={}", parameter, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return reviews;
    }

    /**
     * 将ResultSet映射为Review对象
     */
    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setOrderId(rs.getLong("order_id"));
        review.setProductId(rs.getLong("product_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setDeleted(rs.getBoolean("is_deleted"));
        
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            review.setCreateTime(createTime.toLocalDateTime());
        }
        
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            review.setUpdateTime(updateTime.toLocalDateTime());
        }
        
        return review;
    }

    /**
     * 将ResultSet映射为ReviewVO对象
     */
    private ReviewVO mapResultSetToReviewVO(ResultSet rs) throws SQLException {
        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setId(rs.getLong("id"));
        reviewVO.setOrderId(rs.getLong("order_id"));
        reviewVO.setProductId(rs.getLong("product_id"));
        reviewVO.setRating(rs.getInt("rating"));
        reviewVO.setComment(rs.getString("comment"));
        
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            reviewVO.setCreateTime(createTime.toLocalDateTime());
        }

        // 设置用户信息
        UserVO user = new UserVO();
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setNickname(rs.getString("nickname"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        reviewVO.setUser(user);

        return reviewVO;
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
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭Connection失败: {}", e.getMessage(), e);
            }
        }
    }
}
