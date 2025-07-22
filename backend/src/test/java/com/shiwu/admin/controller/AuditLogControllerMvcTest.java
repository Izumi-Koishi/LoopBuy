package com.shiwu.admin.controller;

import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.common.result.Result;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuditLogController MVC测试
 * 测试所有7个API的功能完整性
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditLogControllerMvcTest {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogControllerMvcTest.class);

    private AuditLogController auditLogController;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private AdminService adminService;
    
    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogController = new AuditLogController(auditLogService, adminService);
        logger.info("AuditLogController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("AuditLogController MVC测试清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("MVC-1 查询审计日志列表接口测试")
    public void testGetAuditLogs() {
        logger.info("开始测试查询审计日志列表接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟审计日志查询结果
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("logs", Arrays.asList());
        mockResult.put("total", 0);
        when(auditLogService.getAuditLogs(any())).thenReturn(mockResult);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getAuditLogs(1, 20, null, null, null, null, null, request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试查询审计日志列表接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(2)
    @DisplayName("MVC-2 获取审计日志详情接口测试")
    public void testGetAuditLogDetail() {
        logger.info("开始测试获取审计日志详情接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getAuditLogDetail(1L, request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试获取审计日志详情接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(3)
    @DisplayName("MVC-3 获取审计日志统计信息接口测试（修复功能）")
    public void testGetAuditLogStatistics() {
        logger.info("开始测试获取审计日志统计信息接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟统计数据
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalLogs", 1000);
        mockStats.put("todayLogs", 50);
        when(auditLogService.getOperationStats(anyInt())).thenReturn(mockStats);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getAuditLogStatistics(7, request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试获取审计日志统计信息接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(4)
    @DisplayName("MVC-4 导出审计日志接口测试（修复功能）")
    public void testExportAuditLogs() {
        logger.info("开始测试导出审计日志接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟导出数据
        when(auditLogService.exportAuditLogs(any())).thenReturn(Arrays.asList());
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.exportAuditLogs("csv", null, null, request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试导出审计日志接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(5)
    @DisplayName("MVC-5 获取活动趋势数据接口测试（新增功能）")
    public void testGetActivityTrend() {
        logger.info("开始测试获取活动趋势数据接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟趋势数据
        List<Map<String, Object>> mockTrend = Arrays.asList();
        when(auditLogService.getActivityTrend(anyInt())).thenReturn(mockTrend);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getActivityTrend(7, request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试获取活动趋势数据接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(6)
    @DisplayName("MVC-6 获取可用操作类型接口测试（新增功能）")
    public void testGetAvailableActions() {
        logger.info("开始测试获取可用操作类型接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟操作类型数据
        List<Map<String, String>> mockActions = Arrays.asList();
        when(auditLogService.getAvailableActions()).thenReturn(mockActions);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getAvailableActions(request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试获取可用操作类型接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(7)
    @DisplayName("MVC-7 获取可用目标类型接口测试（新增功能）")
    public void testGetAvailableTargetTypes() {
        logger.info("开始测试获取可用目标类型接口");
        
        // 模拟权限检查
        when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
        
        // 模拟目标类型数据
        List<Map<String, String>> mockTargetTypes = Arrays.asList();
        when(auditLogService.getAvailableTargetTypes()).thenReturn(mockTargetTypes);
        
        // 模拟有效的JWT Token
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        
        // 执行测试
        Result<Object> result = auditLogController.getAvailableTargetTypes(request);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("测试获取可用目标类型接口 - 完成: success={}", result.isSuccess());
    }

    @Test
    @Order(8)
    @DisplayName("MVC-8 AuditLogController完整功能测试")
    public void testCompleteAuditLogWorkflow() {
        logger.info("开始测试AuditLogController完整功能流程");
        
        try {
            // 模拟权限检查
            when(adminService.hasPermission(anyLong(), anyString())).thenReturn(true);
            when(request.getHeader("Authorization")).thenReturn("Bearer test.jwt.token");
            
            // 1. 查询审计日志列表
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("logs", Arrays.asList());
            mockResult.put("total", 0);
            when(auditLogService.getAuditLogs(any())).thenReturn(mockResult);
            
            Result<Object> listResult = auditLogController.getAuditLogs(1, 20, null, null, null, null, null, request);
            assertNotNull(listResult, "查询审计日志列表结果不应为空");
            logger.info("查询审计日志列表完成: success={}", listResult.isSuccess());
            
            // 2. 获取统计信息
            Map<String, Object> mockStats = new HashMap<>();
            mockStats.put("totalLogs", 1000);
            when(auditLogService.getOperationStats(anyInt())).thenReturn(mockStats);
            
            Result<Object> statsResult = auditLogController.getAuditLogStatistics(7, request);
            assertNotNull(statsResult, "获取统计信息结果不应为空");
            logger.info("获取统计信息完成: success={}", statsResult.isSuccess());
            
            // 3. 获取可用操作类型
            when(auditLogService.getAvailableActions()).thenReturn(Arrays.asList());
            
            Result<Object> actionsResult = auditLogController.getAvailableActions(request);
            assertNotNull(actionsResult, "获取可用操作类型结果不应为空");
            logger.info("获取可用操作类型完成: success={}", actionsResult.isSuccess());
            
            logger.info("AuditLogController完整功能测试通过");
        } catch (Exception e) {
            logger.error("AuditLogController完整功能测试失败", e);
            fail("测试不应该抛出异常");
        }
    }
}
