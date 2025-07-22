package com.shiwu.admin.controller;

import com.shiwu.admin.dto.AuditLogQueryDTO;
import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.admin.service.impl.AdminServiceImpl;
import com.shiwu.admin.service.impl.AuditLogServiceImpl;
import com.shiwu.admin.vo.AuditLogVO;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 实现NFR-SEC-03要求的审计日志查看功能
 */
@Controller
@WebServlet("/api/admin/audit-logs/*")
public class AuditLogController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private AdminService adminService;

    public AuditLogController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.auditLogService = new AuditLogServiceImpl();
        this.adminService = new AdminServiceImpl();
        logger.info("AuditLogController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public AuditLogController(AuditLogService auditLogService, AdminService adminService) {
        this.auditLogService = auditLogService;
        this.adminService = adminService;
        logger.info("AuditLogController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 查询审计日志列表 - MVC版本
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Object> getAuditLogs(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                      @RequestParam(value = "size", defaultValue = "20") Integer size,
                                      @RequestParam(value = "action", required = false) String action,
                                      @RequestParam(value = "targetType", required = false) String targetType,
                                      @RequestParam(value = "adminId", required = false) Long adminId,
                                      @RequestParam(value = "startTime", required = false) String startTime,
                                      @RequestParam(value = "endTime", required = false) String endTime,
                                      HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看审计日志");
            }

            // 构建查询条件
            AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setPageSize(size);
            queryDTO.setAction(action);
            queryDTO.setTargetType(targetType);
            queryDTO.setAdminId(adminId);

            // 解析时间参数
            if (startTime != null && !startTime.trim().isEmpty()) {
                try {
                    queryDTO.setStartTime(LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
                } catch (DateTimeParseException e) {
                    return Result.fail("400", "开始时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }

            if (endTime != null && !endTime.trim().isEmpty()) {
                try {
                    queryDTO.setEndTime(LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
                } catch (DateTimeParseException e) {
                    return Result.fail("400", "结束时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }

            logger.debug("处理查询审计日志请求: adminId={}, page={}, size={}", currentAdminId, page, size);

            Map<String, Object> result = auditLogService.getAuditLogs(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询审计日志失败: {}", e.getMessage(), e);
            return Result.fail("500", "查询审计日志失败");
        }
    }

    /**
     * 获取审计日志详情 - MVC版本
     */
    @RequestMapping(value = "/{logId}", method = "GET")
    public Result<Object> getAuditLogDetail(@PathVariable("logId") Long logId, HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看审计日志");
            }

            if (logId == null || logId <= 0) {
                return Result.fail("400", "日志ID不能为空");
            }

            logger.debug("处理获取审计日志详情请求: adminId={}, logId={}", currentAdminId, logId);

            AuditLogVO log = auditLogService.getAuditLogDetail(logId);
            if (log == null) {
                return Result.fail("404", "审计日志不存在");
            }

            return Result.success(log);
        } catch (Exception e) {
            logger.error("获取审计日志详情失败: {}", e.getMessage(), e);
            return Result.fail("500", "获取审计日志详情失败");
        }
    }

    /**
     * 获取审计日志统计信息 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/statistics", method = "GET")
    public Result<Object> getAuditLogStatistics(@RequestParam(value = "days", defaultValue = "7") Integer days,
                                               HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看审计日志统计");
            }

            if (days == null || days <= 0 || days > 365) {
                return Result.fail("400", "天数参数错误，应在1-365之间");
            }

            logger.debug("处理获取审计日志统计请求: adminId={}, days={}", currentAdminId, days);

            Map<String, Object> statistics = auditLogService.getOperationStats(days);
            return Result.success(statistics);
        } catch (Exception e) {
            logger.error("获取审计日志统计失败: {}", e.getMessage(), e);
            return Result.fail("500", "获取审计日志统计失败");
        }
    }

    /**
     * 导出审计日志 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/export", method = "GET")
    public Result<Object> exportAuditLogs(@RequestParam(value = "format", defaultValue = "csv") String format,
                                         @RequestParam(value = "startTime", required = false) String startTime,
                                         @RequestParam(value = "endTime", required = false) String endTime,
                                         HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_EXPORT")) {
                return Result.fail("403", "权限不足，无法导出审计日志");
            }

            if (!"csv".equals(format) && !"excel".equals(format)) {
                return Result.fail("400", "导出格式错误，支持csv或excel");
            }

            logger.debug("处理导出审计日志请求: adminId={}, format={}", currentAdminId, format);

            // 构建查询条件
            AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();

            // 解析时间参数
            if (startTime != null && !startTime.trim().isEmpty()) {
                try {
                    queryDTO.setStartTime(LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
                } catch (DateTimeParseException e) {
                    return Result.fail("400", "开始时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }

            if (endTime != null && !endTime.trim().isEmpty()) {
                try {
                    queryDTO.setEndTime(LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
                } catch (DateTimeParseException e) {
                    return Result.fail("400", "结束时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }

            List<AuditLogVO> exportData = auditLogService.exportAuditLogs(queryDTO);

            Map<String, Object> result = new HashMap<>();
            result.put("exportId", "EXPORT_" + System.currentTimeMillis());
            result.put("format", format);
            result.put("data", exportData);
            result.put("count", exportData.size());

            return Result.success(result);
        } catch (Exception e) {
            logger.error("导出审计日志失败: {}", e.getMessage(), e);
            return Result.fail("500", "导出审计日志失败");
        }
    }

    /**
     * 获取活动趋势数据 - MVC版本（新增功能）
     * API: GET /api/admin/audit-logs/activity-trend
     */
    @RequestMapping(value = "/activity-trend", method = "GET")
    public Result<Object> getActivityTrend(@RequestParam(value = "days", defaultValue = "7") Integer days,
                                          HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看审计日志趋势");
            }

            if (days == null || days <= 0 || days > 365) {
                return Result.fail("400", "天数参数错误，应在1-365之间");
            }

            logger.debug("处理获取活动趋势请求: adminId={}, days={}", currentAdminId, days);

            List<Map<String, Object>> trendData = auditLogService.getActivityTrend(days);
            return Result.success(trendData);
        } catch (Exception e) {
            logger.error("获取活动趋势失败: {}", e.getMessage(), e);
            return Result.fail("500", "获取活动趋势失败");
        }
    }

    /**
     * 获取可用操作类型 - MVC版本（新增功能）
     * API: GET /api/admin/audit-logs/available-actions
     */
    @RequestMapping(value = "/available-actions", method = "GET")
    public Result<Object> getAvailableActions(HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看操作类型");
            }

            logger.debug("处理获取可用操作类型请求: adminId={}", currentAdminId);

            List<Map<String, String>> actions = auditLogService.getAvailableActions();
            return Result.success(actions);
        } catch (Exception e) {
            logger.error("获取可用操作类型失败: {}", e.getMessage(), e);
            return Result.fail("500", "获取可用操作类型失败");
        }
    }

    /**
     * 获取可用目标类型 - MVC版本（新增功能）
     * API: GET /api/admin/audit-logs/available-target-types
     */
    @RequestMapping(value = "/available-target-types", method = "GET")
    public Result<Object> getAvailableTargetTypes(HttpServletRequest request) {
        try {
            // 检查管理员权限
            Long currentAdminId = getCurrentAdminId(request);
            if (currentAdminId == null) {
                return Result.fail("401", "管理员未登录");
            }

            if (!adminService.hasPermission(currentAdminId, "AUDIT_LOG_VIEW")) {
                return Result.fail("403", "权限不足，无法查看目标类型");
            }

            logger.debug("处理获取可用目标类型请求: adminId={}", currentAdminId);

            List<Map<String, String>> targetTypes = auditLogService.getAvailableTargetTypes();
            return Result.success(targetTypes);
        } catch (Exception e) {
            logger.error("获取可用目标类型失败: {}", e.getMessage(), e);
            return Result.fail("500", "获取可用目标类型失败");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前管理员ID
     */
    protected Long getCurrentAdminId(HttpServletRequest request) {
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
}
