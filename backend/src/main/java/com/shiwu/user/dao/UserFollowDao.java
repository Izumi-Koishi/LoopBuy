package com.shiwu.user.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.user.model.UserFollow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户关注数据访问对象 - MVC框架版本
 */
@Repository
public class UserFollowDao {
    
    private static final Logger logger = LoggerFactory.getLogger(UserFollowDao.class);
    
    /**
     * 检查用户A是否关注了用户B
     * 
     * @param followerId 关注者用户ID
     * @param followedId 被关注者用户ID
     * @return true if following, false otherwise
     */
    public boolean isFollowing(Long followerId, Long followedId) {
        // 参数验证
        if (followerId == null || followedId == null) {
            logger.warn("检查关注关系失败: 参数为空 followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        // 自己不能关注自己
        if (followerId.equals(followedId)) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND followed_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("数据库连接为空");
                return false;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, followerId);
            pstmt.setLong(2, followedId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("检查关注关系失败: followerId={}, followedId={}, error={}",
                        followerId, followedId, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("检查关注关系发生未知错误: followerId={}, followedId={}, error={}",
                        followerId, followedId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return false;
    }
    
    /**
     * 关注用户
     * 
     * @param followerId 关注者用户ID
     * @param followedId 被关注者用户ID
     * @return true if successful, false otherwise
     */
    public boolean followUser(Long followerId, Long followedId) {
        // 参数验证
        if (followerId == null || followedId == null) {
            logger.warn("关注用户失败: 参数为空 followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        // 不能关注自己
        if (followerId.equals(followedId)) {
            logger.warn("关注用户失败: 不能关注自己 followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        // 先检查是否已经关注
        if (isFollowing(followerId, followedId)) {
            logger.warn("用户已经关注了目标用户: followerId={}, followedId={}", followerId, followedId);
            return false;
        }
        
        String sql = "INSERT INTO user_follow (follower_id, followed_id, is_deleted) VALUES (?, ?, 0)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, followerId);
            pstmt.setLong(2, followedId);
            
            int rowsAffected = pstmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                logger.info("关注用户成功: followerId={}, followedId={}", followerId, followedId);
            } else {
                logger.warn("关注用户失败: followerId={}, followedId={}", followerId, followedId);
            }
            
            return success;
        } catch (SQLException e) {
            logger.error("关注用户失败: followerId={}, followedId={}, error={}", 
                        followerId, followedId, e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 取关用户
     * 
     * @param followerId 关注者用户ID
     * @param followedId 被关注者用户ID
     * @return true if successful, false otherwise
     */
    public boolean unfollowUser(Long followerId, Long followedId) {
        // 参数验证
        if (followerId == null || followedId == null) {
            logger.warn("取关用户失败: 参数为空 followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        // 不能取关自己
        if (followerId.equals(followedId)) {
            logger.warn("取关用户失败: 不能取关自己 followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        logger.info("开始取关操作: followerId={}, followedId={}", followerId, followedId);

        // 先检查是否已经关注
        boolean isCurrentlyFollowing = isFollowing(followerId, followedId);
        logger.info("当前关注状态: {}", isCurrentlyFollowing);

        if (!isCurrentlyFollowing) {
            logger.warn("用户未关注目标用户，无法取关: followerId={}, followedId={}", followerId, followedId);
            return false;
        }

        String sql = "UPDATE user_follow SET is_deleted = 1 WHERE follower_id = ? AND followed_id = ? AND is_deleted = 0";
        logger.info("执行SQL: {}", sql);

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("数据库连接为空");
                return false;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, followerId);
            pstmt.setLong(2, followedId);

            logger.info("执行UPDATE操作...");
            int rowsAffected = pstmt.executeUpdate();
            logger.info("影响的行数: {}", rowsAffected);

            boolean success = rowsAffected > 0;
            
            if (success) {
                logger.info("取关用户成功: followerId={}, followedId={}", followerId, followedId);
            } else {
                logger.warn("取关用户失败: followerId={}, followedId={}", followerId, followedId);
            }
            
            return success;
        } catch (SQLException e) {
            logger.error("取关用户失败: followerId={}, followedId={}, error={}", 
                        followerId, followedId, e.getMessage(), e);
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    /**
     * 获取用户的粉丝数量
     * 
     * @param userId 用户ID
     * @return 粉丝数量
     */
    public int getFollowerCount(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("获取粉丝数量失败: 用户ID为空");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM user_follow WHERE followed_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            if (conn == null) {
                logger.error("数据库连接为空");
                return 0;
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("获取粉丝数量失败: userId={}, error={}", userId, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("获取粉丝数量发生未知错误: userId={}, error={}", userId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return 0;
    }
    
    /**
     * 获取用户关注的人数
     * 
     * @param userId 用户ID
     * @return 关注的人数
     */
    public int getFollowingCount(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("获取关注数量失败: 用户ID为空");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND is_deleted = 0";
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
            logger.error("获取关注数量失败: userId={}, error={}", userId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return 0;
    }
    
    /**
     * 根据关注关系查询记录
     * 
     * @param followerId 关注者用户ID
     * @param followedId 被关注者用户ID
     * @return 关注记录，如果不存在则返回null
     */
    public UserFollow findByFollowerAndFollowed(Long followerId, Long followedId) {
        // 参数验证
        if (followerId == null || followedId == null) {
            logger.warn("查询关注记录失败: 参数为空 followerId={}, followedId={}", followerId, followedId);
            return null;
        }

        String sql = "SELECT id, follower_id, followed_id, create_time, update_time, is_deleted FROM user_follow WHERE follower_id = ? AND followed_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        UserFollow userFollow = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, followerId);
            pstmt.setLong(2, followedId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                userFollow = new UserFollow();
                userFollow.setId(rs.getLong("id"));
                userFollow.setFollowerId(rs.getLong("follower_id"));
                userFollow.setFollowedId(rs.getLong("followed_id"));
                userFollow.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
                userFollow.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
                userFollow.setDeleted(rs.getBoolean("is_deleted"));
            }
        } catch (SQLException e) {
            logger.error("根据关注关系查询记录失败: followerId={}, followedId={}, error={}", 
                        followerId, followedId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return userFollow;
    }
    
    /**
     * 获取用户的粉丝ID列表
     * 用于Task4_2_1_2: 商品审核通过粉丝通知功能
     *
     * @param userId 用户ID
     * @return 粉丝ID列表
     */
    public List<Long> getFollowerIds(Long userId) {
        if (userId == null) {
            logger.warn("获取粉丝ID列表失败: 用户ID为空");
            return new ArrayList<>();
        }

        String sql = "SELECT follower_id FROM user_follow WHERE followed_id = ? AND is_deleted = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);

            rs = pstmt.executeQuery();

            List<Long> followerIds = new ArrayList<>();
            while (rs.next()) {
                followerIds.add(rs.getLong("follower_id"));
            }

            logger.debug("获取粉丝ID列表成功: userId={}, count={}", userId, followerIds.size());
            return followerIds;

        } catch (SQLException e) {
            logger.error("获取粉丝ID列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("关闭数据库资源失败: {}", e.getMessage(), e);
        }
    }
}
