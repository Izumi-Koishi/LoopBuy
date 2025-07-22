package com.shiwu.admin.controller;

import com.shiwu.admin.model.AdminUserQueryDTO;
import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.impl.AdminServiceImpl;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.user.service.AdminUserService;
import com.shiwu.user.service.impl.AdminUserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器 - 重构为MVC框架
 * 使用注解驱动，大幅简化路由分发代码
 */
@Controller
@WebServlet("/api/admin/users/*")
public class AdminUserController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;
    private final AdminService adminService;

    public AdminUserController() {
        this.adminUserService = new AdminUserServiceImpl();
        this.adminService = new AdminServiceImpl();
        logger.info("AdminUserController初始化完成 - 使用MVC框架");
    }

    // 用于测试的构造函数，支持依赖注入
    public AdminUserController(AdminUserService adminUserService, AdminService adminService) {
        this.adminUserService = adminUserService;
        this.adminService = adminService;
        logger.info("AdminUserController初始化完成 - 使用依赖注入");
    }

    /**
     * 查询用户列表
     * API: GET /api/admin/users/
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Map<String, Object>> getUsers(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                               @RequestParam(value = "size", defaultValue = "10") Integer size,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "status", required = false) String status,
                                               HttpServletRequest request) {
        logger.info("查询用户列表请求: page={}, size={}, keyword={}, status={}", page, size, keyword, status);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 构建查询条件
            AdminUserQueryDTO queryDTO = new AdminUserQueryDTO();
            queryDTO.setPageNum(page);
            queryDTO.setPageSize(size);
            queryDTO.setKeyword(keyword);
            if (status != null && !status.trim().isEmpty()) {
                try {
                    queryDTO.setStatus(Integer.parseInt(status));
                } catch (NumberFormatException e) {
                    logger.warn("无效的状态参数: {}", status);
                }
            }

            // 查询用户列表
            Map<String, Object> result = adminUserService.findUsers(queryDTO);

            logger.info("查询用户列表成功: page={}, size={}, total={}", page, size, result.get("total"));
            return Result.success(result);

        } catch (Exception e) {
            logger.error("查询用户列表失败", e);
            return Result.fail("USER001", "查询用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     * API: GET /api/admin/users/{userId}
     */
    @RequestMapping(value = "/{userId}", method = "GET")
    public Result<Object> getUserDetail(@PathVariable("userId") Long userId,
                                       HttpServletRequest request) {
        logger.info("获取用户详情请求: userId={}", userId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 获取用户详情
            Map<String, Object> userDetail = adminUserService.getUserDetail(userId, adminId);
            if (userDetail == null) {
                logger.warn("用户不存在: userId={}", userId);
                return Result.fail("USER002", "用户不存在");
            }

            logger.info("获取用户详情成功: userId={}", userId);
            return Result.success(userDetail);

        } catch (Exception e) {
            logger.error("获取用户详情失败: userId={}", userId, e);
            return Result.fail("USER003", "获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 封禁用户 - MVC版本（新增功能）
     * API: POST /api/admin/users/{userId}/ban
     */
    @RequestMapping(value = "/{userId}/ban", method = "POST")
    public Result<Object> banUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.info("封禁用户请求: userId={}", userId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体获取封禁原因
            String reason = "管理员封禁";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = parseRequestBody(request, Map.class);
                if (requestMap != null && requestMap.containsKey("reason")) {
                    reason = (String) requestMap.get("reason");
                }
            } catch (Exception e) {
                logger.warn("解析封禁原因失败，使用默认原因");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行封禁操作
            boolean success = adminUserService.banUser(userId, adminId, reason, ipAddress, userAgent);
            if (success) {
                logger.info("封禁用户成功: userId={}, adminId={}", userId, adminId);
                return Result.success("用户封禁成功");
            } else {
                logger.warn("封禁用户失败: userId={}, adminId={}", userId, adminId);
                return Result.fail("USER004", "封禁用户失败");
            }

        } catch (Exception e) {
            logger.error("封禁用户异常: userId={}", userId, e);
            return Result.fail("USER005", "封禁用户异常: " + e.getMessage());
        }
    }

    /**
     * 禁言用户 - MVC版本（新增功能）
     * API: POST /api/admin/users/{userId}/mute
     */
    @RequestMapping(value = "/{userId}/mute", method = "POST")
    public Result<Object> muteUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.info("禁言用户请求: userId={}", userId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体获取禁言原因
            String reason = "管理员禁言";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = parseRequestBody(request, Map.class);
                if (requestMap != null && requestMap.containsKey("reason")) {
                    reason = (String) requestMap.get("reason");
                }
            } catch (Exception e) {
                logger.warn("解析禁言原因失败，使用默认原因");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行禁言操作
            boolean success = adminUserService.muteUser(userId, adminId, reason, ipAddress, userAgent);
            if (success) {
                logger.info("禁言用户成功: userId={}, adminId={}", userId, adminId);
                return Result.success("用户禁言成功");
            } else {
                logger.warn("禁言用户失败: userId={}, adminId={}", userId, adminId);
                return Result.fail("USER006", "禁言用户失败");
            }

        } catch (Exception e) {
            logger.error("禁言用户异常: userId={}", userId, e);
            return Result.fail("USER007", "禁言用户异常: " + e.getMessage());
        }
    }

    /**
     * 解封用户 - MVC版本（新增功能）
     * API: POST /api/admin/users/{userId}/unban
     */
    @RequestMapping(value = "/{userId}/unban", method = "POST")
    public Result<Object> unbanUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.info("解封用户请求: userId={}", userId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行解封操作
            boolean success = adminUserService.unbanUser(userId, adminId, ipAddress, userAgent);
            if (success) {
                logger.info("解封用户成功: userId={}, adminId={}", userId, adminId);
                return Result.success("用户解封成功");
            } else {
                logger.warn("解封用户失败: userId={}, adminId={}", userId, adminId);
                return Result.fail("USER008", "解封用户失败");
            }

        } catch (Exception e) {
            logger.error("解封用户异常: userId={}", userId, e);
            return Result.fail("USER009", "解封用户异常: " + e.getMessage());
        }
    }

    /**
     * 解除禁言 - MVC版本（新增功能）
     * API: POST /api/admin/users/{userId}/unmute
     */
    @RequestMapping(value = "/{userId}/unmute", method = "POST")
    public Result<Object> unmuteUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.info("解除禁言请求: userId={}", userId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行解除禁言操作
            boolean success = adminUserService.unmuteUser(userId, adminId, ipAddress, userAgent);
            if (success) {
                logger.info("解除禁言成功: userId={}, adminId={}", userId, adminId);
                return Result.success("用户解除禁言成功");
            } else {
                logger.warn("解除禁言失败: userId={}, adminId={}", userId, adminId);
                return Result.fail("USER010", "解除禁言失败");
            }

        } catch (Exception e) {
            logger.error("解除禁言异常: userId={}", userId, e);
            return Result.fail("USER011", "解除禁言异常: " + e.getMessage());
        }
    }

    /**
     * 批量封禁用户 - MVC版本（新增功能）
     * API: POST /api/admin/users/batch/ban
     */
    @RequestMapping(value = "/batch/ban", method = "POST")
    public Result<Object> batchBanUsers(HttpServletRequest request) {
        logger.info("批量封禁用户请求");

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = parseRequestBody(request, Map.class);
            if (requestMap == null || !requestMap.containsKey("userIds")) {
                return Result.fail("USER012", "用户ID列表不能为空");
            }

            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) requestMap.get("userIds");
            String reason = (String) requestMap.getOrDefault("reason", "管理员批量封禁");

            if (userIds == null || userIds.isEmpty()) {
                return Result.fail("USER013", "用户ID列表不能为空");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行批量封禁操作
            Map<String, Object> result = adminUserService.batchBanUsers(userIds, adminId, reason, ipAddress, userAgent);

            logger.info("批量封禁用户完成: adminId={}, userCount={}", adminId, userIds.size());
            return Result.success(result);

        } catch (Exception e) {
            logger.error("批量封禁用户异常", e);
            return Result.fail("USER014", "批量封禁用户异常: " + e.getMessage());
        }
    }

    /**
     * 批量禁言用户 - MVC版本（新增功能）
     * API: POST /api/admin/users/batch/mute
     */
    @RequestMapping(value = "/batch/mute", method = "POST")
    public Result<Object> batchMuteUsers(HttpServletRequest request) {
        logger.info("批量禁言用户请求");

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = parseRequestBody(request, Map.class);
            if (requestMap == null || !requestMap.containsKey("userIds")) {
                return Result.fail("USER015", "用户ID列表不能为空");
            }

            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) requestMap.get("userIds");
            String reason = (String) requestMap.getOrDefault("reason", "管理员批量禁言");

            if (userIds == null || userIds.isEmpty()) {
                return Result.fail("USER016", "用户ID列表不能为空");
            }

            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行批量禁言操作
            Map<String, Object> result = adminUserService.batchMuteUsers(userIds, adminId, reason, ipAddress, userAgent);

            logger.info("批量禁言用户完成: adminId={}, userCount={}", adminId, userIds.size());
            return Result.success(result);

        } catch (Exception e) {
            logger.error("批量禁言用户异常", e);
            return Result.fail("USER017", "批量禁言用户异常: " + e.getMessage());
        }
    }

    /**
     * 验证管理员权限
     */
    private Long validateAdminPermission(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!JwtUtil.validateToken(token)) {
                logger.warn("Token验证失败");
                return null;
            }

            Long adminId = JwtUtil.getUserIdFromToken(token);
            if (adminId == null) {
                logger.warn("无效的令牌");
                return null;
            }

            if (!adminService.hasPermission(adminId, "ADMIN")) {
                logger.warn("权限不足，管理员ID: {}", adminId);
                return null;
            }

            return adminId;
        } catch (Exception e) {
            logger.error("权限验证异常", e);
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}