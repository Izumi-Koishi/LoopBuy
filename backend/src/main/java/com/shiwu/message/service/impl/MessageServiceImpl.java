package com.shiwu.message.service.impl;

import com.shiwu.common.result.Result;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.message.dao.ConversationDao;
import com.shiwu.message.dao.MessageDao;
import com.shiwu.message.dto.MessageSendDTO;
import com.shiwu.message.model.Conversation;
import com.shiwu.message.model.Message;
import com.shiwu.message.service.MessageService;
import com.shiwu.message.vo.ConversationVO;
import com.shiwu.message.vo.MessageVO;
import com.shiwu.notification.service.NotificationService;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息服务实现类 - MVC框架版本
 *
 * 实现实时消息收发、会话管理等功能
 * 支持基于轮询的实时消息推送
 *
 * 使用MVC框架的@Service注解和@Autowired依赖注入
 * 继承BaseService获得通用功能支持
 *
 * Task4_3_1_2: 在消息发送时创建通知
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Service
public class MessageServiceImpl extends BaseService implements MessageService {

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userDao;

    /**
     * 默认构造函数 - MVC框架会自动注入依赖
     */
    public MessageServiceImpl() {
        logger.info("MessageServiceImpl初始化 - 使用MVC框架依赖注入");
    }

    /**
     * 兼容性构造函数 - 用于测试或非MVC环境
     * 保持向后兼容性，支持渐进式迁移
     */
    public MessageServiceImpl(MessageDao messageDao, ConversationDao conversationDao,
                             NotificationService notificationService, UserDao userDao) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.notificationService = notificationService;
        this.userDao = userDao;
        logger.info("MessageServiceImpl初始化 - 使用手动依赖注入（兼容模式）");
    }
    
    @Override
    public Result<MessageVO> sendMessage(Long senderId, MessageSendDTO dto) {
        try {
            // 参数验证
            if (senderId == null || dto == null) {
                logger.warn("发送消息失败: 参数为空");
                return Result.error("参数不能为空");
            }
            
            if (dto.getReceiverId() == null || dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                logger.warn("发送消息失败: 必要参数为空");
                return Result.error("接收者ID和消息内容不能为空");
            }
            
            if (senderId.equals(dto.getReceiverId())) {
                logger.warn("发送消息失败: 不能给自己发送消息");
                return Result.error("不能给自己发送消息");
            }
            
            // 创建或获取会话
            String conversationId = generateConversationId(senderId, dto.getReceiverId(), dto.getProductId());
            Conversation conversation = conversationDao.findConversationById(conversationId);

            if (conversation == null) {
                // 创建新会话
                conversation = new Conversation(conversationId, senderId, dto.getReceiverId(), dto.getProductId());
                Long newConversationId = conversationDao.insertConversation(conversation);
                if (newConversationId == null) {
                    logger.error("创建会话失败: conversationId={}", conversationId);
                    return Result.error("创建会话失败");
                }
                conversation.setId(newConversationId);
            }
            
            // 创建消息
            Message message = new Message();
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setReceiverId(dto.getReceiverId());
            message.setProductId(dto.getProductId());
            message.setContent(dto.getContent().trim());
            message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "TEXT");
            message.setIsRead(false);
            message.setCreateTime(LocalDateTime.now());
            
            // 保存消息
            Long messageId = messageDao.insertMessage(message);
            if (messageId == null) {
                logger.error("保存消息失败: senderId={}, receiverId={}", senderId, dto.getReceiverId());
                return Result.error("保存消息失败");
            }
            message.setId(messageId);
            
            // 更新会话信息
            updateConversationAfterMessage(conversation, message, senderId);

            // Task4_3_1_2: 创建消息通知
            createMessageNotification(message);

            // 转换为VO
            MessageVO messageVO = convertToMessageVO(message);

            logger.info("消息发送成功: messageId={}, senderId={}, receiverId={}",
                       messageId, senderId, dto.getReceiverId());

            return Result.success(messageVO);
            
        } catch (Exception e) {
            logger.error("发送消息时发生异常: senderId={}, receiverId={}", 
                        senderId, dto != null ? dto.getReceiverId() : null, e);
            return Result.error("发送消息失败");
        }
    }
    
    @Override
    public Result<List<ConversationVO>> getConversations(Long userId, int page, int size) {
        try {
            if (userId == null) {
                logger.warn("获取会话列表失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;
            
            int offset = (page - 1) * size;
            List<Conversation> conversations = conversationDao.findConversationsByUserId(userId, null, null, offset, size);
            
            List<ConversationVO> conversationVOs = new ArrayList<>();
            for (Conversation conversation : conversations) {
                ConversationVO vo = convertToConversationVO(conversation, userId);
                conversationVOs.add(vo);
            }
            
            logger.info("获取会话列表成功: userId={}, count={}", userId, conversationVOs.size());
            return Result.success(conversationVOs);
            
        } catch (Exception e) {
            logger.error("获取会话列表时发生异常: userId={}", userId, e);
            return Result.error("获取会话列表失败");
        }
    }
    
    @Override
    public Result<List<MessageVO>> getMessageHistory(Long userId, String conversationId, int page, int size) {
        try {
            if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
                logger.warn("获取消息历史失败: 参数为空");
                return Result.error("参数不能为空");
            }
            
            // 检查权限
            if (!hasConversationPermission(userId, conversationId)) {
                logger.warn("获取消息历史失败: 用户{}无权限访问会话{}", userId, conversationId);
                return Result.error("无权限访问该会话");
            }
            
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 50;
            
            int offset = (page - 1) * size;
            List<Message> messages = messageDao.findMessagesByConversationId(conversationId, offset, size);
            
            List<MessageVO> messageVOs = new ArrayList<>();
            for (Message message : messages) {
                MessageVO vo = convertToMessageVO(message);
                messageVOs.add(vo);
            }
            
            logger.info("获取消息历史成功: userId={}, conversationId={}, count={}", 
                       userId, conversationId, messageVOs.size());
            
            return Result.success(messageVOs);
            
        } catch (Exception e) {
            logger.error("获取消息历史时发生异常: userId={}, conversationId={}", userId, conversationId, e);
            return Result.error("获取消息历史失败");
        }
    }
    
    @Override
    public Result<Void> markMessagesAsRead(Long userId, String conversationId) {
        try {
            if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
                logger.warn("标记消息已读失败: 参数为空");
                return Result.error("参数不能为空");
            }
            
            // 检查权限
            if (!hasConversationPermission(userId, conversationId)) {
                logger.warn("标记消息已读失败: 用户{}无权限访问会话{}", userId, conversationId);
                return Result.error("无权限访问该会话");
            }
            
            // 标记消息为已读
            boolean success = messageDao.markMessagesAsRead(conversationId, userId);
            if (!success) {
                logger.warn("标记消息已读失败: conversationId={}, userId={}", conversationId, userId);
                return Result.error("标记消息已读失败");
            }
            
            // 更新会话的未读数量
            conversationDao.updateUnreadCount(conversationId, userId, 0);
            
            logger.info("标记消息已读成功: userId={}, conversationId={}", userId, conversationId);
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("标记消息已读时发生异常: userId={}, conversationId={}", userId, conversationId, e);
            return Result.error("标记消息已读失败");
        }
    }
    
    @Override
    public Result<List<MessageVO>> getNewMessages(Long userId, Long lastMessageTime) {
        try {
            if (userId == null) {
                logger.warn("获取新消息失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            if (lastMessageTime == null || lastMessageTime < 0) {
                lastMessageTime = 0L;
            }
            
            LocalDateTime lastTime = LocalDateTime.now().minusSeconds(lastMessageTime / 1000);
            List<Message> newMessages = messageDao.findNewMessagesByUserId(userId, lastTime);
            
            List<MessageVO> messageVOs = new ArrayList<>();
            for (Message message : newMessages) {
                MessageVO vo = convertToMessageVO(message);
                messageVOs.add(vo);
            }
            
            logger.debug("获取新消息成功: userId={}, count={}", userId, messageVOs.size());
            return Result.success(messageVOs);
            
        } catch (Exception e) {
            logger.error("获取新消息时发生异常: userId={}", userId, e);
            return Result.error("获取新消息失败");
        }
    }
    
    @Override
    public Result<Integer> getUnreadMessageCount(Long userId) {
        try {
            if (userId == null) {
                logger.warn("获取未读消息数量失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            int count = conversationDao.getTotalUnreadCount(userId);
            
            logger.debug("获取未读消息数量成功: userId={}, count={}", userId, count);
            return Result.success(count);
            
        } catch (Exception e) {
            logger.error("获取未读消息数量时发生异常: userId={}", userId, e);
            return Result.error("获取未读消息数量失败");
        }
    }
    
    @Override
    public Result<ConversationVO> getOrCreateConversation(Long participant1Id, Long participant2Id, Long productId) {
        try {
            if (participant1Id == null || participant2Id == null) {
                logger.warn("创建或获取会话失败: 参与者ID为空");
                return Result.error("参与者ID不能为空");
            }
            
            if (participant1Id.equals(participant2Id)) {
                logger.warn("创建或获取会话失败: 参与者不能相同");
                return Result.error("参与者不能相同");
            }
            
            String conversationId = generateConversationId(participant1Id, participant2Id, productId);
            Conversation conversation = conversationDao.findConversationById(conversationId);

            if (conversation == null) {
                // 创建新会话
                conversation = new Conversation(conversationId, participant1Id, participant2Id, productId);
                Long newConversationId = conversationDao.insertConversation(conversation);
                if (newConversationId == null) {
                    logger.error("创建会话失败: conversationId={}", conversationId);
                    return Result.error("创建会话失败");
                }
                conversation.setId(newConversationId);
                logger.info("创建新会话成功: conversationId={}", conversationId);
            }
            
            ConversationVO conversationVO = convertToConversationVO(conversation, participant1Id);
            return Result.success(conversationVO);
            
        } catch (Exception e) {
            logger.error("创建或获取会话时发生异常: participant1Id={}, participant2Id={}", 
                        participant1Id, participant2Id, e);
            return Result.error("创建或获取会话失败");
        }
    }
    
    @Override
    public boolean hasConversationPermission(Long userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
            return false;
        }
        
        try {
            Conversation conversation = conversationDao.findConversationById(conversationId);
            if (conversation == null) {
                return false;
            }
            
            return userId.equals(conversation.getParticipant1Id()) || 
                   userId.equals(conversation.getParticipant2Id());
                   
        } catch (Exception e) {
            logger.error("检查会话权限时发生异常: userId={}, conversationId={}", userId, conversationId, e);
            return false;
        }
    }
    
    @Override
    public Result<ConversationVO> getConversationDetail(Long userId, String conversationId) {
        try {
            if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
                logger.warn("获取会话详情失败: 参数为空");
                return Result.error("参数不能为空");
            }
            
            // 检查权限
            if (!hasConversationPermission(userId, conversationId)) {
                logger.warn("获取会话详情失败: 用户{}无权限访问会话{}", userId, conversationId);
                return Result.error("无权限访问该会话");
            }
            
            Conversation conversation = conversationDao.findConversationById(conversationId);
            if (conversation == null) {
                logger.warn("获取会话详情失败: 会话不存在, conversationId={}", conversationId);
                return Result.error("会话不存在");
            }
            
            ConversationVO conversationVO = convertToConversationVO(conversation, userId);
            
            logger.info("获取会话详情成功: userId={}, conversationId={}", userId, conversationId);
            return Result.success(conversationVO);
            
        } catch (Exception e) {
            logger.error("获取会话详情时发生异常: userId={}, conversationId={}", userId, conversationId, e);
            return Result.error("获取会话详情失败");
        }
    }
    
    @Override
    public Result<Void> updateConversationStatus(Long userId, String conversationId, String status) {
        try {
            if (userId == null || conversationId == null || status == null) {
                logger.warn("更新会话状态失败: 参数为空");
                return Result.error("参数不能为空");
            }
            
            // 检查权限
            if (!hasConversationPermission(userId, conversationId)) {
                logger.warn("更新会话状态失败: 用户{}无权限访问会话{}", userId, conversationId);
                return Result.error("无权限访问该会话");
            }
            
            // 验证状态值
            if (!isValidStatus(status)) {
                logger.warn("更新会话状态失败: 无效的状态值, status={}", status);
                return Result.error("无效的状态值");
            }
            
            boolean success = conversationDao.updateStatus(conversationId, status);
            if (!success) {
                logger.warn("更新会话状态失败: conversationId={}, status={}", conversationId, status);
                return Result.error("更新会话状态失败");
            }
            
            logger.info("更新会话状态成功: userId={}, conversationId={}, status={}", 
                       userId, conversationId, status);
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("更新会话状态时发生异常: userId={}, conversationId={}, status={}", 
                        userId, conversationId, status, e);
            return Result.error("更新会话状态失败");
        }
    }
    
    /**
     * 生成会话ID
     */
    private String generateConversationId(Long participant1Id, Long participant2Id, Long productId) {
        // 确保较小的ID在前面，保证会话ID的唯一性
        Long smallerId = Math.min(participant1Id, participant2Id);
        Long largerId = Math.max(participant1Id, participant2Id);
        
        if (productId != null) {
            return smallerId + "_" + largerId + "_" + productId;
        } else {
            return smallerId + "_" + largerId;
        }
    }
    
    /**
     * 更新会话信息（在发送消息后）
     */
    private void updateConversationAfterMessage(Conversation conversation, Message message, Long senderId) {
        try {
            // 更新最后消息信息
            conversationDao.updateLastMessage(conversation.getConversationId(),
                                            message.getContent(),
                                            message.getCreateTime());

            // 更新未读数量
            Long receiverId = senderId.equals(conversation.getParticipant1Id()) ?
                             conversation.getParticipant2Id() : conversation.getParticipant1Id();

            // 获取当前未读数量并加1
            int currentUnreadCount = 0;
            if (receiverId.equals(conversation.getParticipant1Id())) {
                currentUnreadCount = conversation.getUnreadCount1() + 1;
            } else {
                currentUnreadCount = conversation.getUnreadCount2() + 1;
            }

            conversationDao.updateUnreadCount(conversation.getConversationId(), receiverId, currentUnreadCount);

        } catch (Exception e) {
            logger.error("更新会话信息失败: conversationId={}", conversation.getConversationId(), e);
        }
    }
    
    /**
     * 转换Message为MessageVO
     */
    private MessageVO convertToMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setProductId(message.getProductId());
        vo.setContent(message.getContent());
        vo.setMessageType(message.getMessageType());
        vo.setIsRead(message.getIsRead());
        vo.setSendTime(message.getCreateTime());
        return vo;
    }
    
    /**
     * 转换Conversation为ConversationVO
     */
    private ConversationVO convertToConversationVO(Conversation conversation, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setParticipant1Id(conversation.getParticipant1Id());
        vo.setParticipant2Id(conversation.getParticipant2Id());
        vo.setProductId(conversation.getProductId());
        vo.setLastMessage(conversation.getLastMessage());
        vo.setLastMessageTime(conversation.getLastMessageTime());
        vo.setStatus(conversation.getStatus());
        
        // 设置当前用户的未读数量
        if (currentUserId.equals(conversation.getParticipant1Id())) {
            vo.setUnreadCount(conversation.getUnreadCount1());
        } else {
            vo.setUnreadCount(conversation.getUnreadCount2());
        }
        
        return vo;
    }
    
    /**
     * 验证状态值是否有效
     */
    private boolean isValidStatus(String status) {
        return "ACTIVE".equals(status) || "ARCHIVED".equals(status) || "BLOCKED".equals(status);
    }

    /**
     * Task4_3_1_2: 创建消息通知
     * 当用户收到新消息时，为接收者创建通知
     */
    private void createMessageNotification(Message message) {
        try {
            // 检查依赖服务是否可用（测试环境可能为null）
            if (notificationService == null || userDao == null) {
                logger.debug("通知服务或用户DAO不可用，跳过通知创建: messageId={}", message.getId());
                return;
            }

            // 获取发送者信息
            User sender = userDao.findPublicInfoById(message.getSenderId());
            if (sender == null) {
                logger.warn("创建消息通知失败: 发送者不存在, senderId={}", message.getSenderId());
                return;
            }

            // 创建通知
            Result<Long> result = notificationService.createMessageNotification(
                message.getReceiverId(),
                message.getSenderId(),
                sender.getNickname() != null ? sender.getNickname() : sender.getUsername(),
                message.getContent(),
                message.getConversationId()
            );

            if (result.isSuccess()) {
                logger.info("创建消息通知成功: messageId={}, notificationId={}, receiverId={}",
                           message.getId(), result.getData(), message.getReceiverId());
            } else {
                logger.warn("创建消息通知失败: messageId={}, receiverId={}, error={}",
                           message.getId(), message.getReceiverId(), result.getMessage());
            }

        } catch (Exception e) {
            // 通知创建失败不影响消息发送的主流程
            logger.error("创建消息通知时发生异常: messageId={}, receiverId={}, error={}",
                        message.getId(), message.getReceiverId(), e.getMessage(), e);
        }
    }
}
