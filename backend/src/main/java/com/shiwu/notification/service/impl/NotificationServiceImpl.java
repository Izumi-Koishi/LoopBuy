package com.shiwu.notification.service.impl;

import com.shiwu.common.result.Result;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.notification.dao.NotificationDao;
import com.shiwu.notification.model.Notification;
import com.shiwu.notification.service.NotificationService;
import com.shiwu.notification.vo.NotificationVO;
import com.shiwu.user.dao.UserFollowDao;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.model.User;

//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知服务实现类 - MVC框架版本
 *
 * 用于Task4_2_1_2: 商品审核通过粉丝通知功能
 * 实现通知的创建、查询、管理等功能
 *
 * 使用MVC框架的@Service注解和@Autowired依赖注入
 * 继承BaseService获得通用功能支持
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 * @since 2024-01-15
 */
@Service
public class NotificationServiceImpl extends BaseService implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private UserFollowDao userFollowDao;

    @Autowired
    private UserDao userDao;

    // 默认构造函数 - 用于兼容性
    public NotificationServiceImpl() {
        // MVC框架会自动注入依赖
    }

    // 用于测试的构造函数
    public NotificationServiceImpl(NotificationDao notificationDao, UserFollowDao userFollowDao, UserDao userDao) {
        this.notificationDao = notificationDao;
        this.userFollowDao = userFollowDao;
        this.userDao = userDao;
    }
    
    @Override
    public Result<Long> createNotification(Notification notification) {
        try {
            if (notification == null) {
                logger.warn("创建通知失败: 通知对象为空");
                return Result.error("通知对象不能为空");
            }
            
            if (notification.getRecipientId() == null) {
                logger.warn("创建通知失败: 接收者ID为空");
                return Result.error("接收者ID不能为空");
            }
            
            if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
                logger.warn("创建通知失败: 通知标题为空");
                return Result.error("通知标题不能为空");
            }
            
            if (notification.getContent() == null || notification.getContent().trim().isEmpty()) {
                logger.warn("创建通知失败: 通知内容为空");
                return Result.error("通知内容不能为空");
            }
            
            Long notificationId = notificationDao.createNotification(notification);
            
            if (notificationId != null) {
                logger.info("创建通知成功: id={}, recipientId={}", notificationId, notification.getRecipientId());
                return Result.success(notificationId);
            } else {
                logger.error("创建通知失败: 数据库操作失败");
                return Result.error("创建通知失败");
            }
            
        } catch (Exception e) {
            logger.error("创建通知时发生异常: {}", e.getMessage(), e);
            return Result.error("创建通知失败");
        }
    }
    
    @Override
    public Result<Integer> batchCreateNotifications(List<Notification> notifications) {
        try {
            if (notifications == null || notifications.isEmpty()) {
                logger.warn("批量创建通知失败: 通知列表为空");
                return Result.error("通知列表不能为空");
            }
            
            // 验证通知对象
            List<Notification> validNotifications = new ArrayList<>();
            for (Notification notification : notifications) {
                if (notification != null && notification.getRecipientId() != null &&
                    notification.getTitle() != null && !notification.getTitle().trim().isEmpty() &&
                    notification.getContent() != null && !notification.getContent().trim().isEmpty()) {
                    validNotifications.add(notification);
                } else {
                    logger.warn("跳过无效通知: {}", notification);
                }
            }
            
            if (validNotifications.isEmpty()) {
                logger.warn("批量创建通知失败: 没有有效的通知");
                return Result.error("没有有效的通知");
            }
            
            int createdCount = notificationDao.batchCreateNotifications(validNotifications);
            
            logger.info("批量创建通知完成: 总数={}, 成功={}", notifications.size(), createdCount);
            return Result.success(createdCount);
            
        } catch (Exception e) {
            logger.error("批量创建通知时发生异常: {}", e.getMessage(), e);
            return Result.error("批量创建通知失败");
        }
    }
    
    @Override
    public Result<Integer> createProductApprovedNotifications(Long productId, Long sellerId, String productTitle) {
        try {
            if (productId == null || sellerId == null) {
                logger.warn("创建商品审核通过通知失败: 参数为空 productId={}, sellerId={}", productId, sellerId);
                return Result.error("商品ID和卖家ID不能为空");
            }
            
            if (productTitle == null || productTitle.trim().isEmpty()) {
                logger.warn("创建商品审核通过通知失败: 商品标题为空");
                return Result.error("商品标题不能为空");
            }
            
            // 获取卖家信息
            User seller = userDao.findPublicInfoById(sellerId);
            if (seller == null) {
                logger.warn("创建商品审核通过通知失败: 卖家不存在 sellerId={}", sellerId);
                return Result.error("卖家不存在");
            }
            
            // 获取卖家的所有粉丝
            List<Long> followerIds = userFollowDao.getFollowerIds(sellerId);
            
            if (followerIds.isEmpty()) {
                logger.info("卖家没有粉丝，无需创建通知: sellerId={}", sellerId);
                return Result.success(0);
            }
            
            // 为每个粉丝创建通知
            List<Notification> notifications = new ArrayList<>();
            String sellerName = seller.getUsername();
            
            for (Long followerId : followerIds) {
                Notification notification = new Notification();
                notification.setRecipientId(followerId);
                notification.setTitle("您关注的 " + sellerName + " 发布了新商品");
                notification.setContent("您关注的卖家 " + sellerName + " 刚刚发布了新商品《" + productTitle + "》，快来看看吧！");
                notification.setNotificationType(Notification.TYPE_PRODUCT_APPROVED);
                notification.setSourceType(Notification.SOURCE_PRODUCT);
                notification.setSourceId(productId);
                notification.setRelatedUserId(sellerId);
                notification.setRelatedUserName(sellerName);
                notification.setActionUrl("/product/" + productId);
                notification.setPriority(Notification.PRIORITY_NORMAL);
                notification.setExpireAfterHours(168); // 7天过期
                
                notifications.add(notification);
            }
            
            // 批量创建通知
            int createdCount = notificationDao.batchCreateNotifications(notifications);
            
            logger.info("为商品审核通过创建粉丝通知完成: productId={}, sellerId={}, followerCount={}, createdCount={}", 
                       productId, sellerId, followerIds.size(), createdCount);
            
            return Result.success(createdCount);
            
        } catch (Exception e) {
            logger.error("创建商品审核通过通知时发生异常: productId={}, sellerId={}, error={}", 
                        productId, sellerId, e.getMessage(), e);
            return Result.error("创建商品审核通过通知失败");
        }
    }
    
    @Override
    public Result<List<NotificationVO>> getUserNotifications(Long userId, int page, int size, boolean onlyUnread) {
        try {
            if (userId == null) {
                logger.warn("获取用户通知失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            if (page < 1) {
                page = 1;
            }
            
            if (size < 1 || size > 100) {
                size = 20; // 默认每页20条
            }
            
            List<Notification> notifications = notificationDao.findNotificationsByUserId(userId, page, size, onlyUnread);
            
            // 转换为VO
            List<NotificationVO> notificationVOs = new ArrayList<>();
            for (Notification notification : notifications) {
                NotificationVO vo = convertToNotificationVO(notification);
                notificationVOs.add(vo);
            }
            
            logger.debug("获取用户通知成功: userId={}, count={}, onlyUnread={}", userId, notificationVOs.size(), onlyUnread);
            return Result.success(notificationVOs);
            
        } catch (Exception e) {
            logger.error("获取用户通知时发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.error("获取用户通知失败");
        }
    }
    
    @Override
    public Result<Integer> getUnreadNotificationCount(Long userId) {
        try {
            if (userId == null) {
                logger.warn("获取未读通知数量失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            int count = notificationDao.getUnreadNotificationCount(userId);
            
            logger.debug("获取未读通知数量成功: userId={}, count={}", userId, count);
            return Result.success(count);
            
        } catch (Exception e) {
            logger.error("获取未读通知数量时发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.error("获取未读通知数量失败");
        }
    }
    
    @Override
    public Result<Void> markNotificationAsRead(Long notificationId, Long userId) {
        try {
            if (notificationId == null || userId == null) {
                logger.warn("标记通知已读失败: 参数为空");
                return Result.error("通知ID和用户ID不能为空");
            }
            
            boolean success = notificationDao.markNotificationAsRead(notificationId, userId);
            
            if (success) {
                logger.info("标记通知已读成功: notificationId={}, userId={}", notificationId, userId);
                return Result.success(null);
            } else {
                logger.warn("标记通知已读失败: 通知不存在或已读 notificationId={}, userId={}", notificationId, userId);
                return Result.error("通知不存在或已读");
            }
            
        } catch (Exception e) {
            logger.error("标记通知已读时发生异常: notificationId={}, userId={}, error={}", 
                        notificationId, userId, e.getMessage(), e);
            return Result.error("标记通知已读失败");
        }
    }
    
    @Override
    public Result<Integer> batchMarkNotificationsAsRead(Long userId, List<Long> notificationIds) {
        try {
            if (userId == null) {
                logger.warn("批量标记通知已读失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            int markedCount = notificationDao.batchMarkNotificationsAsRead(userId, notificationIds);
            
            logger.info("批量标记通知已读成功: userId={}, count={}", userId, markedCount);
            return Result.success(markedCount);
            
        } catch (Exception e) {
            logger.error("批量标记通知已读时发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.error("批量标记通知已读失败");
        }
    }
    
    @Override
    public Result<Integer> createSystemNotice(String title, String content, List<Long> targetUserIds, 
                                            Integer priority, Integer expireHours) {
        try {
            if (title == null || title.trim().isEmpty()) {
                logger.warn("创建系统公告失败: 标题为空");
                return Result.error("公告标题不能为空");
            }
            
            if (content == null || content.trim().isEmpty()) {
                logger.warn("创建系统公告失败: 内容为空");
                return Result.error("公告内容不能为空");
            }
            
            if (priority == null) {
                priority = Notification.PRIORITY_IMPORTANT;
            }
            
            // 如果没有指定目标用户，则获取所有活跃用户
            List<Long> recipientIds = targetUserIds;
            if (recipientIds == null || recipientIds.isEmpty()) {
                recipientIds = userDao.getAllActiveUserIds();
            }
            
            if (recipientIds.isEmpty()) {
                logger.warn("创建系统公告失败: 没有目标用户");
                return Result.error("没有目标用户");
            }
            
            // 为每个用户创建通知
            List<Notification> notifications = new ArrayList<>();
            
            for (Long userId : recipientIds) {
                Notification notification = new Notification();
                notification.setRecipientId(userId);
                notification.setTitle("系统公告：" + title);
                notification.setContent(content);
                notification.setNotificationType(Notification.TYPE_SYSTEM_NOTICE);
                notification.setSourceType(Notification.SOURCE_SYSTEM);
                notification.setPriority(priority);
                
                if (expireHours != null && expireHours > 0) {
                    notification.setExpireAfterHours(expireHours);
                }
                
                notifications.add(notification);
            }
            
            // 批量创建通知
            int createdCount = notificationDao.batchCreateNotifications(notifications);
            
            logger.info("创建系统公告完成: title={}, targetCount={}, createdCount={}", 
                       title, recipientIds.size(), createdCount);
            
            return Result.success(createdCount);
            
        } catch (Exception e) {
            logger.error("创建系统公告时发生异常: title={}, error={}", title, e.getMessage(), e);
            return Result.error("创建系统公告失败");
        }
    }
    
    @Override
    public Result<Long> createMessageNotification(Long recipientId, Long senderId, String senderName, 
                                                String messageContent, String conversationId) {
        try {
            if (recipientId == null || senderId == null) {
                logger.warn("创建消息通知失败: 参数为空");
                return Result.error("接收者ID和发送者ID不能为空");
            }
            
            if (senderName == null || senderName.trim().isEmpty()) {
                logger.warn("创建消息通知失败: 发送者名称为空");
                return Result.error("发送者名称不能为空");
            }
            
            // 截取消息内容摘要
            String contentSummary = messageContent;
            if (contentSummary != null && contentSummary.length() > 50) {
                contentSummary = contentSummary.substring(0, 50) + "...";
            }
            
            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setTitle("您收到了来自 " + senderName + " 的新消息");
            notification.setContent(senderName + " 给您发送了一条消息：" + (contentSummary != null ? contentSummary : ""));
            notification.setNotificationType(Notification.TYPE_MESSAGE_RECEIVED);
            notification.setSourceType(Notification.SOURCE_MESSAGE);
            notification.setRelatedUserId(senderId);
            notification.setRelatedUserName(senderName);
            notification.setActionUrl("/message/" + conversationId);
            notification.setPriority(Notification.PRIORITY_NORMAL);
            notification.setExpireAfterHours(72); // 3天过期
            
            Long notificationId = notificationDao.createNotification(notification);
            
            if (notificationId != null) {
                logger.info("创建消息通知成功: id={}, recipientId={}, senderId={}", 
                           notificationId, recipientId, senderId);
                return Result.success(notificationId);
            } else {
                logger.error("创建消息通知失败: 数据库操作失败");
                return Result.error("创建消息通知失败");
            }
            
        } catch (Exception e) {
            logger.error("创建消息通知时发生异常: recipientId={}, senderId={}, error={}", 
                        recipientId, senderId, e.getMessage(), e);
            return Result.error("创建消息通知失败");
        }
    }
    
    /**
     * 将Notification转换为NotificationVO
     */
    private NotificationVO convertToNotificationVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setNotificationType(notification.getNotificationType());
        vo.setSourceType(notification.getSourceType());
        vo.setSourceId(notification.getSourceId());
        vo.setRelatedUserId(notification.getRelatedUserId());
        vo.setRelatedUserName(notification.getRelatedUserName());
        vo.setActionUrl(notification.getActionUrl());
        vo.setIsRead(notification.getIsRead());
        vo.setReadTime(notification.getReadTime());
        vo.setPriority(notification.getPriority());
        vo.setCreateTime(notification.getCreateTime());
        
        return vo;
    }
}
