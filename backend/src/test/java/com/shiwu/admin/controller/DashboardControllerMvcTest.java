package com.shiwu.admin.controller;

import com.shiwu.admin.model.DashboardStatsVO;
import com.shiwu.admin.model.StatsPeriod;
import com.shiwu.admin.service.DashboardService;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardController MVC框架测试类
 * 测试重构后的DashboardController在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DashboardController MVC框架测试")
public class DashboardControllerMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(DashboardControllerMvcTest.class);
    
    @Mock
    private DashboardService dashboardService;
    
    @Mock
    private HttpServletRequest request;
    
    private DashboardController dashboardController;
    
    // 测试数据
    private static final String VALID_TOKEN = "valid_token";
    private static final String INVALID_TOKEN = "invalid_token";
    private static final String ADMIN_ROLE = "ADMIN";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardController = new DashboardController(dashboardService);
        logger.info("DashboardController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("DashboardController MVC测试清理完成");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 获取统计数据测试 - 成功场景")
    public void testGetStats_Success() {
        logger.info("开始测试获取统计数据功能 - 成功场景 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            DashboardStatsVO statsVO = createTestStatsVO();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用
            when(dashboardService.getDashboardStats()).thenReturn(statsVO);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.getStats(null, request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            assertNotNull(result.getData().getOverview(), "概览数据不应为空");
            
            // 验证服务调用
            verify(dashboardService, times(1)).getDashboardStats();
            
            logger.info("获取统计数据测试通过 - 成功场景 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 获取统计数据测试 - 指定周期")
    public void testGetStats_WithPeriod() {
        logger.info("开始测试获取统计数据功能 - 指定周期 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            DashboardStatsVO statsVO = createTestStatsVO();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用
            when(dashboardService.getDashboardStats(StatsPeriod.LAST_7_DAYS)).thenReturn(statsVO);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.getStats("LAST_7_DAYS", request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            
            // 验证服务调用
            verify(dashboardService, times(1)).getDashboardStats(StatsPeriod.LAST_7_DAYS);
            
            logger.info("获取统计数据测试通过 - 指定周期 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取统计数据测试 - 权限验证失败")
    public void testGetStats_AuthFailure() {
        logger.info("开始测试获取统计数据功能 - 权限验证失败 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 模拟JWT验证失败
            jwtUtilMock.when(() -> JwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.getStats(null, request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertFalse(result.getSuccess(), "应该失败");
            assertEquals("AUTH001", result.getError().getCode(), "错误码应该正确");
            
            // 验证服务未被调用
            verify(dashboardService, never()).getDashboardStats();
            
            logger.info("获取统计数据测试通过 - 权限验证失败 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 获取实时统计数据测试 - 成功场景")
    public void testGetRealTimeStats_Success() {
        logger.info("开始测试获取实时统计数据功能 - 成功场景 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            DashboardStatsVO statsVO = createTestStatsVO();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用
            when(dashboardService.getRealTimeStats()).thenReturn(statsVO);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.getRealTimeStats(request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            
            // 验证服务调用
            verify(dashboardService, times(1)).getRealTimeStats();
            
            logger.info("获取实时统计数据测试通过 - 成功场景 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 刷新统计数据缓存测试 - 成功场景")
    public void testRefreshStats_Success() {
        logger.info("开始测试刷新统计数据缓存功能 - 成功场景 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            DashboardStatsVO statsVO = createTestStatsVO();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用
            when(dashboardService.refreshStatsCache()).thenReturn(true);
            when(dashboardService.getDashboardStats()).thenReturn(statsVO);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.refreshStats(request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            
            // 验证服务调用
            verify(dashboardService, times(1)).refreshStatsCache();
            verify(dashboardService, times(1)).getDashboardStats();
            
            logger.info("刷新统计数据缓存测试通过 - 成功场景 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 刷新统计数据缓存测试 - 刷新失败")
    public void testRefreshStats_RefreshFailure() {
        logger.info("开始测试刷新统计数据缓存功能 - 刷新失败 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用失败
            when(dashboardService.refreshStatsCache()).thenReturn(false);
            
            // 执行测试
            Result<DashboardStatsVO> result = dashboardController.refreshStats(request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertFalse(result.getSuccess(), "应该失败");
            assertEquals("CACHE001", result.getError().getCode(), "错误码应该正确");
            
            // 验证服务调用
            verify(dashboardService, times(1)).refreshStatsCache();
            verify(dashboardService, never()).getDashboardStats();
            
            logger.info("刷新统计数据缓存测试通过 - 刷新失败 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 仪表盘控制器完整流程测试")
    public void testCompleteDashboardWorkflow() {
        logger.info("开始测试仪表盘控制器完整流程 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            DashboardStatsVO statsVO = createTestStatsVO();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(ADMIN_ROLE);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟服务调用
            when(dashboardService.getDashboardStats()).thenReturn(statsVO);
            when(dashboardService.getDashboardStats(any(StatsPeriod.class))).thenReturn(statsVO);
            when(dashboardService.getRealTimeStats()).thenReturn(statsVO);
            when(dashboardService.refreshStatsCache()).thenReturn(true);
            
            // 1. 获取默认统计数据
            Result<DashboardStatsVO> result1 = dashboardController.getStats(null, request);
            assertTrue(result1.getSuccess(), "获取默认统计数据应该成功");
            logger.info("获取默认统计数据完成");
            
            // 2. 获取指定周期统计数据
            Result<DashboardStatsVO> result2 = dashboardController.getStats("TODAY", request);
            assertTrue(result2.getSuccess(), "获取指定周期统计数据应该成功");
            logger.info("获取指定周期统计数据完成");
            
            // 3. 获取实时统计数据
            Result<DashboardStatsVO> result3 = dashboardController.getRealTimeStats(request);
            assertTrue(result3.getSuccess(), "获取实时统计数据应该成功");
            logger.info("获取实时统计数据完成");
            
            // 4. 刷新统计数据缓存
            Result<DashboardStatsVO> result4 = dashboardController.refreshStats(request);
            assertTrue(result4.getSuccess(), "刷新统计数据缓存应该成功");
            logger.info("刷新统计数据缓存完成");
            
            // 验证所有服务调用
            verify(dashboardService, times(2)).getDashboardStats(); // 默认调用 + 刷新后调用
            verify(dashboardService, times(1)).getDashboardStats(StatsPeriod.TODAY);
            verify(dashboardService, times(1)).getRealTimeStats();
            verify(dashboardService, times(1)).refreshStatsCache();
            
            logger.info("仪表盘控制器完整流程测试通过 - MVC框架");
        }
    }

    /**
     * 创建测试用的统计数据VO
     */
    private DashboardStatsVO createTestStatsVO() {
        DashboardStatsVO statsVO = new DashboardStatsVO();

        // 创建概览统计数据
        DashboardStatsVO.OverviewStats overview = new DashboardStatsVO.OverviewStats();
        overview.setTotalUsers(1000L);
        overview.setTotalProducts(500L);
        overview.setTotalActiveUsers(800L);
        overview.setTotalPendingProducts(10L);
        overview.setAverageRating(4.5);
        statsVO.setOverview(overview);

        return statsVO;
    }
}
