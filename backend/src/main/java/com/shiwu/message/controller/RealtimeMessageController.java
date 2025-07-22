package com.shiwu.message.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.message.dto.MessagePollDTO;
import com.shiwu.message.service.RealtimeMessageService;
import com.shiwu.message.vo.MessagePollVO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * 实时消息控制器 - MVC框架版本
 *
 * 处理基于轮询的实时消息推送请求
 * 支持短轮询和长轮询两种模式
 *
 * 使用MVC框架的注解驱动方式，大幅简化代码
 * 继承BaseController获得路由分发和统一处理功能
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Controller
@WebServlet("/api/realtime/*")
public class RealtimeMessageController extends BaseController {

    @Autowired
    private RealtimeMessageService realtimeMessageService;

    public RealtimeMessageController() {
        // 使用BaseController的logger
        logger.info("RealtimeMessageController初始化完成 - 使用MVC框架依赖注入");
    }

    /**
     * 轮询消息接口 - 主路径
     */
    @RequestMapping(value = "/api/realtime/poll", method = "GET")
    public Result<MessagePollVO> pollMessages(
            @RequestParam(value = "lastMessageTime", defaultValue = "0") Long lastMessageTime,
            @RequestParam(value = "timeout", defaultValue = "30") Integer timeout,
            HttpServletRequest request) {

        logger.debug("轮询消息: lastMessageTime={}, timeout={}", lastMessageTime, timeout);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        // 创建轮询DTO
        MessagePollDTO pollDTO = new MessagePollDTO();
        pollDTO.setLastMessageTime(lastMessageTime);

        return realtimeMessageService.pollNewMessages(userId, pollDTO);
    }

    /**
     * 轮询消息接口 - 兼容性路径 (原来的 /api/realtime/)
     */
    @RequestMapping(value = "/api/realtime/", method = "GET")
    public Result<MessagePollVO> pollMessagesCompat(
            @RequestParam(value = "lastMessageTime", defaultValue = "0") Long lastMessageTime,
            @RequestParam(value = "timeout", defaultValue = "30") Integer timeout,
            HttpServletRequest request) {

        return pollMessages(lastMessageTime, timeout, request);
    }

    /**
     * 长轮询消息接口
     */
    @RequestMapping(value = "/api/realtime/long-poll", method = "GET")
    public Result<MessagePollVO> longPollMessages(
            @RequestParam(value = "lastMessageTime", defaultValue = "0") Long lastMessageTime,
            @RequestParam(value = "timeout", defaultValue = "60") Integer timeout,
            HttpServletRequest request) {

        logger.debug("长轮询消息: lastMessageTime={}, timeout={}", lastMessageTime, timeout);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        // 创建轮询DTO
        MessagePollDTO pollDTO = new MessagePollDTO();
        pollDTO.setLastMessageTime(lastMessageTime);

        return realtimeMessageService.longPollNewMessages(userId, pollDTO, timeout);
    }

    /**
     * 获取实时状态
     */
    @RequestMapping(value = "/api/realtime/status", method = "GET")
    public Result<MessagePollVO> getRealtimeStatus(HttpServletRequest request) {

        logger.debug("获取实时状态");

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return realtimeMessageService.getUserRealtimeStatus(userId);
    }

    /**
     * 检查新消息
     */
    @RequestMapping(value = "/api/realtime/check", method = "GET")
    public Result<Boolean> checkNewMessages(
            @RequestParam(value = "lastCheckTime", defaultValue = "0") Long lastCheckTime,
            HttpServletRequest request) {

        logger.debug("检查新消息: lastCheckTime={}", lastCheckTime);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        return realtimeMessageService.hasNewMessages(userId, lastCheckTime);
    }

    /**
     * 获取在线用户数量
     */
    @RequestMapping(value = "/api/realtime/online-count", method = "GET")
    public Result<Integer> getOnlineCount() {

        logger.debug("获取在线用户数量");

        int count = realtimeMessageService.getOnlineUserCount();
        return Result.success(count);
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

    /**
     * 轮询消息接口 - POST方法支持
     */
    @RequestMapping(value = "/api/realtime/poll", method = "POST")
    public Result<MessagePollVO> pollMessagesPost(HttpServletRequest request) {

        logger.debug("POST轮询消息");

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        // 创建默认轮询DTO
        MessagePollDTO pollDTO = new MessagePollDTO();
        pollDTO.setLastMessageTime(0L);

        return realtimeMessageService.pollNewMessages(userId, pollDTO);
    }

    /**
     * 长轮询消息接口 - POST方法支持
     */
    @RequestMapping(value = "/api/realtime/long-poll", method = "POST")
    public Result<MessagePollVO> longPollMessagesPost(
            @RequestParam(value = "timeout", defaultValue = "60") Integer timeout,
            HttpServletRequest request) {

        logger.debug("POST长轮询消息: timeout={}", timeout);

        // 验证用户身份
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "未授权访问");
        }

        // 创建默认轮询DTO
        MessagePollDTO pollDTO = new MessagePollDTO();
        pollDTO.setLastMessageTime(0L);

        return realtimeMessageService.longPollNewMessages(userId, pollDTO, timeout);
    }
}
