package com.shiwu.product.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.product.model.Product;
import com.shiwu.product.model.ProductCardVO;
import com.shiwu.product.model.ProductDetailVO;
import com.shiwu.product.model.ProductImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品数据访问对象 - MVC框架版本
 */
@Repository
public class ProductDao {
    private static final Logger logger = LoggerFactory.getLogger(ProductDao.class);
    private static final Integer PRODUCT_STATUS_ONSALE = 1; // 在售状态

    /**
     * 查询商品列表，支持复合条件
     * @param keyword 搜索关键词，搜索标题和描述
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortBy 排序字段
     * @param sortDirection 排序方向（asc/desc）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 查询结果（包括商品列表和总数）
     */
    public Map<String, Object> findProducts(String keyword, Integer categoryId, 
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          String sortBy, String sortDirection,
                                          int pageNum, int pageSize) {
        // 构建基础查询和条件
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder countSqlBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // 查询语句基础部分
        sqlBuilder.append("SELECT p.id, p.title, p.price, p.status, p.create_time, p.seller_id, ")
                 .append("(SELECT image_url FROM product_image WHERE product_id = p.id AND is_main = 1 LIMIT 1) AS main_image_url ")
                 .append("FROM product p WHERE p.status = ? AND p.is_deleted = 0 ");
        
        // 计数语句基础部分
        countSqlBuilder.append("SELECT COUNT(*) FROM product p WHERE p.status = ? AND p.is_deleted = 0 ");
        
        // 添加基础参数
        params.add(PRODUCT_STATUS_ONSALE);
        
        // 添加搜索条件
        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlBuilder.append("AND (p.title LIKE ? OR p.description LIKE ?) ");
            countSqlBuilder.append("AND (p.title LIKE ? OR p.description LIKE ?) ");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        
        // 添加分类条件
        if (categoryId != null) {
            sqlBuilder.append("AND p.category_id = ? ");
            countSqlBuilder.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        
        // 添加价格范围条件
        if (minPrice != null) {
            sqlBuilder.append("AND p.price >= ? ");
            countSqlBuilder.append("AND p.price >= ? ");
            params.add(minPrice);
        }
        
        if (maxPrice != null) {
            sqlBuilder.append("AND p.price <= ? ");
            countSqlBuilder.append("AND p.price <= ? ");
            params.add(maxPrice);
        }
        
        // 添加排序
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            // 安全处理排序字段
            String safeField = getSafeOrderField(sortBy);
            String safeDirection = sortDirection != null && "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
            sqlBuilder.append("ORDER BY p.").append(safeField).append(" ").append(safeDirection);
        } else {
            // 默认按创建时间倒序
            sqlBuilder.append("ORDER BY p.create_time DESC");
        }
        
        // 添加分页
        sqlBuilder.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((pageNum - 1) * pageSize);
        
        // 执行查询
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        List<ProductCardVO> products = new ArrayList<>();
        int total = 0;
        
        try {
            conn = DBUtil.getConnection();
            
            // 先查询总数
            pstmt = conn.prepareStatement(countSqlBuilder.toString());
            for (int i = 0; i < params.size() - 2; i++) {  // 减去分页的两个参数
                pstmt.setObject(i + 1, params.get(i));
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                total = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            
            // 若总数为0，直接返回空列表
            if (total == 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("list", products);
                result.put("total", 0);
                result.put("pageNum", pageNum);
                result.put("pageSize", pageSize);
                result.put("pages", 0);
                return result;
            }
            
            // 查询商品列表
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ProductCardVO product = new ProductCardVO();
                product.setId(rs.getLong("id"));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setStatus(rs.getInt("status"));
                product.setMainImageUrl(rs.getString("main_image_url"));
                product.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                product.setSellerId(rs.getLong("seller_id"));
                products.add(product);
            }
        } catch (SQLException e) {
            logger.error("查询商品列表失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", products);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("pages", pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);  // 总页数，防止除零
        
        return result;
    }

    /**
     * 获取安全的排序字段名，防止SQL注入
     */
    private String getSafeOrderField(String field) {
        if ("price".equalsIgnoreCase(field)) {
            return "price";
        } else if ("create_time".equalsIgnoreCase(field)) {
            return "create_time";
        } else {
            return "create_time";  // 默认按创建时间
        }
    }

    /**
     * 获取用户在售商品
     * @param sellerId 卖家ID
     * @return 在售商品列表
     */
    public List<ProductCardVO> findOnSaleProductsBySellerId(Long sellerId) {
        String sql = "SELECT p.id, p.title, p.price, p.create_time, " +
                "    (SELECT image_url FROM product_image WHERE product_id = p.id AND is_main = 1 LIMIT 1) AS main_image_url " +
                "FROM product p " +
                "WHERE p.seller_id = ? AND p.status = ? AND p.is_deleted = 0 " +
                "ORDER BY p.create_time DESC";
                
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ProductCardVO> products = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, sellerId);
            pstmt.setInt(2, PRODUCT_STATUS_ONSALE);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                ProductCardVO product = new ProductCardVO();
                product.setId(rs.getLong("id"));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setMainImageUrl(rs.getString("main_image_url"));
                product.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                products.add(product);
            }
        } catch (SQLException e) {
            logger.error("查询用户在售商品失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return products;
    }
    
    /**
     * 根据卖家ID和商品状态查询商品列表
     * @param sellerId 卖家ID
     * @param status 商品状态，如果为null则查询所有状态
     * @return 商品列表
     */
    public List<ProductCardVO> findProductsBySellerIdAndStatus(Long sellerId, Integer status) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT p.id, p.title, p.price, p.status, p.create_time, ")
                 .append("    (SELECT image_url FROM product_image WHERE product_id = p.id AND is_main = 1 LIMIT 1) AS main_image_url ")
                 .append("FROM product p ")
                 .append("WHERE p.seller_id = ? AND p.is_deleted = 0 ");
                
        if (status != null) {
            sqlBuilder.append("AND p.status = ? ");
        }
        sqlBuilder.append("ORDER BY p.create_time DESC");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ProductCardVO> products = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sqlBuilder.toString());
            pstmt.setLong(1, sellerId);
            
            if (status != null) {
                pstmt.setInt(2, status);
            }
            
            rs = pstmt.executeQuery();

            while (rs.next()) {
                ProductCardVO product = new ProductCardVO();
                product.setId(rs.getLong("id"));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setStatus(rs.getInt("status"));
                product.setMainImageUrl(rs.getString("main_image_url"));
                product.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                products.add(product);
            }
        } catch (SQLException e) {
            logger.error("查询用户商品列表失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return products;
    }
    
    /**
     * 创建商品
     * @param product 商品信息
     * @return 创建的商品ID，如果创建失败则返回null
     */
    public Long createProduct(Product product) {
        String sql = "INSERT INTO product (seller_id, category_id, title, description, price, status) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long productId = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, product.getSellerId());
            pstmt.setInt(2, product.getCategoryId());
            pstmt.setString(3, product.getTitle());
            pstmt.setString(4, product.getDescription());
            pstmt.setBigDecimal(5, product.getPrice());
            pstmt.setInt(6, product.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    productId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("创建商品失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return productId;
    }
    
    /**
     * 更新商品信息
     * @param product 商品信息
     * @return 更新是否成功
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE product SET category_id = ?, title = ?, description = ?, price = ? WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, product.getCategoryId());
            pstmt.setString(2, product.getTitle());
            pstmt.setString(3, product.getDescription());
            pstmt.setBigDecimal(4, product.getPrice());
            pstmt.setLong(5, product.getId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("更新商品信息失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 更新商品状态
     * @param productId 商品ID
     * @param status 新状态
     * @param sellerId 卖家ID (用于权限验证)
     * @return 更新是否成功
     */
    public boolean updateProductStatus(Long productId, Integer status, Long sellerId) {
        String sql = "UPDATE product SET status = ? WHERE id = ? AND seller_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setLong(2, productId);
            pstmt.setLong(3, sellerId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("更新商品状态失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 系统级别更新商品状态（不需要权限验证，用于订单系统等）
     * @param productId 商品ID
     * @param status 新状态
     * @return 更新是否成功
     */
    public boolean updateProductStatusBySystem(Long productId, Integer status) {
        String sql = "UPDATE product SET status = ?, update_time = NOW() WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setLong(2, productId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("系统更新商品状态成功: productId={}, status={}", productId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("系统更新商品状态失败: productId={}, status={}, error={}", productId, status, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }

        return false;
    }
    
    /**
     * 删除商品（逻辑删除）
     * @param productId 商品ID
     * @param sellerId 卖家ID (用于权限验证)
     * @return 删除是否成功
     */
    public boolean deleteProduct(Long productId, Long sellerId) {
        String sql = "UPDATE product SET is_deleted = 1 WHERE id = ? AND seller_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, productId);
            pstmt.setLong(2, sellerId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("删除商品失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 添加商品图片
     * @param productImage 商品图片信息
     * @return 添加是否成功
     */
    public boolean addProductImage(ProductImage productImage) {
        String sql = "INSERT INTO product_image (product_id, image_url, is_main) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, productImage.getProductId());
            pstmt.setString(2, productImage.getImageUrl());
            pstmt.setBoolean(3, productImage.getIsMain());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("添加商品图片失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 根据ID查询商品
     * @param id 商品ID
     * @return 商品对象，如果不存在则返回null
     */
    public Product findById(Long id) {
        String sql = "SELECT id, seller_id, category_id, title, description, price, status, create_time, update_time, is_deleted " +
                    "FROM product WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Product product = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, id);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                product = new Product();
                product.setId(rs.getLong("id"));
                product.setSellerId(rs.getLong("seller_id"));
                product.setCategoryId(rs.getInt("category_id"));
                product.setTitle(rs.getString("title"));
                product.setDescription(rs.getString("description"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setStatus(rs.getInt("status"));
                product.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                product.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
                product.setDeleted(rs.getBoolean("is_deleted"));
            }
        } catch (SQLException e) {
            logger.error("根据ID查询商品失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return product;
    }
    
    /**
     * 获取商品的所有图片
     * @param productId 商品ID
     * @return 商品图片列表
     */
    public List<ProductImage> findImagesByProductId(Long productId) {
        String sql = "SELECT id, product_id, image_url, is_main, create_time FROM product_image WHERE product_id = ? ORDER BY is_main DESC, id ASC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ProductImage> images = new ArrayList<>();
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, productId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ProductImage image = new ProductImage();
                image.setId(rs.getLong("id"));
                image.setProductId(rs.getLong("product_id"));
                image.setImageUrl(rs.getString("image_url"));
                image.setIsMain(rs.getBoolean("is_main"));
                image.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                images.add(image);
            }
        } catch (SQLException e) {
            logger.error("获取商品图片失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return images;
    }

    /**
     * 获取商品详情，包括商品基本信息、卖家信息、分类信息和图片列表
     * @param productId 商品ID
     * @return 商品详情视图对象，如果商品不存在则返回null
     */
    public ProductDetailVO findProductDetailById(Long productId) {
        // 构建SQL查询
        String sql = "SELECT p.id, p.title, p.description, p.price, p.status, p.create_time, " +
                "p.category_id, c.name AS category_name, " +
                "p.seller_id, u.nickname AS seller_name, u.avatar_url AS seller_avatar " +
                "FROM product p " +
                "LEFT JOIN category c ON p.category_id = c.id " +
                "LEFT JOIN system_user u ON p.seller_id = u.id " +
                "WHERE p.id = ? AND p.is_deleted = 0";
                
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ProductDetailVO productDetail = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, productId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                productDetail = new ProductDetailVO();
                productDetail.setId(rs.getLong("id"));
                productDetail.setTitle(rs.getString("title"));
                productDetail.setDescription(rs.getString("description"));
                productDetail.setPrice(rs.getBigDecimal("price"));
                productDetail.setStatus(rs.getInt("status"));
                productDetail.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                productDetail.setCategoryId(rs.getInt("category_id"));
                productDetail.setCategoryName(rs.getString("category_name"));
                productDetail.setSellerId(rs.getLong("seller_id"));
                productDetail.setSellerName(rs.getString("seller_name"));
                productDetail.setSellerAvatar(rs.getString("seller_avatar"));
                
                // 查询商品图片
                List<String> imageUrls = new ArrayList<>();
                String mainImageUrl = null;
                
                // 关闭当前结果集和语句
                rs.close();
                pstmt.close();
                
                // 查询商品图片
                String imageSql = "SELECT image_url, is_main FROM product_image WHERE product_id = ? ORDER BY is_main DESC, id ASC";
                pstmt = conn.prepareStatement(imageSql);
                pstmt.setLong(1, productId);
                rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String imageUrl = rs.getString("image_url");
                    boolean isMain = rs.getBoolean("is_main");
                    
                    imageUrls.add(imageUrl);
                    if (isMain && mainImageUrl == null) {
                        mainImageUrl = imageUrl;
                    }
                }
                
                productDetail.setImageUrls(imageUrls);
                productDetail.setMainImageUrl(mainImageUrl);
            }
        } catch (SQLException e) {
            logger.error("查询商品详情失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return productDetail;
    }

    // ====================================================================
    // 统计查询方法（用于管理员仪表盘）
    // ====================================================================

    /**
     * 获取商品总数
     * @return 商品总数
     */
    public Long getTotalProductCount() {
        String sql = "SELECT COUNT(*) FROM product WHERE is_deleted = 0";
        return executeCountQuery(sql);
    }

    /**
     * 获取指定状态的商品数
     * @param status 商品状态
     * @return 商品数
     */
    public Long getProductCountByStatus(Integer status) {
        String sql = "SELECT COUNT(*) FROM product WHERE is_deleted = 0 AND status = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long count = 0L;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("查询指定状态商品数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取指定时间段内新增商品数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 新增商品数
     */
    public Long getNewProductCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM product WHERE is_deleted = 0 AND create_time >= ? AND create_time < ?";
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
            logger.error("查询新增商品数失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
    }

    /**
     * 获取商品增长趋势数据（按天统计）
     * @param days 统计天数
     * @return 趋势数据列表，每个元素包含日期和当天新增商品数
     */
    public List<Map<String, Object>> getProductGrowthTrend(int days) {
        String sql = "SELECT DATE(create_time) as date, COUNT(*) as count " +
                    "FROM product " +
                    "WHERE is_deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
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
            logger.error("查询商品增长趋势失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return trendData;
    }

    /**
     * 获取商品状态分布统计
     * @return 状态分布数据，key为状态值，value为数量
     */
    public Map<Integer, Long> getProductStatusDistribution() {
        String sql = "SELECT status, COUNT(*) as count FROM product WHERE is_deleted = 0 GROUP BY status";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<Integer, Long> distribution = new HashMap<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Integer status = rs.getInt("status");
                Long count = rs.getLong("count");
                distribution.put(status, count);
            }
        } catch (SQLException e) {
            logger.error("查询商品状态分布失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return distribution;
    }

    /**
     * 执行计数查询的通用方法
     * @param sql SQL查询语句
     * @return 计数结果
     */
    private Long executeCountQuery(String sql) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long count = 0L;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("数据库连接为空");
                return count;
            }

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("执行计数查询失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("执行计数查询时发生未知异常: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return count;
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