package com.shiwu.notification.service;

import com.shiwu.common.result.Result;
import com.shiwu.notification.dao.NotificationDao;
import com.shiwu.notification.model.Notification;
import com.shiwu.notification.service.impl.NotificationServiceImpl;
import com.shiwu.notification.vo.NotificationVO;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.dao.UserFollowDao;
import com.shiwu.user.model.User;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationService MVC测试
 * 
 * 测试通知服务的所有核心功能，包括：
 * 1. 创建单个通知
 * 2. 批量创建通知
 * 3. 创建商品审核通过粉丝通知
 * 4. 获取用户通知列表
 * 5. 获取未读通知数量
 * 6. 标记通知为已读
 * 7. 批量标记通知为已读
 * 8. 创建系统公告通知
 * 9. 创建新消息通知
 * 10. 异常处理和边界情况
 * 
 * @author LoopBuy Team
 * @version 2.0 - MVC专属版本
 * @since 2024-01-15
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotificationServiceMvcTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceMvcTest.class);
    
    private NotificationServiceImpl notificationService;
    
    @Mock
    private NotificationDao mockNotificationDao;
    
    @Mock
    private UserFollowDao mockUserFollowDao;
    
    @Mock
    private UserDao mockUserDao;
    
    // 测试数据常量
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_SELLER_ID = 2L;
    private static final Long TEST_PRODUCT_ID = 100L;
    private static final Long TEST_NOTIFICATION_ID = 1001L;
    private static final String TEST_PRODUCT_TITLE = "测试商品";
    private static final String TEST_MESSAGE_CONTENT = "测试消息内容";
    
    @BeforeEach
    void setUp() {
        logger.info("NotificationService MVC测试环境初始化开始");
        
        MockitoAnnotations.openMocks(this);
        
        // 创建NotificationServiceImpl实例，注入Mock的DAO
        notificationService = new NotificationServiceImpl(mockNotificationDao, mockUserFollowDao, mockUserDao);
        
        logger.info("NotificationService MVC测试环境初始化完成");
    }
    
    @AfterEach
    void tearDown() {
        logger.info("NotificationService MVC测试清理完成");
    }
    
    /**
     * 测试创建单个通知功能
     */
    @Test
    @Order(1)
    @DisplayName("MVC-1 创建单个通知功能测试")
    void testCreateNotification() {
        logger.info("开始测试创建单个通知功能");
        
        // 准备测试数据
        Notification notification = createMockNotification();
        
        // 模拟DAO返回成功
        when(mockNotificationDao.createNotification(notification)).thenReturn(TEST_NOTIFICATION_ID);
        
        // 执行测试
        Result<Long> result = notificationService.createNotification(notification);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "创建通知应该成功");
        assertEquals(TEST_NOTIFICATION_ID, result.getData(), "返回的通知ID应该正确");
        
        // 验证DAO调用
        verify(mockNotificationDao).createNotification(notification);
        
        logger.info("创建单个通知功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试批量创建通知功能
     */
    @Test
    @Order(2)
    @DisplayName("MVC-2 批量创建通知功能测试")
    void testBatchCreateNotifications() {
        logger.info("开始测试批量创建通知功能");
        
        // 准备测试数据
        List<Notification> notifications = Arrays.asList(
            createMockNotification(),
            createMockNotification()
        );
        
        // 模拟DAO返回成功
        when(mockNotificationDao.batchCreateNotifications(notifications)).thenReturn(2);

        // 执行测试
        Result<Integer> result = notificationService.batchCreateNotifications(notifications);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "批量创建通知应该成功");
        assertEquals(2, result.getData(), "应该成功创建2个通知");

        // 验证DAO调用
        verify(mockNotificationDao).batchCreateNotifications(notifications);
        
        logger.info("批量创建通知功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试创建商品审核通过粉丝通知功能
     */
    @Test
    @Order(3)
    @DisplayName("MVC-3 创建商品审核通过粉丝通知功能测试")
    void testCreateProductApprovedNotifications() {
        logger.info("开始测试创建商品审核通过粉丝通知功能");
        
        // 模拟粉丝列表
        List<Long> followerIds = Arrays.asList(1L, 2L, 3L);
        when(mockUserFollowDao.getFollowerIds(TEST_SELLER_ID)).thenReturn(followerIds);
        
        // 模拟卖家信息
        User seller = new User();
        seller.setId(TEST_SELLER_ID);
        seller.setUsername("测试卖家");
        when(mockUserDao.findPublicInfoById(TEST_SELLER_ID)).thenReturn(seller);
        
        // 模拟批量通知创建成功
        when(mockNotificationDao.batchCreateNotifications(anyList())).thenReturn(3);
        
        // 执行测试
        Result<Integer> result = notificationService.createProductApprovedNotifications(
                TEST_PRODUCT_ID, TEST_SELLER_ID, TEST_PRODUCT_TITLE);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "创建粉丝通知应该成功");
        assertEquals(3, result.getData(), "应该为3个粉丝创建通知");
        
        // 验证DAO调用
        verify(mockUserFollowDao).getFollowerIds(TEST_SELLER_ID);
        verify(mockUserDao).findPublicInfoById(TEST_SELLER_ID);
        verify(mockNotificationDao).batchCreateNotifications(anyList());
        
        logger.info("创建商品审核通过粉丝通知功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试获取用户通知列表功能
     */
    @Test
    @Order(4)
    @DisplayName("MVC-4 获取用户通知列表功能测试")
    void testGetUserNotifications() {
        logger.info("开始测试获取用户通知列表功能");
        
        // 模拟通知列表
        List<Notification> notifications = Arrays.asList(createMockNotification());
        when(mockNotificationDao.findNotificationsByUserId(TEST_USER_ID, 1, 10, false)).thenReturn(notifications);

        // 执行测试
        Result<List<NotificationVO>> result = notificationService.getUserNotifications(TEST_USER_ID, 1, 10, false);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "获取通知列表应该成功");
        assertNotNull(result.getData(), "通知列表不应为空");
        assertFalse(result.getData().isEmpty(), "通知列表不应为空");

        // 验证DAO调用
        verify(mockNotificationDao).findNotificationsByUserId(TEST_USER_ID, 1, 10, false);
        
        logger.info("获取用户通知列表功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试获取未读通知数量功能
     */
    @Test
    @Order(5)
    @DisplayName("MVC-5 获取未读通知数量功能测试")
    void testGetUnreadNotificationCount() {
        logger.info("开始测试获取未读通知数量功能");
        
        // 模拟未读通知数量
        when(mockNotificationDao.getUnreadNotificationCount(TEST_USER_ID)).thenReturn(5);

        // 执行测试
        Result<Integer> result = notificationService.getUnreadNotificationCount(TEST_USER_ID);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "获取未读通知数量应该成功");
        assertEquals(5, result.getData(), "未读通知数量应该是5");

        // 验证DAO调用
        verify(mockNotificationDao).getUnreadNotificationCount(TEST_USER_ID);
        
        logger.info("获取未读通知数量功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试标记通知为已读功能
     */
    @Test
    @Order(6)
    @DisplayName("MVC-6 标记通知为已读功能测试")
    void testMarkNotificationAsRead() {
        logger.info("开始测试标记通知为已读功能");
        
        // 模拟标记已读成功
        when(mockNotificationDao.markNotificationAsRead(TEST_NOTIFICATION_ID, TEST_USER_ID)).thenReturn(true);

        // 执行测试
        Result<Void> result = notificationService.markNotificationAsRead(TEST_NOTIFICATION_ID, TEST_USER_ID);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "标记通知为已读应该成功");

        // 验证DAO调用
        verify(mockNotificationDao).markNotificationAsRead(TEST_NOTIFICATION_ID, TEST_USER_ID);
        
        logger.info("标记通知为已读功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试批量标记通知为已读功能
     */
    @Test
    @Order(7)
    @DisplayName("MVC-7 批量标记通知为已读功能测试")
    void testBatchMarkNotificationsAsRead() {
        logger.info("开始测试批量标记通知为已读功能");
        
        // 模拟批量标记成功
        List<Long> notificationIds = Arrays.asList(1L, 2L, 3L);
        when(mockNotificationDao.batchMarkNotificationsAsRead(TEST_USER_ID, notificationIds)).thenReturn(3);

        // 执行测试
        Result<Integer> result = notificationService.batchMarkNotificationsAsRead(TEST_USER_ID, notificationIds);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "批量标记通知为已读应该成功");
        assertEquals(3, result.getData(), "应该标记3个通知为已读");

        // 验证DAO调用
        verify(mockNotificationDao).batchMarkNotificationsAsRead(TEST_USER_ID, notificationIds);
        
        logger.info("批量标记通知为已读功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试创建新消息通知功能
     */
    @Test
    @Order(8)
    @DisplayName("MVC-8 创建新消息通知功能测试")
    void testCreateMessageNotification() {
        logger.info("开始测试创建新消息通知功能");
        
        // 模拟通知创建成功
        when(mockNotificationDao.createNotification(any(Notification.class))).thenReturn(TEST_NOTIFICATION_ID);
        
        // 执行测试
        Result<Long> result = notificationService.createMessageNotification(
                TEST_USER_ID, TEST_SELLER_ID, "发送者", TEST_MESSAGE_CONTENT, "conv_123");
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        assertTrue(result.isSuccess(), "创建消息通知应该成功");
        assertEquals(TEST_NOTIFICATION_ID, result.getData(), "返回的通知ID应该正确");
        
        // 验证DAO调用
        verify(mockNotificationDao).createNotification(any(Notification.class));
        
        logger.info("创建新消息通知功能测试通过: success={}", result.isSuccess());
    }
    
    /**
     * 测试参数验证失败情况
     */
    @Test
    @Order(9)
    @DisplayName("MVC-9 参数验证失败测试")
    void testParameterValidationFailure() {
        logger.info("开始测试参数验证失败情况");
        
        // 测试空通知对象
        Result<Long> result1 = notificationService.createNotification(null);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "空通知对象应该返回失败");
        
        // 测试空用户ID
        Result<Integer> result2 = notificationService.getUnreadNotificationCount(null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "空用户ID应该返回失败");
        
        logger.info("参数验证失败测试通过");
    }
    
    // 辅助方法
    private Notification createMockNotification() {
        Notification notification = new Notification();
        notification.setId(TEST_NOTIFICATION_ID);
        notification.setRecipientId(TEST_USER_ID);
        notification.setTitle("测试通知");
        notification.setContent("测试通知内容");
        notification.setNotificationType("SYSTEM");
        notification.setSourceType("PRODUCT");
        notification.setSourceId(TEST_PRODUCT_ID);
        notification.setIsRead(false);
        notification.setPriority(1);
        return notification;
    }
}
