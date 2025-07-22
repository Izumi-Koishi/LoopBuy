package com.shiwu.order.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.order.model.Order;
import com.shiwu.order.model.OrderVO;
import com.shiwu.user.model.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单数据访问对象 - MVC框架版本
 */
@Repository
public class OrderDao {
    private static final Logger logger = LoggerFactory.getLogger(OrderDao.class);

    /**
     * 创建订单
     * @param order 订单对象
     * @return 创建的订单ID，失败返回null
     */
    public Long createOrder(Order order) {
        // 参数验证
        if (order == null) {
            logger.warn("创建订单失败: 订单对象为空");
            return null;
        }

        // 详细字段验证
        if (order.getBuyerId() == null || order.getSellerId() == null ||
            order.getProductId() == null || order.getPriceAtPurchase() == null ||
            order.getStatus() == null) {
            logger.warn("创建订单失败: 必要字段为空 buyerId={}, sellerId={}, productId={}, price={}, status={}",
                       order.getBuyerId(), order.getSellerId(), order.getProductId(),
                       order.getPriceAtPurchase(), order.getStatus());
            return null;
        }

        String sql = "INSERT INTO trade_order (buyer_id, seller_id, product_id, price_at_purchase, " +
                    "product_title_snapshot, product_description_snapshot, product_image_urls_snapshot, " +
                    "status, is_deleted, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long orderId = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, order.getBuyerId());
            pstmt.setLong(2, order.getSellerId());
            pstmt.setLong(3, order.getProductId());
            pstmt.setBigDecimal(4, order.getPriceAtPurchase());
            pstmt.setString(5, order.getProductTitleSnapshot());
            pstmt.setString(6, order.getProductDescriptionSnapshot());
            pstmt.setString(7, order.getProductImageUrlsSnapshot());
            pstmt.setInt(8, order.getStatus());

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    orderId = rs.getLong(1);
                    logger.info("创建订单成功: orderId={}, buyerId={}, sellerId={}, productId={}", 
                               orderId, order.getBuyerId(), order.getSellerId(), order.getProductId());
                }
            }
        } catch (SQLException e) {
            logger.error("创建订单失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return orderId;
    }

    /**
     * 根据订单ID查询订单
     * @param orderId 订单ID
     * @return 订单对象，不存在返回null
     */
    public Order findById(Long orderId) {
        // 参数验证
        if (orderId == null) {
            logger.warn("查询订单失败: 订单ID为空");
            return null;
        }
        if (orderId <= 0) {
            logger.warn("查询订单失败: 订单ID无效: {}", orderId);
            return null;
        }

        String sql = "SELECT id, buyer_id, seller_id, product_id, price_at_purchase, " +
                    "product_title_snapshot, product_description_snapshot, product_image_urls_snapshot, " +
                    "status, is_deleted, create_time, update_time " +
                    "FROM trade_order WHERE id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Order order = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, orderId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                order = new Order();
                order.setId(rs.getLong("id"));
                order.setBuyerId(rs.getLong("buyer_id"));
                order.setSellerId(rs.getLong("seller_id"));
                order.setProductId(rs.getLong("product_id"));
                order.setPriceAtPurchase(rs.getBigDecimal("price_at_purchase"));
                order.setProductTitleSnapshot(rs.getString("product_title_snapshot"));
                order.setProductDescriptionSnapshot(rs.getString("product_description_snapshot"));
                order.setProductImageUrlsSnapshot(rs.getString("product_image_urls_snapshot"));
                order.setStatus(rs.getInt("status"));
                order.setDeleted(rs.getBoolean("is_deleted"));
                order.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                order.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            }
        } catch (SQLException e) {
            logger.error("查询订单失败: orderId={}, error={}", orderId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return order;
    }

    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param status 新状态
     * @return 是否更新成功
     */
    public boolean updateOrderStatus(Long orderId, Integer status) {
        // 参数验证
        if (orderId == null) {
            logger.warn("更新订单状态失败: 订单ID为空");
            return false;
        }
        if (status == null) {
            logger.warn("更新订单状态失败: 状态为空");
            return false;
        }
        if (orderId <= 0) {
            logger.warn("更新订单状态失败: 订单ID无效: {}", orderId);
            return false;
        }

        String sql = "UPDATE trade_order SET status = ?, update_time = NOW() WHERE id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setLong(2, orderId);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                logger.info("更新订单状态成功: orderId={}, status={}", orderId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("更新订单状态失败: orderId={}, status={}, error={}", orderId, status, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }
        
        return false;
    }

    /**
     * 查询用户的订单列表（作为买家）
     * @param buyerId 买家ID
     * @return 订单列表
     */
    public List<OrderVO> findOrdersByBuyerId(Long buyerId) {
        String sql = "SELECT o.id, o.buyer_id, o.seller_id, o.product_id, o.price_at_purchase, " +
                    "o.product_title_snapshot, o.product_description_snapshot, o.product_image_urls_snapshot, " +
                    "o.status, o.create_time, o.update_time, " +
                    "buyer.username as buyer_username, buyer.nickname as buyer_nickname, buyer.avatar_url as buyer_avatar, " +
                    "seller.username as seller_username, seller.nickname as seller_nickname, seller.avatar_url as seller_avatar " +
                    "FROM trade_order o " +
                    "LEFT JOIN system_user buyer ON o.buyer_id = buyer.id " +
                    "LEFT JOIN system_user seller ON o.seller_id = seller.id " +
                    "WHERE o.buyer_id = ? AND o.is_deleted = 0 " +
                    "ORDER BY o.create_time DESC";
        
        return executeOrderQuery(sql, buyerId);
    }

    /**
     * 查询用户的订单列表（作为卖家）
     * @param sellerId 卖家ID
     * @return 订单列表
     */
    public List<OrderVO> findOrdersBySellerId(Long sellerId) {
        String sql = "SELECT o.id, o.buyer_id, o.seller_id, o.product_id, o.price_at_purchase, " +
                    "o.product_title_snapshot, o.product_description_snapshot, o.product_image_urls_snapshot, " +
                    "o.status, o.create_time, o.update_time, " +
                    "buyer.username as buyer_username, buyer.nickname as buyer_nickname, buyer.avatar_url as buyer_avatar, " +
                    "seller.username as seller_username, seller.nickname as seller_nickname, seller.avatar_url as seller_avatar " +
                    "FROM trade_order o " +
                    "LEFT JOIN system_user buyer ON o.buyer_id = buyer.id " +
                    "LEFT JOIN system_user seller ON o.seller_id = seller.id " +
                    "WHERE o.seller_id = ? AND o.is_deleted = 0 " +
                    "ORDER BY o.create_time DESC";
        
        return executeOrderQuery(sql, sellerId);
    }

    /**
     * 执行订单查询的通用方法
     * @param sql SQL语句
     * @param userId 用户ID参数
     * @return 订单VO列表
     */
    private List<OrderVO> executeOrderQuery(String sql, Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("查询订单失败: 用户ID为空");
            return new ArrayList<>();
        }
        if (userId <= 0) {
            logger.warn("查询订单失败: 用户ID无效: {}", userId);
            return new ArrayList<>();
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<OrderVO> orders = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                OrderVO orderVO = new OrderVO();
                orderVO.setId(rs.getLong("id"));
                orderVO.setProductId(rs.getLong("product_id"));
                orderVO.setPriceAtPurchase(rs.getBigDecimal("price_at_purchase"));
                orderVO.setProductTitleSnapshot(rs.getString("product_title_snapshot"));
                orderVO.setProductDescriptionSnapshot(rs.getString("product_description_snapshot"));
                orderVO.setStatus(rs.getInt("status"));
                orderVO.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                orderVO.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());

                // 解析商品图片URL快照
                String imageUrlsJson = rs.getString("product_image_urls_snapshot");
                if (imageUrlsJson != null && !imageUrlsJson.trim().isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> imageUrls = JsonUtil.fromJson(imageUrlsJson, List.class);
                        orderVO.setProductImageUrlsSnapshot(imageUrls);
                    } catch (Exception e) {
                        logger.warn("解析商品图片快照失败: orderId={}, error={}", orderVO.getId(), e.getMessage());
                        orderVO.setProductImageUrlsSnapshot(new ArrayList<>());
                    }
                } else {
                    orderVO.setProductImageUrlsSnapshot(new ArrayList<>());
                }

                // 设置买家信息
                UserVO buyer = new UserVO();
                buyer.setId(rs.getLong("buyer_id"));
                buyer.setUsername(rs.getString("buyer_username"));
                buyer.setNickname(rs.getString("buyer_nickname"));
                buyer.setAvatarUrl(rs.getString("buyer_avatar"));
                orderVO.setBuyer(buyer);

                // 设置卖家信息
                UserVO seller = new UserVO();
                seller.setId(rs.getLong("seller_id"));
                seller.setUsername(rs.getString("seller_username"));
                seller.setNickname(rs.getString("seller_nickname"));
                seller.setAvatarUrl(rs.getString("seller_avatar"));
                orderVO.setSeller(seller);

                orders.add(orderVO);
            }
        } catch (SQLException e) {
            logger.error("查询订单列表失败: userId={}, error={}", userId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return orders;
    }

    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("关闭ResultSet失败: {}", e.getMessage());
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                logger.error("关闭PreparedStatement失败: {}", e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭Connection失败: {}", e.getMessage());
            }
        }
    }
}
