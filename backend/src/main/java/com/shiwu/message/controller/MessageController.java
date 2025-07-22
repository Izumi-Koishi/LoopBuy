package com.shiwu.message.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.message.dto.MessageSendDTO;
import com.shiwu.message.service.MessageService;
import com.shiwu.message.vo.ConversationVO;
import com.shiwu.message.vo.MessageVO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息控制器 - MVC框架版本
 *
 * 处理实时消息收发相关的HTTP请求
 * 支持基于轮询的实时消息推送
 *
 * 使用MVC框架的注解驱动方式，大幅简化代码
 * 继承BaseController获得路由分发和统一处理功能
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Controller
@WebServlet("/api/message/*")
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    public MessageController() {
        // 使用BaseController的logger
        logger.info("MessageController初始化完成 - 使用MVC框架依赖注入");
    }

    /**
     * 获取会话列表 - 主路径
     */
    @RequestMapping(value = "/api/message/conversations", method = "GET")
    public Result<List<ConversationVO>> getConversations(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            HttpServletRequest request) {

        logger.debug("获取会话列表: page={}, size={}", page, size);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.getConversations(userId, page, size);
    }

    /**
     * 获取会话列表 - 兼容性路径 (原来的 /api/message/)
     */
    @RequestMapping(value = "/api/message/", method = "GET")
    public Result<List<ConversationVO>> getConversationsCompat(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            HttpServletRequest request) {

        return getConversations(page, size, request);
    }

    /**
     * 获取消息历史
     */
    @RequestMapping(value = "/api/message/history/{conversationId}", method = "GET")
    public Result<List<MessageVO>> getMessageHistory(
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size,
            HttpServletRequest request) {

        logger.debug("获取消息历史: conversationId={}, page={}, size={}", conversationId, page, size);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.getMessageHistory(userId, conversationId, page, size);
    }

    /**
     * 获取新消息（轮询接口）
     */
    @RequestMapping(value = "/api/message/new", method = "GET")
    public Result<List<MessageVO>> getNewMessages(
            @RequestParam(value = "lastMessageTime", defaultValue = "0") Long lastMessageTime,
            HttpServletRequest request) {

        logger.debug("获取新消息: lastMessageTime={}", lastMessageTime);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.getNewMessages(userId, lastMessageTime);
    }

    /**
     * 获取未读消息数量
     */
    @RequestMapping(value = "/api/message/unread-count", method = "GET")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {

        logger.debug("获取未读消息数量");

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.getUnreadMessageCount(userId);
    }

    /**
     * 获取会话详情
     */
    @RequestMapping(value = "/api/message/conversation/{conversationId}", method = "GET")
    public Result<ConversationVO> getConversationDetail(
            @PathVariable("conversationId") String conversationId,
            HttpServletRequest request) {

        logger.debug("获取会话详情: conversationId={}", conversationId);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.getConversationDetail(userId, conversationId);
    }

    /**
     * 发送消息 - 主路径
     */
    @RequestMapping(value = "/api/message/send", method = "POST")
    public Result<MessageVO> sendMessage(HttpServletRequest request) {

        logger.debug("发送消息");

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        try {
            // 解析请求体
            MessageSendDTO dto = parseRequestBody(request, MessageSendDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求体不能为空或格式错误");
            }

            return messageService.sendMessage(userId, dto);

        } catch (Exception e) {
            logger.error("发送消息时解析请求失败: userId={}", userId, e);
            return Result.fail("400", "请求格式错误");
        }
    }

    /**
     * 发送消息 - 兼容性路径 (原来的 /api/message/)
     */
    @RequestMapping(value = "/api/message/", method = "POST")
    public Result<MessageVO> sendMessageCompat(HttpServletRequest request) {
        return sendMessage(request);
    }

    /**
     * 创建会话
     */
    @RequestMapping(value = "/api/message/conversation", method = "POST")
    public Result<ConversationVO> createConversation(
            @RequestParam("otherUserId") Long otherUserId,
            @RequestParam(value = "productId", required = false) Long productId,
            HttpServletRequest request) {

        logger.debug("创建会话: otherUserId={}, productId={}", otherUserId, productId);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        if (otherUserId == null) {
            return Result.fail("400", "对方用户ID不能为空");
        }

        return messageService.getOrCreateConversation(userId, otherUserId, productId);
    }

    /**
     * 标记消息为已读
     */
    @RequestMapping(value = "/api/message/read/{conversationId}", method = "PUT")
    public Result<Void> markAsRead(
            @PathVariable("conversationId") String conversationId,
            HttpServletRequest request) {

        logger.debug("标记消息为已读: conversationId={}", conversationId);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return messageService.markMessagesAsRead(userId, conversationId);
    }

    /**
     * 更新会话状态
     */
    @RequestMapping(value = "/api/message/conversation/{conversationId}", method = "PUT")
    public Result<Void> updateConversationStatus(
            @PathVariable("conversationId") String conversationId,
            @RequestParam("status") String status,
            HttpServletRequest request) {

        logger.debug("更新会话状态: conversationId={}, status={}", conversationId, status);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        if (status == null || status.trim().isEmpty()) {
            return Result.fail("400", "状态不能为空");
        }

        return messageService.updateConversationStatus(userId, conversationId, status);
    }

    /**
     * 从JWT Token中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        try {
            return JwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            logger.warn("解析JWT Token失败: {}", e.getMessage());
            return null;
        }
    }

}
