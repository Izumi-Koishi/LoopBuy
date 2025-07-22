package com.shiwu.notification.controller;

import com.shiwu.common.result.Result;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.notification.service.NotificationService;
import com.shiwu.notification.vo.NotificationVO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知控制器 - MVC框架版本
 *
 * 用于Task4_2_1_2: 商品审核通过粉丝通知功能
 * 用于Task4_3_1_3: 获取通知列表和未读计数的API
 *
 * 提供完整的通知相关HTTP API接口：
 * - GET /api/notification/list - 获取通知列表
 * - GET /api/notification/unread-count - 获取未读通知数量
 * - PUT /api/notification/mark-read - 标记单个通知已读
 * - PUT /api/notification/mark-all-read - 批量标记通知已读
 *
 * 使用MVC框架的注解驱动方式，大幅简化代码
 * 继承BaseController获得路由分发和统一处理功能
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 * @since 2024-01-15
 */
@Controller
@WebServlet("/api/notification/*")
public class NotificationController extends BaseController {
    
    @Autowired
    private NotificationService notificationService;
    
    // 用于测试的构造函数
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * 获取通知列表
     * API: GET /api/notification/list
     */
    @RequestMapping(value = "/api/notification/list", method = "GET")
    public Result<List<NotificationVO>> getNotificationList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "onlyUnread", defaultValue = "false") Boolean onlyUnread,
            HttpServletRequest request) {
        
        logger.debug("获取通知列表: page={}, size={}, onlyUnread={}", page, size, onlyUnread);
        
        // 获取当前登录用户ID（从JWT token中解析）
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "请先登录");
        }
        
        // 参数验证
        if (page < 1) {
            return Result.fail("400", "页码必须大于0");
        }
        if (size < 1 || size > 100) {
            return Result.fail("400", "每页大小必须在1-100之间");
        }
        
        return notificationService.getUserNotifications(userId, page, size, onlyUnread);
    }
    
    /**
     * 获取未读通知数量
     * API: GET /api/notification/unread-count
     */
    @RequestMapping(value = "/api/notification/unread-count", method = "GET")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        
        logger.debug("获取未读通知数量");
        
        // 获取当前登录用户ID
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "请先登录");
        }
        
        return notificationService.getUnreadNotificationCount(userId);
    }
    
    /**
     * 标记通知为已读
     * API: PUT /api/notification/mark-read
     */
    @RequestMapping(value = "/api/notification/mark-read", method = "PUT")
    public Result<Void> markAsRead(
            @RequestParam("id") Long notificationId,
            HttpServletRequest request) {
        
        logger.debug("标记通知为已读: notificationId={}", notificationId);
        
        // 获取当前登录用户ID
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "请先登录");
        }
        
        // 参数验证
        if (notificationId == null) {
            return Result.fail("400", "通知ID不能为空");
        }
        
        return notificationService.markNotificationAsRead(notificationId, userId);
    }
    
    /**
     * 批量标记通知为已读
     * API: PUT /api/notification/mark-all-read
     */
    @RequestMapping(value = "/api/notification/mark-all-read", method = "PUT")
    public Result<Integer> markAllAsRead(
            @RequestParam(value = "ids", required = false) String ids,
            HttpServletRequest request) {
        
        logger.debug("批量标记通知为已读: ids={}", ids);
        
        // 获取当前登录用户ID
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "请先登录");
        }
        
        List<Long> notificationIds = null;
        
        // 如果提供了ids参数，解析为Long列表
        if (ids != null && !ids.trim().isEmpty()) {
            try {
                notificationIds = Arrays.stream(ids.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                
                if (notificationIds.isEmpty()) {
                    return Result.fail("400", "通知ID列表不能为空");
                }
            } catch (NumberFormatException e) {
                logger.warn("解析通知ID列表失败: ids={}, error={}", ids, e.getMessage());
                return Result.fail("400", "通知ID格式错误");
            }
        }
        
        return notificationService.batchMarkNotificationsAsRead(userId, notificationIds);
    }
    
    /**
     * 标记单个通知为已读 - 路径参数版本
     * API: PUT /api/notification/{id}/read
     */
    @RequestMapping(value = "/api/notification/{id}/read", method = "PUT")
    public Result<Void> markAsReadByPath(
            @PathVariable("id") Long notificationId,
            HttpServletRequest request) {
        
        logger.debug("标记通知为已读(路径参数): notificationId={}", notificationId);
        
        // 获取当前登录用户ID
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.fail("401", "请先登录");
        }
        
        return notificationService.markNotificationAsRead(notificationId, userId);
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
            return com.shiwu.common.util.JwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            logger.warn("解析JWT Token失败: {}", e.getMessage());
            return null;
        }
    }
}
