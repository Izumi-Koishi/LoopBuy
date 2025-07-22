package com.shiwu.admin.controller;

import com.shiwu.admin.model.DashboardStatsVO;
import com.shiwu.admin.model.StatsPeriod;
import com.shiwu.admin.service.DashboardService;
import com.shiwu.admin.service.impl.DashboardServiceImpl;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * 仪表盘控制器 - 重构为MVC框架
 * 使用注解驱动，大幅简化路由分发代码
 * 根据项目规范UC-15实现
 */
@Controller
@WebServlet("/admin/dashboard/*")
public class DashboardController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController() {
        this.dashboardService = new DashboardServiceImpl();
        logger.info("DashboardController初始化完成 - 使用MVC框架");
    }

    // 用于测试的构造函数，支持依赖注入
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        logger.info("DashboardController初始化完成 - 使用依赖注入");
    }
    
    /**
     * 获取仪表盘统计数据
     * API: GET /admin/dashboard/stats
     */
    @RequestMapping(value = "/stats", method = "GET")
    public Result<DashboardStatsVO> getStats(@RequestParam(value = "period", required = false) String period,
                                           HttpServletRequest request) {
        logger.info("获取仪表盘统计数据请求: period={}", period);

        try {
            // 验证管理员权限
            if (!validateAdminPermission(request)) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析统计周期
            StatsPeriod statsPeriod = parseStatsPeriod(period);

            // 获取统计数据
            DashboardStatsVO stats;
            if (statsPeriod != null) {
                stats = dashboardService.getDashboardStats(statsPeriod);
            } else {
                stats = dashboardService.getDashboardStats();
            }

            logger.info("获取仪表盘统计数据成功: period={}", period);
            return Result.success(stats);

        } catch (Exception e) {
            logger.error("获取仪表盘统计数据失败", e);
            return Result.fail("STATS001", "获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时统计数据
     * API: GET /admin/dashboard/stats/realtime
     */
    @RequestMapping(value = "/stats/realtime", method = "GET")
    public Result<DashboardStatsVO> getRealTimeStats(HttpServletRequest request) {
        logger.info("获取实时统计数据请求");

        try {
            // 验证管理员权限
            if (!validateAdminPermission(request)) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 获取实时统计数据
            DashboardStatsVO stats = dashboardService.getRealTimeStats();

            logger.info("获取实时统计数据成功");
            return Result.success(stats);

        } catch (Exception e) {
            logger.error("获取实时统计数据失败", e);
            return Result.fail("STATS002", "获取实时统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新统计数据缓存
     * API: POST /admin/dashboard/stats/refresh
     */
    @RequestMapping(value = "/stats/refresh", method = "POST")
    public Result<DashboardStatsVO> refreshStats(HttpServletRequest request) {
        logger.info("刷新统计数据缓存请求");

        try {
            // 验证管理员权限
            if (!validateAdminPermission(request)) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 刷新缓存
            boolean refreshSuccess = dashboardService.refreshStatsCache();
            if (!refreshSuccess) {
                logger.warn("刷新统计数据缓存失败");
                return Result.fail("CACHE001", "刷新缓存失败");
            }

            // 获取刷新后的统计数据
            DashboardStatsVO stats = dashboardService.getDashboardStats();

            logger.info("刷新统计数据缓存成功");
            return Result.success(stats);

        } catch (Exception e) {
            logger.error("刷新统计数据缓存失败", e);
            return Result.fail("CACHE002", "刷新缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证管理员权限
     */
    private boolean validateAdminPermission(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!JwtUtil.validateToken(token)) {
                logger.warn("Token验证失败");
                return false;
            }

            // 检查是否为管理员角色
            String role = JwtUtil.getRoleFromToken(token);
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                logger.warn("权限不足，当前角色: {}", role);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("权限验证异常", e);
            return false;
        }
    }

    /**
     * 解析统计周期参数
     */
    private StatsPeriod parseStatsPeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            return null;
        }

        try {
            return StatsPeriod.fromCode(period);
        } catch (Exception e) {
            logger.warn("无效的统计周期参数: {}", period);
            return null;
        }
    }
}
