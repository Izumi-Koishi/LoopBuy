package com.shiwu.admin.service;

import com.shiwu.admin.model.DashboardStatsVO;
import com.shiwu.admin.model.StatsPeriod;
import com.shiwu.admin.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DashboardService MVC框架测试类
 * 测试重构后的DashboardService在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DashboardService MVC框架测试")
public class DashboardServiceMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceMvcTest.class);
    
    private DashboardService dashboardService;
    
    @BeforeEach
    public void setUp() {
        // 使用MVC框架的依赖注入方式创建Service
        dashboardService = new DashboardServiceImpl();
        logger.info("DashboardService MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("DashboardService MVC测试清理完成");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 获取仪表盘统计数据测试 - 今日")
    public void testGetDashboardStats_Today() {
        logger.info("开始测试获取仪表盘统计数据功能 - 今日 - MVC框架");
        
        // 执行测试
        DashboardStatsVO result = dashboardService.getDashboardStats(StatsPeriod.TODAY);
        
        // 验证结果
        assertNotNull(result, "统计数据不应为空");
        logger.info("获取仪表盘统计数据测试完成 - 今日 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 获取仪表盘统计数据测试 - 本周")
    public void testGetDashboardStats_Week() {
        logger.info("开始测试获取仪表盘统计数据功能 - 本周 - MVC框架");

        // 执行测试
        DashboardStatsVO result = dashboardService.getDashboardStats(StatsPeriod.THIS_WEEK);

        // 验证结果
        assertNotNull(result, "统计数据不应为空");
        logger.info("获取仪表盘统计数据测试完成 - 本周 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取仪表盘统计数据测试 - 本月")
    public void testGetDashboardStats_Month() {
        logger.info("开始测试获取仪表盘统计数据功能 - 本月 - MVC框架");

        // 执行测试
        DashboardStatsVO result = dashboardService.getDashboardStats(StatsPeriod.THIS_MONTH);

        // 验证结果
        assertNotNull(result, "统计数据不应为空");
        logger.info("获取仪表盘统计数据测试完成 - 本月 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 获取仪表盘统计数据测试 - 参数验证")
    public void testGetDashboardStatsValidation() {
        logger.info("开始测试获取仪表盘统计数据参数验证 - MVC框架");
        
        // 测试null统计周期
        DashboardStatsVO result1 = dashboardService.getDashboardStats(null);
        assertNotNull(result1, "返回结果不应为空");
        logger.info("null统计周期测试: result={}", result1);
        
        logger.info("获取仪表盘统计数据参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 刷新缓存测试")
    public void testRefreshCache() {
        logger.info("开始测试刷新缓存功能 - MVC框架");

        // 执行测试
        boolean result = dashboardService.refreshStatsCache();

        // 验证结果
        logger.info("刷新缓存测试完成 - MVC框架: success={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 获取默认统计数据测试")
    public void testGetDefaultStats() {
        logger.info("开始测试获取默认统计数据功能 - MVC框架");

        // 执行测试
        DashboardStatsVO result = dashboardService.getDashboardStats();

        // 验证结果
        assertNotNull(result, "默认统计数据不应为空");
        logger.info("获取默认统计数据测试完成 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 获取过去7天统计数据测试")
    public void testGetLast7DaysStats() {
        logger.info("开始测试获取过去7天统计数据功能 - MVC框架");

        // 执行测试
        DashboardStatsVO result = dashboardService.getDashboardStats(StatsPeriod.LAST_7_DAYS);

        // 验证结果
        assertNotNull(result, "过去7天统计数据不应为空");
        logger.info("获取过去7天统计数据测试完成 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 获取实时数据测试")
    public void testGetRealTimeData() {
        logger.info("开始测试获取实时数据功能 - MVC框架");
        
        // 执行测试
        DashboardStatsVO result = dashboardService.getRealTimeStats();
        
        // 验证结果
        assertNotNull(result, "实时数据不应为空");
        logger.info("获取实时数据测试完成 - MVC框架: result={}", result);
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("MVC-9 缓存机制测试")
    public void testCacheMechanism() {
        logger.info("开始测试缓存机制 - MVC框架");
        
        // 1. 第一次获取数据（应该从数据库获取）
        long startTime1 = System.currentTimeMillis();
        DashboardStatsVO result1 = dashboardService.getDashboardStats(StatsPeriod.TODAY);
        long endTime1 = System.currentTimeMillis();
        long duration1 = endTime1 - startTime1;
        
        assertNotNull(result1, "第一次获取的数据不应为空");
        logger.info("第一次获取数据耗时: {}ms", duration1);
        
        // 2. 第二次获取数据（应该从缓存获取，速度更快）
        long startTime2 = System.currentTimeMillis();
        DashboardStatsVO result2 = dashboardService.getDashboardStats(StatsPeriod.TODAY);
        long endTime2 = System.currentTimeMillis();
        long duration2 = endTime2 - startTime2;
        
        assertNotNull(result2, "第二次获取的数据不应为空");
        logger.info("第二次获取数据耗时: {}ms", duration2);
        
        // 3. 刷新缓存
        boolean refreshResult = dashboardService.refreshStatsCache();
        logger.info("刷新缓存: success={}", refreshResult);
        
        logger.info("缓存机制测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 仪表盘完整流程测试")
    public void testCompleteDashboardWorkflow() {
        logger.info("开始测试仪表盘完整流程 - MVC框架");

        // 1. 获取默认统计数据
        DashboardStatsVO defaultStats = dashboardService.getDashboardStats();
        assertNotNull(defaultStats, "默认统计数据不应为空");
        logger.info("默认统计数据: result={}", defaultStats);

        // 2. 获取今日统计数据
        DashboardStatsVO todayStats = dashboardService.getDashboardStats(StatsPeriod.TODAY);
        assertNotNull(todayStats, "今日统计数据不应为空");
        logger.info("今日统计数据: result={}", todayStats);

        // 3. 获取本周统计数据
        DashboardStatsVO weekStats = dashboardService.getDashboardStats(StatsPeriod.THIS_WEEK);
        assertNotNull(weekStats, "本周统计数据不应为空");
        logger.info("本周统计数据: result={}", weekStats);

        // 4. 获取本月统计数据
        DashboardStatsVO monthStats = dashboardService.getDashboardStats(StatsPeriod.THIS_MONTH);
        assertNotNull(monthStats, "本月统计数据不应为空");
        logger.info("本月统计数据: result={}", monthStats);

        // 5. 获取过去30天统计数据
        DashboardStatsVO last30DaysStats = dashboardService.getDashboardStats(StatsPeriod.LAST_30_DAYS);
        assertNotNull(last30DaysStats, "过去30天统计数据不应为空");
        logger.info("过去30天统计数据: result={}", last30DaysStats);

        // 6. 获取实时数据
        DashboardStatsVO realTimeStats = dashboardService.getRealTimeStats();
        assertNotNull(realTimeStats, "实时数据不应为空");
        logger.info("实时数据: result={}", realTimeStats);

        // 7. 刷新缓存
        boolean refreshResult = dashboardService.refreshStatsCache();
        logger.info("刷新缓存: success={}", refreshResult);

        // 8. 再次获取统计数据验证缓存刷新
        DashboardStatsVO refreshedStats = dashboardService.getDashboardStats(StatsPeriod.TODAY);
        assertNotNull(refreshedStats, "刷新后统计数据不应为空");
        logger.info("刷新后统计数据: result={}", refreshedStats);

        logger.info("仪表盘完整流程测试通过 - MVC框架");
    }
}
