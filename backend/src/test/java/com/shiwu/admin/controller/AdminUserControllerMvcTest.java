package com.shiwu.admin.controller;

import com.shiwu.admin.model.AdminUserQueryDTO;
import com.shiwu.admin.service.AdminService;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.user.service.AdminUserService;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserController MVC框架测试类
 * 测试重构后的AdminUserController在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AdminUserController MVC框架测试")
public class AdminUserControllerMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserControllerMvcTest.class);
    
    @Mock
    private AdminUserService adminUserService;
    
    @Mock
    private AdminService adminService;
    
    @Mock
    private HttpServletRequest request;
    
    private AdminUserController adminUserController;
    
    // 测试数据
    private static final String VALID_TOKEN = "valid_token";
    private static final String INVALID_TOKEN = "invalid_token";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        adminUserController = new AdminUserController(adminUserService, adminService);
        logger.info("AdminUserController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("AdminUserController MVC测试清理完成");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 查询用户列表测试 - 成功场景")
    public void testGetUsers_Success() {
        logger.info("开始测试查询用户列表功能 - 成功场景 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            Map<String, Object> mockResult = createMockUserListResult();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(ADMIN_ID);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟权限验证
            when(adminService.hasPermission(ADMIN_ID, "ADMIN")).thenReturn(true);
            
            // 模拟服务调用
            when(adminUserService.findUsers(any(AdminUserQueryDTO.class))).thenReturn(mockResult);
            
            // 执行测试
            Result<Map<String, Object>> result = adminUserController.getUsers(1, 10, "test", "1", request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            assertEquals(2, result.getData().get("total"), "总数应该匹配");
            
            // 验证服务调用
            verify(adminUserService, times(1)).findUsers(any(AdminUserQueryDTO.class));
            verify(adminService, times(1)).hasPermission(ADMIN_ID, "ADMIN");
            
            logger.info("查询用户列表测试通过 - 成功场景 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 查询用户列表测试 - 权限验证失败")
    public void testGetUsers_AuthFailure() {
        logger.info("开始测试查询用户列表功能 - 权限验证失败 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 模拟JWT验证失败
            jwtUtilMock.when(() -> JwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
            
            // 执行测试
            Result<Map<String, Object>> result = adminUserController.getUsers(1, 10, null, null, request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertFalse(result.getSuccess(), "应该失败");
            assertEquals("AUTH001", result.getError().getCode(), "错误码应该正确");
            
            // 验证服务未被调用
            verify(adminUserService, never()).findUsers(any(AdminUserQueryDTO.class));
            
            logger.info("查询用户列表测试通过 - 权限验证失败 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取用户详情测试 - 成功场景")
    public void testGetUserDetail_Success() {
        logger.info("开始测试获取用户详情功能 - 成功场景 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            Map<String, Object> mockUserDetail = createMockUserDetail();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(ADMIN_ID);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟权限验证
            when(adminService.hasPermission(ADMIN_ID, "ADMIN")).thenReturn(true);
            
            // 模拟服务调用
            when(adminUserService.getUserDetail(USER_ID, ADMIN_ID)).thenReturn(mockUserDetail);
            
            // 执行测试
            Result<Object> result = adminUserController.getUserDetail(USER_ID, request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertTrue(result.getSuccess(), "应该成功");
            assertNotNull(result.getData(), "数据不应为空");
            
            // 验证服务调用
            verify(adminUserService, times(1)).getUserDetail(USER_ID, ADMIN_ID);
            verify(adminService, times(1)).hasPermission(ADMIN_ID, "ADMIN");
            
            logger.info("获取用户详情测试通过 - 成功场景 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 获取用户详情测试 - 用户不存在")
    public void testGetUserDetail_UserNotFound() {
        logger.info("开始测试获取用户详情功能 - 用户不存在 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(ADMIN_ID);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟权限验证
            when(adminService.hasPermission(ADMIN_ID, "ADMIN")).thenReturn(true);
            
            // 模拟服务调用返回null
            when(adminUserService.getUserDetail(USER_ID, ADMIN_ID)).thenReturn(null);
            
            // 执行测试
            Result<Object> result = adminUserController.getUserDetail(USER_ID, request);
            
            // 验证结果
            assertNotNull(result, "结果不应为空");
            assertFalse(result.getSuccess(), "应该失败");
            assertEquals("USER002", result.getError().getCode(), "错误码应该正确");
            
            // 验证服务调用
            verify(adminUserService, times(1)).getUserDetail(USER_ID, ADMIN_ID);
            
            logger.info("获取用户详情测试通过 - 用户不存在 - MVC框架");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 用户管理完整流程测试")
    public void testCompleteUserManagementWorkflow() {
        logger.info("开始测试用户管理完整流程 - MVC框架");
        
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 准备测试数据
            Map<String, Object> mockUserList = createMockUserListResult();
            Map<String, Object> mockUserDetail = createMockUserDetail();
            
            // 模拟JWT验证
            jwtUtilMock.when(() -> JwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(ADMIN_ID);
            
            // 模拟请求
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            
            // 模拟权限验证
            when(adminService.hasPermission(ADMIN_ID, "ADMIN")).thenReturn(true);
            
            // 模拟服务调用
            when(adminUserService.findUsers(any(AdminUserQueryDTO.class))).thenReturn(mockUserList);
            when(adminUserService.getUserDetail(USER_ID, ADMIN_ID)).thenReturn(mockUserDetail);
            
            // 1. 查询用户列表
            Result<Map<String, Object>> listResult = adminUserController.getUsers(1, 10, null, null, request);
            assertTrue(listResult.getSuccess(), "查询用户列表应该成功");
            logger.info("查询用户列表完成");
            
            // 2. 获取用户详情
            Result<Object> detailResult = adminUserController.getUserDetail(USER_ID, request);
            assertTrue(detailResult.getSuccess(), "获取用户详情应该成功");
            logger.info("获取用户详情完成");
            
            // 验证所有服务调用
            verify(adminUserService, times(1)).findUsers(any(AdminUserQueryDTO.class));
            verify(adminUserService, times(1)).getUserDetail(USER_ID, ADMIN_ID);
            verify(adminService, times(2)).hasPermission(ADMIN_ID, "ADMIN");
            
            logger.info("用户管理完整流程测试通过 - MVC框架");
        }
    }

    /**
     * 创建测试用的用户列表结果
     */
    private Map<String, Object> createMockUserListResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 2);
        result.put("list", java.util.Arrays.asList(
            createMockUser(100L, "user1@test.com"),
            createMockUser(101L, "user2@test.com")
        ));
        return result;
    }

    /**
     * 创建测试用的用户详情
     */
    private Map<String, Object> createMockUserDetail() {
        return createMockUser(USER_ID, "testuser@test.com");
    }

    /**
     * 创建测试用的用户对象
     */
    private Map<String, Object> createMockUser(Long userId, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("username", "testuser" + userId);
        user.put("status", 1);
        user.put("createTime", "2024-01-01 00:00:00");
        return user;
    }
}
