package com.shiwu.message.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.message.dto.MessagePollDTO;
import com.shiwu.message.service.RealtimeMessageService;
import com.shiwu.message.vo.MessagePollVO;
import com.shiwu.test.TestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealtimeMessageController MVC框架版本测试类
 * 
 * 测试重构后的RealtimeMessageController，验证MVC框架注解方式的功能正确性
 * 与原RealtimeMessageControllerComprehensiveTest保持相同的测试范围
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("RealtimeMessageController MVC框架测试")
public class RealtimeMessageControllerMvcTest extends TestBase {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeMessageControllerMvcTest.class);
    
    private RealtimeMessageController realtimeMessageController;
    private HttpServletRequest request;
    private RealtimeMessageService mockRealtimeMessageService;
    
    // 测试数据
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_LAST_MESSAGE_TIME = System.currentTimeMillis() - 60000L;
    private static final Integer TEST_TIMEOUT = 30;

    // 生成有效的JWT token
    private static final String TEST_JWT_TOKEN = "Bearer " + JwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);
    
    @BeforeEach
    public void setUp() {
        logger.info("RealtimeMessageController MVC测试环境初始化开始");
        super.setUp();
        
        // 创建RealtimeMessageController实例
        realtimeMessageController = new RealtimeMessageController();
        
        // 创建Mock对象
        request = mock(HttpServletRequest.class);
        mockRealtimeMessageService = mock(RealtimeMessageService.class);
        
        // 使用反射注入Mock的RealtimeMessageService
        try {
            Field serviceField = RealtimeMessageController.class.getDeclaredField("realtimeMessageService");
            serviceField.setAccessible(true);
            serviceField.set(realtimeMessageController, mockRealtimeMessageService);
        } catch (Exception e) {
            logger.error("注入Mock RealtimeMessageService失败", e);
        }
        
        // 设置默认Mock行为
        when(request.getHeader("Authorization")).thenReturn(TEST_JWT_TOKEN);
        
        logger.info("RealtimeMessageController MVC测试环境初始化完成");
    }
    
    @AfterEach
    public void tearDown() {
        logger.info("RealtimeMessageController MVC测试清理完成");
    }
    
    /**
     * 测试轮询消息接口 - 成功场景
     */
    @Test
    @Order(1)
    @DisplayName("11.1 轮询消息接口 - 成功场景")
    public void testPollMessages_Success() {
        logger.info("开始测试轮询消息接口 - 成功场景");
        
        // 准备测试数据
        MessagePollVO pollVO = new MessagePollVO();
        pollVO.setHasNewMessages(true);
        pollVO.setTotalUnreadCount(5);

        Result<MessagePollVO> expectedResult = Result.success(pollVO);

        // 设置Mock行为
        when(mockRealtimeMessageService.pollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class)))
            .thenReturn(expectedResult);

        // 执行测试
        Result<MessagePollVO> result = realtimeMessageController.pollMessages(
            TEST_LAST_MESSAGE_TIME, TEST_TIMEOUT, request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getHasNewMessages());
        assertEquals(5, result.getData().getTotalUnreadCount().intValue());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).pollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class));
        
        logger.info("测试轮询消息接口 - 成功场景 - 通过");
    }
    
    /**
     * 测试轮询消息接口 - 未授权场景
     */
    @Test
    @Order(2)
    @DisplayName("11.2 轮询消息接口 - 未授权场景")
    public void testPollMessages_Unauthorized() {
        logger.info("开始测试轮询消息接口 - 未授权场景");
        
        // 设置Mock行为 - 无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // 执行测试
        Result<MessagePollVO> result = realtimeMessageController.pollMessages(
            TEST_LAST_MESSAGE_TIME, TEST_TIMEOUT, request);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("401", result.getError().getCode());
        assertEquals("未授权访问", result.getError().getMessage());
        
        // 验证Mock未被调用
        verify(mockRealtimeMessageService, never()).pollNewMessages(any(), any());
        
        logger.info("测试轮询消息接口 - 未授权场景 - 通过");
    }
    
    /**
     * 测试长轮询消息接口 - 成功场景
     */
    @Test
    @Order(3)
    @DisplayName("11.3 长轮询消息接口 - 成功场景")
    public void testLongPollMessages_Success() {
        logger.info("开始测试长轮询消息接口 - 成功场景");
        
        // 准备测试数据
        MessagePollVO pollVO = new MessagePollVO();
        pollVO.setHasNewMessages(false);
        pollVO.setTotalUnreadCount(0);

        Result<MessagePollVO> expectedResult = Result.success(pollVO);

        // 设置Mock行为
        when(mockRealtimeMessageService.longPollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class), eq(60)))
            .thenReturn(expectedResult);

        // 执行测试
        Result<MessagePollVO> result = realtimeMessageController.longPollMessages(
            TEST_LAST_MESSAGE_TIME, 60, request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().getHasNewMessages());
        assertEquals(0, result.getData().getTotalUnreadCount().intValue());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).longPollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class), eq(60));
        
        logger.info("测试长轮询消息接口 - 成功场景 - 通过");
    }
    
    /**
     * 测试获取实时状态接口 - 成功场景
     */
    @Test
    @Order(4)
    @DisplayName("11.4 获取实时状态接口 - 成功场景")
    public void testGetRealtimeStatus_Success() {
        logger.info("开始测试获取实时状态接口 - 成功场景");
        
        // 准备测试数据
        MessagePollVO statusVO = new MessagePollVO();
        statusVO.setTotalUnreadCount(3);
        statusVO.setHasNewMessages(true);

        Result<MessagePollVO> expectedResult = Result.success(statusVO);

        // 设置Mock行为
        when(mockRealtimeMessageService.getUserRealtimeStatus(TEST_USER_ID))
            .thenReturn(expectedResult);

        // 执行测试
        Result<MessagePollVO> result = realtimeMessageController.getRealtimeStatus(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getTotalUnreadCount().intValue());
        assertTrue(result.getData().getHasNewMessages());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).getUserRealtimeStatus(TEST_USER_ID);
        
        logger.info("测试获取实时状态接口 - 成功场景 - 通过");
    }
    
    /**
     * 测试检查新消息接口 - 成功场景
     */
    @Test
    @Order(5)
    @DisplayName("11.5 检查新消息接口 - 成功场景")
    public void testCheckNewMessages_Success() {
        logger.info("开始测试检查新消息接口 - 成功场景");
        
        // 准备测试数据
        Result<Boolean> expectedResult = Result.success(true);
        
        // 设置Mock行为
        when(mockRealtimeMessageService.hasNewMessages(TEST_USER_ID, TEST_LAST_MESSAGE_TIME))
            .thenReturn(expectedResult);
        
        // 执行测试
        Result<Boolean> result = realtimeMessageController.checkNewMessages(TEST_LAST_MESSAGE_TIME, request);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).hasNewMessages(TEST_USER_ID, TEST_LAST_MESSAGE_TIME);
        
        logger.info("测试检查新消息接口 - 成功场景 - 通过");
    }
    
    /**
     * 测试获取在线用户数量接口 - 成功场景
     */
    @Test
    @Order(6)
    @DisplayName("11.6 获取在线用户数量接口 - 成功场景")
    public void testGetOnlineCount_Success() {
        logger.info("开始测试获取在线用户数量接口 - 成功场景");
        
        // 设置Mock行为
        when(mockRealtimeMessageService.getOnlineUserCount()).thenReturn(42);
        
        // 执行测试
        Result<Integer> result = realtimeMessageController.getOnlineCount();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(42, result.getData().intValue());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).getOnlineUserCount();
        
        logger.info("测试获取在线用户数量接口 - 成功场景 - 通过");
    }
    
    /**
     * 测试POST方法轮询消息接口
     */
    @Test
    @Order(7)
    @DisplayName("11.7 POST方法轮询消息接口")
    public void testPollMessagesPost_Success() {
        logger.info("开始测试POST方法轮询消息接口");
        
        // 准备测试数据
        MessagePollVO pollVO = new MessagePollVO();
        pollVO.setHasNewMessages(true);
        pollVO.setTotalUnreadCount(2);

        Result<MessagePollVO> expectedResult = Result.success(pollVO);

        // 设置Mock行为
        when(mockRealtimeMessageService.pollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class)))
            .thenReturn(expectedResult);

        // 执行测试
        Result<MessagePollVO> result = realtimeMessageController.pollMessagesPost(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getHasNewMessages());
        assertEquals(2, result.getData().getTotalUnreadCount().intValue());
        
        // 验证Mock调用
        verify(mockRealtimeMessageService).pollNewMessages(eq(TEST_USER_ID), any(MessagePollDTO.class));
        
        logger.info("测试POST方法轮询消息接口 - 通过");
    }
}
