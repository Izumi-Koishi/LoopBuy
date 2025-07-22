package com.shiwu.cart.dao;

import com.shiwu.cart.model.CartItem;
import com.shiwu.cart.model.CartItemVO;
import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.product.model.ProductCardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.math.BigDecimal;
import java.sql.*;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车数据访问对象 - MVC框架版本
 */
@Repository
public class CartDao {
    private static final Logger logger = LoggerFactory.getLogger(CartDao.class);

    /**
     * 添加商品到购物车
     * @param cartItem 购物车项
     * @return 是否添加成功
     */
    public boolean addToCart(CartItem cartItem) {
        // 参数验证
        if (cartItem == null) {
            logger.warn("添加商品到购物车失败: cartItem为null");
            return false;
        }
        if (cartItem.getUserId() == null) {
            logger.warn("添加商品到购物车失败: userId为null");
            return false;
        }
        if (cartItem.getProductId() == null) {
            logger.warn("添加商品到购物车失败: productId为null");
            return false;
        }
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            logger.warn("添加商品到购物车失败: quantity无效: {}", cartItem.getQuantity());
            return false;
        }

        String sql = "INSERT INTO shopping_cart (user_id, product_id, quantity, is_deleted, create_time, update_time) VALUES (?, ?, ?, 0, NOW(), NOW())";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, cartItem.getUserId());
            pstmt.setLong(2, cartItem.getProductId());
            pstmt.setInt(3, cartItem.getQuantity());

            int result = pstmt.executeUpdate();
            logger.info("添加商品到购物车成功: userId={}, productId={}, quantity={}",
                       cartItem.getUserId(), cartItem.getProductId(), cartItem.getQuantity());
            return result > 0;
        } catch (SQLException e) {
            logger.error("添加商品到购物车失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 检查商品是否已在购物车中
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否已存在
     */
    public boolean existsInCart(Long userId, Long productId) {
        // 参数验证
        if (userId == null) {
            logger.warn("检查商品是否在购物车中失败: userId为null");
            return false;
        }
        if (productId == null) {
            logger.warn("检查商品是否在购物车中失败: productId为null");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ? AND product_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            pstmt.setLong(2, productId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("检查商品是否在购物车中失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return false;
    }

    /**
     * 获取用户购物车列表
     * @param userId 用户ID
     * @return 购物车项列表
     */
    public List<CartItemVO> findCartItemsByUserId(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("获取用户购物车列表失败: userId为null");
            return new ArrayList<>();
        }

        String sql = "SELECT sc.id, sc.product_id, sc.quantity, " +
                    "p.title, p.price, p.status, p.seller_id, " +
                    "u.nickname as seller_name, " +
                    "pi.image_url as main_image_url " +
                    "FROM shopping_cart sc " +
                    "LEFT JOIN product p ON sc.product_id = p.id " +
                    "LEFT JOIN system_user u ON p.seller_id = u.id " +
                    "LEFT JOIN product_image pi ON p.id = pi.product_id AND pi.is_main = 1 " +
                    "WHERE sc.user_id = ? AND sc.is_deleted = 0 " +
                    "ORDER BY sc.create_time DESC";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<CartItemVO> cartItems = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                CartItemVO cartItem = new CartItemVO();
                cartItem.setId(rs.getLong("id"));
                cartItem.setQuantity(rs.getInt("quantity"));
                cartItem.setSellerId(rs.getLong("seller_id"));
                cartItem.setSellerName(rs.getString("seller_name"));

                // 创建商品信息
                ProductCardVO product = new ProductCardVO();
                product.setId(rs.getLong("product_id"));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setMainImageUrl(rs.getString("main_image_url"));
                cartItem.setProduct(product);

                // 检查商品是否可用（状态为在售）
                Integer productStatus = rs.getInt("status");
                cartItem.setAvailable(productStatus != null && productStatus == 1); // 1表示在售

                cartItems.add(cartItem);
            }
        } catch (SQLException e) {
            logger.error("获取用户购物车列表失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return cartItems;
    }

    /**
     * 从购物车中移除商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否移除成功
     */
    public boolean removeFromCart(Long userId, Long productId) {
        // 参数验证
        if (userId == null) {
            logger.warn("从购物车移除商品失败: userId为null");
            return false;
        }
        if (productId == null) {
            logger.warn("从购物车移除商品失败: productId为null");
            return false;
        }

        String sql = "UPDATE shopping_cart SET is_deleted = 1, update_time = NOW() WHERE user_id = ? AND product_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            pstmt.setLong(2, productId);

            int result = pstmt.executeUpdate();
            logger.info("从购物车移除商品成功: userId={}, productId={}", userId, productId);
            return result > 0;
        } catch (SQLException e) {
            logger.error("从购物车移除商品失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 批量从购物车中移除商品
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @return 是否移除成功
     */
    public boolean batchRemoveFromCart(Long userId, List<Long> productIds) {
        // 参数验证
        if (userId == null) {
            logger.warn("批量从购物车移除商品失败: userId为null");
            return false;
        }
        if (productIds == null || productIds.isEmpty()) {
            logger.debug("批量从购物车移除商品: productIds为空，直接返回成功");
            return true;
        }

        StringBuilder sql = new StringBuilder("UPDATE shopping_cart SET is_deleted = 1, update_time = NOW() WHERE user_id = ? AND is_deleted = 0 AND product_id IN (");
        for (int i = 0; i < productIds.size(); i++) {
            sql.append("?");
            if (i < productIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(")");

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setLong(1, userId);
            
            for (int i = 0; i < productIds.size(); i++) {
                pstmt.setLong(i + 2, productIds.get(i));
            }

            int result = pstmt.executeUpdate();
            logger.info("批量从购物车移除商品成功: userId={}, productIds={}, affectedRows={}", userId, productIds, result);
            return result > 0;
        } catch (SQLException e) {
            logger.error("批量从购物车移除商品失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 清空用户购物车
     * @param userId 用户ID
     * @return 是否清空成功
     */
    public boolean clearCart(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("清空用户购物车失败: userId为null");
            return false;
        }

        String sql = "UPDATE shopping_cart SET is_deleted = 1, update_time = NOW() WHERE user_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);

            int result = pstmt.executeUpdate();
            logger.info("清空用户购物车成功: userId={}, affectedRows={}", userId, result);
            return true; // 即使没有商品也算成功
        } catch (SQLException e) {
            logger.error("清空用户购物车失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 获取用户购物车中的商品总数
     * @param userId 用户ID
     * @return 商品总数
     */
    public int getCartItemCount(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("获取购物车商品总数失败: userId为null");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("获取购物车商品总数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return 0;
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
