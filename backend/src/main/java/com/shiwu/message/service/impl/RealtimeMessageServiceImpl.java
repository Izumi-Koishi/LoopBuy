package com.shiwu.message.service.impl;

import com.shiwu.common.result.Result;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.message.dao.ConversationDao;
import com.shiwu.message.dao.MessageDao;
import com.shiwu.message.dto.MessagePollDTO;
import com.shiwu.message.model.Message;
import com.shiwu.message.service.MessageService;
import com.shiwu.message.service.RealtimeMessageService;
import com.shiwu.message.vo.MessagePollVO;
import com.shiwu.message.vo.MessageVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实时消息服务实现类 - MVC框架版本
 *
 * 基于轮询机制实现实时消息推送
 * 支持短轮询和长轮询两种模式
 *
 * 使用MVC框架的依赖注入，提高可测试性和解耦性
 * 继承BaseService获得通用功能支持
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Service
public class RealtimeMessageServiceImpl extends BaseService implements RealtimeMessageService {
    
    // 长轮询的默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    // 轮询间隔（毫秒）
    private static final long POLL_INTERVAL_MS = 1000;

    // 用户活跃超时时间（毫秒）- 5分钟
    private static final long ACTIVE_TIMEOUT = 5 * 60 * 1000;
    
    // 在线用户计数器
    private static final AtomicInteger onlineUserCount = new AtomicInteger(0);
    
    // 用户最后活跃时间记录
    private static final ConcurrentHashMap<Long, Long> userLastActiveTime = new ConcurrentHashMap<>();

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private MessageService messageService;

    public RealtimeMessageServiceImpl() {
        logger.info("RealtimeMessageServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数 - 支持测试和渐进式迁移
    public RealtimeMessageServiceImpl(MessageDao messageDao, ConversationDao conversationDao, MessageService messageService) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.messageService = messageService;
        logger.info("RealtimeMessageServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public Result<MessagePollVO> pollNewMessages(Long userId, MessagePollDTO pollDTO) {
        try {
            if (userId == null) {
                logger.warn("轮询新消息失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            // 更新用户活跃时间
            updateUserActiveTime(userId);
            
            // 设置默认参数
            if (pollDTO == null) {
                pollDTO = new MessagePollDTO();
            }
            
            Long lastMessageTime = pollDTO.getLastMessageTime();
            if (lastMessageTime == null || lastMessageTime <= 0) {
                lastMessageTime = System.currentTimeMillis() - 60000; // 默认获取最近1分钟的消息
            }
            
            // 获取新消息
            LocalDateTime lastTime = LocalDateTime.now().minusSeconds((System.currentTimeMillis() - lastMessageTime) / 1000);
            List<Message> newMessages = messageDao.findNewMessagesByUserId(userId, lastTime);
            
            // 转换为VO
            List<MessageVO> messageVOs = new ArrayList<>();
            for (Message message : newMessages) {
                MessageVO vo = convertToMessageVO(message);
                messageVOs.add(vo);
            }
            
            // 获取总未读数量
            int totalUnreadCount = conversationDao.getTotalUnreadCount(userId);
            
            // 构建响应
            MessagePollVO pollVO = new MessagePollVO();
            pollVO.setNewMessages(messageVOs);
            pollVO.setTotalUnreadCount(totalUnreadCount);
            pollVO.setCurrentTime(System.currentTimeMillis());
            
            logger.debug("轮询新消息成功: userId={}, newCount={}, totalUnread={}", 
                        userId, messageVOs.size(), totalUnreadCount);
            
            return Result.success(pollVO);
            
        } catch (Exception e) {
            logger.error("轮询新消息时发生异常: userId={}", userId, e);
            return Result.error("轮询新消息失败");
        }
    }
    
    @Override
    public Result<MessagePollVO> longPollNewMessages(Long userId, MessagePollDTO pollDTO, int timeoutSeconds) {
        try {
            if (userId == null) {
                logger.warn("长轮询新消息失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            if (timeoutSeconds <= 0 || timeoutSeconds > 60) {
                timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
            }
            
            // 更新用户活跃时间
            updateUserActiveTime(userId);
            
            long startTime = System.currentTimeMillis();
            long timeoutMs = timeoutSeconds * 1000L;
            
            // 长轮询循环
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // 检查是否有新消息
                Result<MessagePollVO> result = pollNewMessages(userId, pollDTO);
                if (result.isSuccess() && result.getData().getHasNewMessages()) {
                    logger.debug("长轮询找到新消息: userId={}, elapsed={}ms", 
                               userId, System.currentTimeMillis() - startTime);
                    return result;
                }
                
                // 等待一段时间后再次检查
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("长轮询被中断: userId={}", userId);
                    break;
                }
                
                // 更新用户活跃时间
                updateUserActiveTime(userId);
            }
            
            // 超时，返回空结果
            logger.debug("长轮询超时: userId={}, timeout={}s", userId, timeoutSeconds);
            return pollNewMessages(userId, pollDTO);
            
        } catch (Exception e) {
            logger.error("长轮询新消息时发生异常: userId={}", userId, e);
            return Result.error("长轮询新消息失败");
        }
    }
    
    @Override
    public Result<MessagePollVO> getUserRealtimeStatus(Long userId) {
        try {
            if (userId == null) {
                logger.warn("获取用户实时状态失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            // 更新用户活跃时间
            updateUserActiveTime(userId);
            
            // 获取总未读数量
            int totalUnreadCount = conversationDao.getTotalUnreadCount(userId);
            
            // 构建响应
            MessagePollVO statusVO = new MessagePollVO();
            statusVO.setNewMessages(new ArrayList<>());
            statusVO.setTotalUnreadCount(totalUnreadCount);
            statusVO.setCurrentTime(System.currentTimeMillis());
            statusVO.setHasNewMessages(false);
            
            logger.debug("获取用户实时状态成功: userId={}, totalUnread={}", userId, totalUnreadCount);
            return Result.success(statusVO);
            
        } catch (Exception e) {
            logger.error("获取用户实时状态时发生异常: userId={}", userId, e);
            return Result.error("获取用户实时状态失败");
        }
    }
    
    @Override
    public Result<Boolean> hasNewMessages(Long userId, Long lastCheckTime) {
        try {
            if (userId == null) {
                logger.warn("检查新消息失败: 用户ID为空");
                return Result.error("用户ID不能为空");
            }
            
            if (lastCheckTime == null || lastCheckTime <= 0) {
                lastCheckTime = System.currentTimeMillis() - 60000; // 默认检查最近1分钟
            }
            
            // 更新用户活跃时间
            updateUserActiveTime(userId);
            
            LocalDateTime lastTime = LocalDateTime.now().minusSeconds((System.currentTimeMillis() - lastCheckTime) / 1000);
            List<Message> newMessages = messageDao.findNewMessagesByUserId(userId, lastTime);
            
            boolean hasNew = !newMessages.isEmpty();
            
            logger.debug("检查新消息完成: userId={}, hasNew={}, count={}", 
                        userId, hasNew, newMessages.size());
            
            return Result.success(hasNew);
            
        } catch (Exception e) {
            logger.error("检查新消息时发生异常: userId={}", userId, e);
            return Result.error("检查新消息失败");
        }
    }
    
    @Override
    public void notifyNewMessage(Long userId, Long messageId) {
        try {
            if (userId == null || messageId == null) {
                logger.warn("通知新消息失败: 参数为空");
                return;
            }

            // 实现消息推送逻辑
            // 1. 检查用户是否在线
            if (isUserOnline(userId)) {
                // 2. 更新用户的最新消息时间戳，用于轮询检测
                updateUserLastMessageTime(userId, messageId);

                // 3. 记录推送事件（用于统计和调试）
                logger.info("推送新消息通知: userId={}, messageId={}, 用户在线", userId, messageId);

                // 4. 未来可以在这里集成WebSocket、SSE或其他实时推送技术
                // 例如：webSocketService.sendMessage(userId, messageData);
                // 例如：sseService.sendEvent(userId, "new-message", messageData);

            } else {
                logger.debug("用户不在线，跳过实时推送: userId={}, messageId={}", userId, messageId);
            }

        } catch (Exception e) {
            logger.error("通知新消息时发生异常: userId={}, messageId={}", userId, messageId, e);
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    private boolean isUserOnline(Long userId) {
        Long lastActiveTime = userLastActiveTime.get(userId);
        if (lastActiveTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActiveTime) <= ACTIVE_TIMEOUT;
    }

    /**
     * 更新用户的最新消息时间戳
     * @param userId 用户ID
     * @param messageId 消息ID
     */
    private void updateUserLastMessageTime(Long userId, Long messageId) {
        // 这里可以存储用户的最新消息ID，用于轮询时快速检测新消息
        // 当前实现中通过数据库查询实现，未来可以优化为内存缓存
        logger.debug("更新用户最新消息时间戳: userId={}, messageId={}", userId, messageId);
    }
    
    @Override
    public int getOnlineUserCount() {
        // 清理过期的用户活跃记录
        cleanupInactiveUsers();
        return onlineUserCount.get();
    }
    
    /**
     * 更新用户活跃时间
     */
    private void updateUserActiveTime(Long userId) {
        Long currentTime = System.currentTimeMillis();
        Long lastTime = userLastActiveTime.put(userId, currentTime);
        
        // 如果是新用户或者用户离线超过5分钟，增加在线用户数
        if (lastTime == null || currentTime - lastTime > 300000) {
            onlineUserCount.incrementAndGet();
            logger.debug("用户上线: userId={}, onlineCount={}", userId, onlineUserCount.get());
        }
    }
    
    /**
     * 清理不活跃的用户
     */
    private void cleanupInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        long inactiveThreshold = 300000; // 5分钟不活跃视为离线
        
        userLastActiveTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > inactiveThreshold) {
                onlineUserCount.decrementAndGet();
                logger.debug("用户离线: userId={}, onlineCount={}", entry.getKey(), onlineUserCount.get());
                return true;
            }
            return false;
        });
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
}
