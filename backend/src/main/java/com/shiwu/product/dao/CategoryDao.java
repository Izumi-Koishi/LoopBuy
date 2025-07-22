package com.shiwu.product.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.product.model.Category;
import com.shiwu.product.model.CategoryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类数据访问对象 - MVC框架版本
 */
@Repository
public class CategoryDao {
    private static final Logger logger = LoggerFactory.getLogger(CategoryDao.class);

    /**
     * 获取所有商品分类列表
     * @return 商品分类列表
     */
    public List<CategoryVO> findAll() {
        String sql = "SELECT id, name, parent_id FROM category WHERE is_deleted = 0 ORDER BY id ASC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<CategoryVO> categories = new ArrayList<>();
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CategoryVO category = new CategoryVO();
                category.setId(rs.getInt("id"));
                category.setName(rs.getString("name"));
                category.setParentId(rs.getInt("parent_id"));
                categories.add(category);
            }
        } catch (SQLException e) {
            logger.error("获取商品分类失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return categories;
    }

    /**
     * 根据ID查询商品分类
     * @param id 商品分类ID
     * @return 商品分类对象，如果不存在则返回null
     */
    public Category findById(Integer id) {
        String sql = "SELECT id, name, parent_id, create_time, update_time, is_deleted FROM category WHERE id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Category category = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                category = new Category();
                category.setId(rs.getInt("id"));
                category.setName(rs.getString("name"));
                category.setParentId(rs.getInt("parent_id"));
                category.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                category.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
                category.setDeleted(rs.getBoolean("is_deleted"));
            }
        } catch (SQLException e) {
            logger.error("根据ID查询商品分类失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return category;
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