package com.shiwu.product.dao;

import com.shiwu.admin.model.AdminProductQueryDTO;
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
 * 管理员商品数据访问对象 - MVC框架版本
 */
@Repository
public class AdminProductDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminProductDao.class);

    /**
     * 查询商品列表（管理员视角）
     */
    public List<Map<String, Object>> findProducts(AdminProductQueryDTO queryDTO) {
        // 参数验证
        if (queryDTO == null) {
            logger.warn("查询商品列表失败: 查询条件为空");
            throw new IllegalArgumentException("查询条件不能为空");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.title, p.price, p.status, p.create_time, p.update_time, ");
        sql.append("p.seller_id, u.username as seller_name, p.category_id ");
        sql.append("FROM product p ");
        sql.append("LEFT JOIN system_user u ON p.seller_id = u.id ");
        sql.append("WHERE p.is_deleted = 0 ");

        List<Object> params = new ArrayList<>();

        // 添加查询条件
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            sql.append("AND p.title LIKE ? ");
            params.add("%" + queryDTO.getKeyword().trim() + "%");
        }

        if (queryDTO.getStatus() != null) {
            sql.append("AND p.status = ? ");
            params.add(queryDTO.getStatus());
        }

        if (queryDTO.getSellerId() != null) {
            sql.append("AND p.seller_id = ? ");
            params.add(queryDTO.getSellerId());
        }

        if (queryDTO.getCategoryId() != null) {
            sql.append("AND p.category_id = ? ");
            params.add(queryDTO.getCategoryId());
        }

        // 添加排序
        sql.append("ORDER BY p.").append(queryDTO.getSortBy()).append(" ").append(queryDTO.getSortDirection()).append(" ");

        // 添加分页
        sql.append("LIMIT ? OFFSET ?");
        params.add(queryDTO.getPageSize());
        params.add((queryDTO.getPageNum() - 1) * queryDTO.getPageSize());

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> products = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getLong("id"));
                product.put("title", rs.getString("title"));
                product.put("price", rs.getBigDecimal("price"));
                product.put("status", rs.getInt("status"));
                product.put("statusText", getStatusText(rs.getInt("status")));
                product.put("createTime", rs.getTimestamp("create_time").toLocalDateTime());
                product.put("updateTime", rs.getTimestamp("update_time").toLocalDateTime());
                product.put("sellerId", rs.getLong("seller_id"));
                product.put("sellerName", rs.getString("seller_name"));
                product.put("categoryId", rs.getInt("category_id"));
                products.add(product);
            }

            return products;
        } catch (SQLException e) {
            logger.error("查询商品列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 统计商品数量
     */
    public int countProducts(AdminProductQueryDTO queryDTO) {
        // 参数验证
        if (queryDTO == null) {
            logger.warn("统计商品数量失败: 查询条件为空");
            throw new IllegalArgumentException("查询条件不能为空");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM product p ");
        sql.append("WHERE p.is_deleted = 0 ");

        List<Object> params = new ArrayList<>();

        // 添加查询条件
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            sql.append("AND p.title LIKE ? ");
            params.add("%" + queryDTO.getKeyword().trim() + "%");
        }

        if (queryDTO.getStatus() != null) {
            sql.append("AND p.status = ? ");
            params.add(queryDTO.getStatus());
        }

        if (queryDTO.getSellerId() != null) {
            sql.append("AND p.seller_id = ? ");
            params.add(queryDTO.getSellerId());
        }

        if (queryDTO.getCategoryId() != null) {
            sql.append("AND p.category_id = ? ");
            params.add(queryDTO.getCategoryId());
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql.toString());

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        } catch (SQLException e) {
            logger.error("统计商品数量失败: {}", e.getMessage(), e);
            return 0;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 更新商品状态（管理员操作）
     */
    public boolean updateProductStatus(Long productId, Integer status, Long adminId) {
        // 参数验证
        if (productId == null) {
            logger.warn("更新商品状态失败: 商品ID为空");
            return false;
        }
        if (status == null) {
            logger.warn("更新商品状态失败: 状态为空");
            return false;
        }
        if (productId <= 0) {
            logger.warn("更新商品状态失败: 商品ID无效: {}", productId);
            return false;
        }

        String sql = "UPDATE product SET status = ?, update_time = ? WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(3, productId);

            int affectedRows = pstmt.executeUpdate();
            boolean success = affectedRows > 0;

            if (success) {
                logger.info("管理员 {} 更新商品 {} 状态为 {} 成功", adminId, productId, status);
            } else {
                logger.warn("管理员 {} 更新商品 {} 状态失败: 商品不存在或已删除", adminId, productId);
            }

            return success;
        } catch (SQLException e) {
            logger.error("更新商品状态失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 删除商品（软删除）
     */
    public boolean deleteProduct(Long productId, Long adminId) {
        // 参数验证
        if (productId == null) {
            logger.warn("删除商品失败: 商品ID为空");
            return false;
        }
        if (productId <= 0) {
            logger.warn("删除商品失败: 商品ID无效: {}", productId);
            return false;
        }

        String sql = "UPDATE product SET is_deleted = 1, update_time = ? WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(2, productId);

            int affectedRows = pstmt.executeUpdate();
            boolean success = affectedRows > 0;

            if (success) {
                logger.info("管理员 {} 删除商品 {} 成功", adminId, productId);
            } else {
                logger.warn("管理员 {} 删除商品 {} 失败: 商品不存在或已删除", adminId, productId);
            }

            return success;
        } catch (SQLException e) {
            logger.error("删除商品失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(int status) {
        switch (status) {
            case 0: return "待审核";
            case 1: return "在售";
            case 2: return "已售出";
            case 3: return "已下架";
            case 4: return "草稿";
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
