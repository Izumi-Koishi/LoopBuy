package com.shiwu.admin.controller;

import com.shiwu.admin.model.*;
import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.impl.AdminServiceImpl;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器 - 重构为MVC框架
 * 使用注解驱动，大幅简化路由分发代码
 */
@Controller
@WebServlet("/api/admin/*")
public class AdminController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController() {
        this.adminService = new AdminServiceImpl();
        logger.info("AdminController初始化完成 - 使用MVC框架");
    }

    // 用于测试的构造函数，支持依赖注入
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
        logger.info("AdminController初始化完成 - 使用依赖注入");
    }

    /**
     * 健康检查接口
     * API: GET /api/admin/status
     */
    @RequestMapping(value = "/status", method = "GET")
    public Result<Map<String, Object>> healthCheck() {
        logger.info("管理员服务健康检查");

        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "LoopBuy Admin API");
        healthData.put("version", "1.0.0");
        healthData.put("timestamp", System.currentTimeMillis());
        healthData.put("message", "管理员API服务运行正常");

        return Result.success(healthData);
    }

    /**
     * 管理员登录接口
     * API: POST /api/admin/login
     */
    @RequestMapping(value = "/login", method = "POST")
    public Result<Object> login(HttpServletRequest request) {
        logger.info("管理员登录请求");

        try {
            // 解析请求体
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = parseRequestBody(request, Map.class);
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");

            // 参数校验
            if (username == null || username.trim().isEmpty()) {
                logger.warn("管理员登录失败: 用户名为空");
                return Result.fail("A0101", "用户名不能为空");
            }

            if (password == null || password.trim().isEmpty()) {
                logger.warn("管理员登录失败: 密码为空");
                return Result.fail("A0101", "密码不能为空");
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行登录
            AdminLoginResult loginResult = adminService.login(username.trim(), password.trim(), ipAddress, userAgent);

            if (loginResult.getSuccess()) {
                logger.info("管理员登录成功: username={}", username);
                return Result.success(loginResult.getData());
            } else {
                logger.warn("管理员登录失败: username={}, error={}", username, loginResult.getError().getMessage());
                return Result.fail(loginResult.getError().getCode(), loginResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("管理员登录异常", e);
            return Result.fail("A0199", "登录服务异常，请稍后重试");
        }
    }

    /**
     * 二次确认接口
     * API: POST /api/admin/confirm
     */
    @RequestMapping(value = "/confirm", method = "POST")
    public Result<Object> secondaryConfirmation(HttpServletRequest request) {
        logger.info("管理员二次确认请求");

        try {
            // 解析请求体
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = parseRequestBody(request, Map.class);
            String password = (String) requestMap.get("password");
            String operationCode = (String) requestMap.get("operationCode");

            // 参数校验
            if (password == null || password.trim().isEmpty()) {
                logger.warn("二次确认失败: 密码为空");
                return Result.fail("SC0102", "密码不能为空");
            }

            if (operationCode == null || operationCode.trim().isEmpty()) {
                logger.warn("二次确认失败: 操作代码为空");
                return Result.fail("SC0103", "操作代码不能为空");
            }

            // 获取管理员信息
            Long adminId = (Long) request.getAttribute("userId");
            if (adminId == null) {
                logger.warn("二次确认失败: 管理员信息不存在");
                return Result.fail("SC0202", "管理员信息不存在，请重新登录");
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行二次确认
            SecondaryConfirmationResult confirmResult = adminService.verifySecondaryConfirmation(
                adminId, password.trim(), operationCode.trim(), ipAddress, userAgent
            );

            if (confirmResult.getSuccess()) {
                // 生成确认令牌
                String confirmationToken = "confirm_" + adminId + "_" + operationCode + "_" + System.currentTimeMillis();

                Map<String, Object> data = new HashMap<>();
                data.put("confirmationToken", confirmationToken);
                data.put("operationCode", operationCode);
                data.put("operationDescription", confirmResult.getData());

                logger.info("管理员二次确认成功: adminId={}, operationCode={}", adminId, operationCode);
                return Result.success(data);
            } else {
                logger.warn("管理员二次确认失败: adminId={}, operationCode={}, error={}",
                    adminId, operationCode, confirmResult.getError().getMessage());
                return Result.fail(confirmResult.getError().getCode(), confirmResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("管理员二次确认异常", e);
            return Result.fail("SC0199", "二次确认服务异常，请稍后重试");
        }
    }





    /**
     * 检查管理员权限 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/permission", method = "GET")
    public Result<Object> checkPermission(@RequestParam(value = "role", required = false) String requiredRole,
                                         HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            if (adminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            boolean hasPermission = adminService.hasPermission(adminId, requiredRole);

            Map<String, Object> result = new HashMap<>();
            result.put("hasPermission", hasPermission);
            result.put("adminId", adminId);
            result.put("requiredRole", requiredRole);

            logger.debug("检查管理员权限: adminId={}, requiredRole={}, hasPermission={}",
                        adminId, requiredRole, hasPermission);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("检查管理员权限失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 检查是否为超级管理员 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/super-admin", method = "GET")
    public Result<Object> checkSuperAdmin(HttpServletRequest request) {
        try {
            Long adminId = getCurrentAdminId(request);
            if (adminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            boolean isSuperAdmin = adminService.isSuperAdmin(adminId);

            Map<String, Object> result = new HashMap<>();
            result.put("isSuperAdmin", isSuperAdmin);
            result.put("adminId", adminId);

            logger.debug("检查是否为超级管理员: adminId={}, isSuperAdmin={}", adminId, isSuperAdmin);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("检查是否为超级管理员失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 创建操作上下文 - MVC版本（新增功能）
     * API: POST /api/admin/operation-context
     */
    @RequestMapping(value = "/operation-context", method = "POST")
    public Result<Object> createOperationContext(HttpServletRequest request) {
        logger.info("创建操作上下文请求");

        try {
            Long adminId = getCurrentAdminId(request);
            if (adminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            // 解析请求体
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = parseRequestBody(request, Map.class);
            if (requestMap == null || !requestMap.containsKey("operationCode")) {
                return Result.fail("400", "操作代码不能为空");
            }

            String operationCode = (String) requestMap.get("operationCode");
            Object operationData = requestMap.get("operationData");
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            String operationId = adminService.createOperationContext(adminId, operationCode, operationData, ipAddress, userAgent);

            Map<String, Object> result = new HashMap<>();
            result.put("operationId", operationId);
            result.put("operationCode", operationCode);

            logger.info("创建操作上下文成功: adminId={}, operationCode={}, operationId={}",
                        adminId, operationCode, operationId);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("创建操作上下文失败", e);
            return Result.fail("500", "创建操作上下文失败: " + e.getMessage());
        }
    }

    /**
     * 获取操作上下文 - MVC版本（新增功能）
     * API: GET /api/admin/operation-context/{operationId}
     */
    @RequestMapping(value = "/operation-context/{operationId}", method = "GET")
    public Result<Object> getOperationContext(@PathVariable("operationId") String operationId, HttpServletRequest request) {
        logger.info("获取操作上下文请求: operationId={}", operationId);

        try {
            Long adminId = getCurrentAdminId(request);
            if (adminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (operationId == null || operationId.trim().isEmpty()) {
                return Result.fail("400", "操作ID不能为空");
            }

            OperationContext context = adminService.getOperationContext(operationId);
            if (context == null) {
                return Result.fail("404", "操作上下文不存在");
            }

            logger.info("获取操作上下文成功: adminId={}, operationId={}", adminId, operationId);

            return Result.success(context);
        } catch (Exception e) {
            logger.error("获取操作上下文失败: operationId={}", operationId, e);
            return Result.fail("500", "获取操作上下文失败: " + e.getMessage());
        }
    }

    /**
     * 检查操作是否需要二次确认 - MVC版本（新增功能）
     * API: GET /api/admin/requires-confirmation
     */
    @RequestMapping(value = "/requires-confirmation", method = "GET")
    public Result<Object> checkRequiresConfirmation(@RequestParam("operationCode") String operationCode,
                                                   HttpServletRequest request) {
        logger.info("检查操作是否需要二次确认请求: operationCode={}", operationCode);

        try {
            Long adminId = getCurrentAdminId(request);
            if (adminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (operationCode == null || operationCode.trim().isEmpty()) {
                return Result.fail("400", "操作代码不能为空");
            }

            // 这里需要获取管理员角色，暂时使用默认值
            String adminRole = "admin"; // 可以从数据库获取实际角色
            boolean requiresConfirmation = adminService.requiresSecondaryConfirmation(operationCode, adminRole);

            Map<String, Object> result = new HashMap<>();
            result.put("requiresConfirmation", requiresConfirmation);
            result.put("operationCode", operationCode);
            result.put("adminRole", adminRole);

            logger.info("检查操作是否需要二次确认成功: adminId={}, operationCode={}, requiresConfirmation={}",
                        adminId, operationCode, requiresConfirmation);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("检查操作是否需要二次确认失败: operationCode={}", operationCode, e);
            return Result.fail("500", "检查操作是否需要二次确认失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前管理员ID
     */
    private Long getCurrentAdminId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (JwtUtil.validateToken(token)) {
                    return JwtUtil.getUserIdFromToken(token);
                }
            }
        } catch (Exception e) {
            logger.warn("获取管理员ID失败: {}", e.getMessage());
        }
        return null;
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
