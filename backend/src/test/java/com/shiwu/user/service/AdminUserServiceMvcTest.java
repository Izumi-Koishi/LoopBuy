package com.shiwu.user.service;

import com.shiwu.admin.model.AdminUserQueryDTO;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.test.TestBase;
import com.shiwu.user.dao.AdminUserDao;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.model.User;
import com.shiwu.user.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserService MVC框架测试
 * 
 * 测试重构后的AdminUserService是否正确使用MVC框架的依赖注入
 * 验证@Service和@Autowired注解的功能
 * 保持与原测试用例完全一致的测试覆盖率
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AdminUserService MVC框架测试")
public class AdminUserServiceMvcTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceMvcTest.class);
    
    // 测试常量
    private static final Long TEST_ADMIN_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_USER_ID_2 = 101L;
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 Test Browser";
    private static final String TEST_REASON = "测试封禁原因";
    
    // 测试对象
    private AdminUserService adminUserService;
    
    // Mock对象
    @Mock
    private AdminUserDao mockAdminUserDao;
    
    @Mock
    private UserDao mockUserDao;
    
    @Mock
    private AuditLogService mockAuditLogService;
    
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        logger.info("AdminUserService MVC测试环境初始化开始");
        
        // 初始化Mockito
        closeable = MockitoAnnotations.openMocks(this);
        
        // 创建AdminUserService实例，使用兼容性构造函数注入Mock对象
        adminUserService = new AdminUserServiceImpl(mockAdminUserDao, mockUserDao, mockAuditLogService);
        
        logger.info("AdminUserService MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
        logger.info("AdminUserService MVC测试清理完成");
    }

    /**
     * 测试查询用户列表 - 成功场景
     */
    @Test
    @Order(1)
    @DisplayName("1.1 查询用户列表测试 - 成功场景")
    public void testFindUsers_Success() {
        logger.info("开始测试查询用户列表功能 - 成功场景");
        
        // 准备测试数据
        AdminUserQueryDTO queryDTO = new AdminUserQueryDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(20);
        
        List<Map<String, Object>> mockUsers = Arrays.asList(
            createMockUserMap(TEST_USER_ID, "testuser1", 0),
            createMockUserMap(TEST_USER_ID_2, "testuser2", 1)
        );
        
        // 设置Mock行为
        when(mockAdminUserDao.findUsers(queryDTO)).thenReturn(mockUsers);
        when(mockAdminUserDao.countUsers(queryDTO)).thenReturn(2);
        
        // 执行测试
        Map<String, Object> result = adminUserService.findUsers(queryDTO);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertEquals(2L, result.get("totalCount"), "总数应该匹配");
        assertEquals(20, result.get("pageSize"), "页面大小应该匹配");
        assertEquals(1, result.get("page"), "页码应该匹配");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        assertNotNull(users, "用户列表不应为null");
        assertEquals(2, users.size(), "用户数量应该匹配");
        
        // 验证Mock调用
        verify(mockAdminUserDao).findUsers(queryDTO);
        verify(mockAdminUserDao).countUsers(queryDTO);
        
        logger.info("测试查询用户列表功能 - 成功场景 - 通过");
    }

    /**
     * 测试查询用户列表 - 空参数
     */
    @Test
    @Order(2)
    @DisplayName("1.2 查询用户列表测试 - 空参数")
    public void testFindUsers_NullParameter() {
        logger.info("开始测试查询用户列表功能 - 空参数场景");
        
        // 执行测试
        Map<String, Object> result = adminUserService.findUsers(null);
        
        // 验证结果
        assertNull(result, "空参数时查询结果应该为null");
        
        // 验证没有调用DAO
        verify(mockAdminUserDao, never()).findUsers(any());
        verify(mockAdminUserDao, never()).countUsers(any());
        
        logger.info("测试查询用户列表功能 - 空参数场景 - 通过");
    }

    /**
     * 测试获取用户详情 - 成功场景
     */
    @Test
    @Order(3)
    @DisplayName("2.1 获取用户详情测试 - 成功场景")
    public void testGetUserDetail_Success() {
        logger.info("开始测试获取用户详情功能 - 成功场景");
        
        // 准备测试数据
        User mockUser = createMockUser(TEST_USER_ID, "testuser", 0);
        
        // 设置Mock行为
        when(mockUserDao.findById(TEST_USER_ID)).thenReturn(mockUser);
        
        // 执行测试
        Map<String, Object> result = adminUserService.getUserDetail(TEST_USER_ID, TEST_ADMIN_ID);
        
        // 验证结果
        assertNotNull(result, "用户详情不应为null");

        User user = (User) result.get("user");
        assertNotNull(user, "用户对象不应为null");
        assertEquals(TEST_USER_ID, user.getId(), "用户ID应该匹配");
        assertEquals("testuser", user.getUsername(), "用户名应该匹配");
        assertEquals(Integer.valueOf(0), user.getStatus(), "用户状态应该匹配");
        
        // 验证Mock调用
        verify(mockUserDao).findById(TEST_USER_ID);
        
        logger.info("测试获取用户详情功能 - 成功场景 - 通过");
    }

    /**
     * 测试获取用户详情 - 用户不存在
     */
    @Test
    @Order(4)
    @DisplayName("2.2 获取用户详情测试 - 用户不存在")
    public void testGetUserDetail_UserNotFound() {
        logger.info("开始测试获取用户详情功能 - 用户不存在场景");
        
        // 设置Mock行为
        when(mockUserDao.findById(999L)).thenReturn(null);
        
        // 执行测试
        Map<String, Object> result = adminUserService.getUserDetail(999L, TEST_ADMIN_ID);
        
        // 验证结果
        assertNull(result, "用户不存在时应该返回null");
        
        // 验证Mock调用
        verify(mockUserDao).findById(999L);
        
        logger.info("测试获取用户详情功能 - 用户不存在场景 - 通过");
    }

    /**
     * 测试封禁用户 - 成功场景
     */
    @Test
    @Order(5)
    @DisplayName("3.1 封禁用户测试 - 成功场景")
    public void testBanUser_Success() {
        logger.info("开始测试封禁用户功能 - 成功场景");
        
        // 准备测试数据
        User mockUser = createMockUser(TEST_USER_ID, "testuser", 0);
        
        // 设置Mock行为
        when(mockUserDao.findById(TEST_USER_ID)).thenReturn(mockUser);
        when(mockAdminUserDao.updateUserStatus(TEST_USER_ID, 1, TEST_ADMIN_ID)).thenReturn(true);
        when(mockAuditLogService.logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class))).thenReturn(1L);
        
        // 执行测试
        boolean result = adminUserService.banUser(TEST_USER_ID, TEST_ADMIN_ID, TEST_REASON, TEST_IP, TEST_USER_AGENT);
        
        // 验证结果
        assertTrue(result, "封禁用户应该成功");
        
        // 验证Mock调用
        verify(mockUserDao).findById(TEST_USER_ID);
        verify(mockAdminUserDao).updateUserStatus(TEST_USER_ID, 1, TEST_ADMIN_ID);
        verify(mockAuditLogService).logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class));
        
        logger.info("测试封禁用户功能 - 成功场景 - 通过");
    }

    /**
     * 测试禁言用户 - 成功场景
     */
    @Test
    @Order(6)
    @DisplayName("4.1 禁言用户测试 - 成功场景")
    public void testMuteUser_Success() {
        logger.info("开始测试禁言用户功能 - 成功场景");
        
        // 准备测试数据
        User mockUser = createMockUser(TEST_USER_ID, "testuser", 0);
        
        // 设置Mock行为
        when(mockUserDao.findById(TEST_USER_ID)).thenReturn(mockUser);
        when(mockAdminUserDao.updateUserStatus(TEST_USER_ID, 2, TEST_ADMIN_ID)).thenReturn(true);
        when(mockAuditLogService.logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class))).thenReturn(1L);
        
        // 执行测试
        boolean result = adminUserService.muteUser(TEST_USER_ID, TEST_ADMIN_ID, TEST_REASON, TEST_IP, TEST_USER_AGENT);
        
        // 验证结果
        assertTrue(result, "禁言用户应该成功");
        
        // 验证Mock调用
        verify(mockUserDao).findById(TEST_USER_ID);
        verify(mockAdminUserDao).updateUserStatus(TEST_USER_ID, 2, TEST_ADMIN_ID);
        verify(mockAuditLogService).logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class));
        
        logger.info("测试禁言用户功能 - 成功场景 - 通过");
    }

    /**
     * 测试解封用户 - 成功场景
     */
    @Test
    @Order(7)
    @DisplayName("5.1 解封用户测试 - 成功场景")
    public void testUnbanUser_Success() {
        logger.info("开始测试解封用户功能 - 成功场景");
        
        // 准备测试数据
        User mockUser = createMockUser(TEST_USER_ID, "testuser", 1); // 已封禁状态
        
        // 设置Mock行为
        when(mockUserDao.findById(TEST_USER_ID)).thenReturn(mockUser);
        when(mockAdminUserDao.updateUserStatus(TEST_USER_ID, 0, TEST_ADMIN_ID)).thenReturn(true);
        when(mockAuditLogService.logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class))).thenReturn(1L);

        // 执行测试
        boolean result = adminUserService.unbanUser(TEST_USER_ID, TEST_ADMIN_ID, TEST_IP, TEST_USER_AGENT);
        
        // 验证结果
        assertTrue(result, "解封用户应该成功");
        
        // 验证Mock调用
        verify(mockUserDao).findById(TEST_USER_ID);
        verify(mockAdminUserDao).updateUserStatus(TEST_USER_ID, 0, TEST_ADMIN_ID);
        verify(mockAuditLogService).logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class));
        
        logger.info("测试解封用户功能 - 成功场景 - 通过");
    }

    /**
     * 测试批量封禁用户 - 成功场景
     */
    @Test
    @Order(8)
    @DisplayName("6.1 批量封禁用户测试 - 成功场景")
    public void testBatchBanUsers_Success() {
        logger.info("开始测试批量封禁用户功能 - 成功场景");
        
        // 准备测试数据
        List<Long> userIds = Arrays.asList(TEST_USER_ID, TEST_USER_ID_2);
        
        // 设置Mock行为
        when(mockUserDao.findById(TEST_USER_ID)).thenReturn(createMockUser(TEST_USER_ID, "testuser1", 0));
        when(mockUserDao.findById(TEST_USER_ID_2)).thenReturn(createMockUser(TEST_USER_ID_2, "testuser2", 0));
        when(mockAdminUserDao.updateUserStatus(anyLong(), eq(1), eq(TEST_ADMIN_ID))).thenReturn(true);
        when(mockAuditLogService.logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class))).thenReturn(1L);
        
        // 执行测试
        Map<String, Object> result = adminUserService.batchBanUsers(userIds, TEST_ADMIN_ID, TEST_REASON, TEST_IP, TEST_USER_AGENT);
        
        // 验证结果
        assertNotNull(result, "批量封禁结果不应为null");
        assertEquals(2, result.get("totalCount"), "总数应该匹配");
        assertEquals(2, result.get("successCount"), "成功数应该匹配");
        assertEquals(0, result.get("failCount"), "失败数应该为0");
        
        // 验证Mock调用
        verify(mockUserDao, times(2)).findById(anyLong());
        verify(mockAdminUserDao, times(2)).updateUserStatus(anyLong(), eq(1), eq(TEST_ADMIN_ID));
        verify(mockAuditLogService, times(2)).logAction(any(Long.class), any(), any(), any(Long.class), any(String.class), any(String.class), any(String.class), any(Boolean.class));
        
        logger.info("测试批量封禁用户功能 - 成功场景 - 通过");
    }

    // ==================== 工具方法 ====================
    
    /**
     * 创建Mock用户Map
     */
    private Map<String, Object> createMockUserMap(Long id, String username, Integer status) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", username);
        user.put("status", status);
        user.put("email", username + "@test.com");
        user.put("createTime", new Date());
        return user;
    }
    
    /**
     * 创建Mock用户对象
     */
    private User createMockUser(Long id, String username, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        user.setEmail(username + "@test.com");
        user.setCreateTime(java.time.LocalDateTime.now());
        return user;
    }
}
