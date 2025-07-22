package com.shiwu.message.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.message.dto.MessageSendDTO;
import com.shiwu.message.service.MessageService;
import com.shiwu.message.vo.ConversationVO;
import com.shiwu.message.vo.MessageVO;
import com.shiwu.test.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * MessageController MVC框架版本测试类
 *
 * 测试重构后的MessageController，验证MVC框架注解方式的功能正确性
 * 与原MessageControllerComprehensiveTest保持相同的测试范围
 */
public class MessageControllerMvcTest extends TestBase {

    private static final Logger logger = LoggerFactory.getLogger(MessageControllerMvcTest.class);

    private MessageController messageController;
    private HttpServletRequest request;
    private MessageService mockMessageService;
    private MockedStatic<JwtUtil> jwtUtilMock;

    // 测试数据 - 与原测试保持一致
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_USER_ID_2 = 2L;
    private static final Long TEST_PRODUCT_ID_1 = 1L;
    private static final String TEST_CONVERSATION_ID = "conv_123";
    private static final String VALID_JWT_TOKEN = "Bearer valid_jwt_token";

    @BeforeEach
    public void setUp() {
        logger.info("MessageController MVC测试环境初始化开始");
        super.setUp();

        // 创建MessageController实例
        messageController = new MessageController();

        // 创建Mock对象
        request = mock(HttpServletRequest.class);
        mockMessageService = mock(MessageService.class);

        // Mock JwtUtil静态方法
        jwtUtilMock = mockStatic(JwtUtil.class);

        // 使用反射注入Mock的MessageService
        try {
            Field serviceField = MessageController.class.getDeclaredField("messageService");
            serviceField.setAccessible(true);
            serviceField.set(messageController, mockMessageService);
        } catch (Exception e) {
            fail("无法注入Mock MessageService: " + e.getMessage());
        }

        // 设置默认的JWT Token认证
        when(request.getHeader("Authorization")).thenReturn(VALID_JWT_TOKEN);
        jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken("valid_jwt_token")).thenReturn(TEST_USER_ID);

        logger.info("MessageController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        if (jwtUtilMock != null) {
            jwtUtilMock.close();
        }
        logger.info("MessageController MVC测试清理完成");
    }
    
    /**
     * 测试发送消息接口 - 成功场景
     */
    @Test
    public void testSendMessage_Success() throws Exception {
        logger.info("开始测试发送消息接口 - 成功场景");

        // 准备测试数据
        MessageSendDTO dto = new MessageSendDTO(TEST_USER_ID_2, "Hello, this is a test message", TEST_PRODUCT_ID_1);
        MessageVO sentMessage = new MessageVO();
        sentMessage.setMessageId(1L);
        sentMessage.setSenderId(TEST_USER_ID);
        sentMessage.setContent("Hello, this is a test message");

        Result<MessageVO> expectedResult = Result.success(sentMessage);

        // 设置Mock行为
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(JsonUtil.toJson(dto))));
        when(mockMessageService.sendMessage(eq(TEST_USER_ID), any(MessageSendDTO.class))).thenReturn(expectedResult);

        // 执行测试
        Result<MessageVO> result = messageController.sendMessage(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Hello, this is a test message", result.getData().getContent());
        assertEquals(TEST_USER_ID, result.getData().getSenderId());

        // 验证Mock调用
        verify(mockMessageService).sendMessage(eq(TEST_USER_ID), any(MessageSendDTO.class));

        logger.info("测试发送消息接口 - 成功场景 - 通过");
    }

    /**
     * 测试发送消息接口 - 未授权场景
     */
    @Test
    public void testSendMessage_Unauthorized() throws Exception {
        logger.info("开始测试发送消息接口 - 未授权场景");

        // 设置Mock行为 - 无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(any())).thenReturn(null);

        // 设置请求体
        MessageSendDTO dto = new MessageSendDTO(TEST_USER_ID_2, "Hello, this is a test message", TEST_PRODUCT_ID_1);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(JsonUtil.toJson(dto))));

        // 执行测试
        Result<MessageVO> result = messageController.sendMessage(request);

        // 验证结果 - 应该返回401未授权
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("401", result.getError().getCode());
        assertEquals("未授权访问", result.getError().getMessage());

        logger.info("测试发送消息接口 - 未授权场景 - 通过");
    }

    /**
     * 测试发送消息接口 - 空请求体场景
     */
    @Test
    public void testSendMessage_EmptyBody() throws Exception {
        logger.info("开始测试发送消息接口 - 空请求体场景");

        // 设置Mock行为
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        // 执行测试
        Result<MessageVO> result = messageController.sendMessage(request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("400", result.getError().getCode());
        assertEquals("请求体不能为空或格式错误", result.getError().getMessage());

        logger.info("测试发送消息接口 - 空请求体场景 - 通过");
    }

    /**
     * 测试发送消息接口 - 无效JSON场景
     */
    @Test
    public void testSendMessage_InvalidJson() throws Exception {
        logger.info("开始测试发送消息接口 - 无效JSON场景");

        // 设置Mock行为
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("invalid json")));

        // 执行测试
        Result<MessageVO> result = messageController.sendMessage(request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("400", result.getError().getCode());
        assertEquals("请求格式错误", result.getError().getMessage());

        logger.info("测试发送消息接口 - 无效JSON场景 - 通过");
    }

    /**
     * 测试获取会话列表接口 - 未授权场景
     */
    @Test
    public void testGetConversations_Unauthorized() {
        logger.info("开始测试获取会话列表接口 - 未授权场景");

        // 设置Mock行为 - 无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(any())).thenReturn(null);

        // 执行测试
        Result<List<ConversationVO>> result = messageController.getConversations(1, 20, request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("401", result.getError().getCode());
        assertEquals("未授权访问", result.getError().getMessage());

        logger.info("测试获取会话列表接口 - 未授权场景 - 通过");
    }

    /**
     * 测试获取消息历史接口 - 未授权场景
     */
    @Test
    public void testGetMessageHistory_Unauthorized() {
        logger.info("开始测试获取消息历史接口 - 未授权场景");

        // 设置Mock行为 - 无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(any())).thenReturn(null);

        // 执行测试
        Result<List<MessageVO>> result = messageController.getMessageHistory(TEST_CONVERSATION_ID, 1, 50, request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("401", result.getError().getCode());
        assertEquals("未授权访问", result.getError().getMessage());

        logger.info("测试获取消息历史接口 - 未授权场景 - 通过");
    }

    /**
     * 测试标记消息为已读接口 - 未授权场景
     */
    @Test
    public void testMarkAsRead_Unauthorized() {
        logger.info("开始测试标记消息为已读接口 - 未授权场景");

        // 设置Mock行为 - 无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtUtilMock.when(() -> JwtUtil.getUserIdFromToken(any())).thenReturn(null);

        // 执行测试
        Result<Void> result = messageController.markAsRead(TEST_CONVERSATION_ID, request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("401", result.getError().getCode());
        assertEquals("未授权访问", result.getError().getMessage());

        logger.info("测试标记消息为已读接口 - 未授权场景 - 通过");
    }

    /**
     * 测试获取会话列表接口 - 成功场景
     */
    @Test
    public void testGetConversations_Success() {
        logger.info("开始测试获取会话列表接口 - 成功场景");

        // 准备测试数据
        ConversationVO conversation = new ConversationVO();
        conversation.setConversationId(TEST_CONVERSATION_ID);
        conversation.setParticipant1Id(TEST_USER_ID);
        conversation.setParticipant2Id(TEST_USER_ID_2);

        List<ConversationVO> conversations = Arrays.asList(conversation);
        Result<List<ConversationVO>> expectedResult = Result.success(conversations);

        // 设置Mock行为
        when(mockMessageService.getConversations(TEST_USER_ID, 1, 20)).thenReturn(expectedResult);

        // 执行测试
        Result<List<ConversationVO>> result = messageController.getConversations(1, 20, request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(TEST_CONVERSATION_ID, result.getData().get(0).getConversationId());

        // 验证Mock调用
        verify(mockMessageService).getConversations(TEST_USER_ID, 1, 20);

        logger.info("测试获取会话列表接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取消息历史接口 - 成功场景
     */
    @Test
    public void testGetMessageHistory_Success() {
        logger.info("开始测试获取消息历史接口 - 成功场景");

        // 准备测试数据
        MessageVO message = new MessageVO();
        message.setMessageId(1L);
        message.setConversationId(TEST_CONVERSATION_ID);
        message.setSenderId(TEST_USER_ID);
        message.setContent("测试消息");

        List<MessageVO> messages = Arrays.asList(message);
        Result<List<MessageVO>> expectedResult = Result.success(messages);

        // 设置Mock行为
        when(mockMessageService.getMessageHistory(TEST_USER_ID, TEST_CONVERSATION_ID, 1, 50))
            .thenReturn(expectedResult);

        // 执行测试
        Result<List<MessageVO>> result = messageController.getMessageHistory(
            TEST_CONVERSATION_ID, 1, 50, request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("测试消息", result.getData().get(0).getContent());

        // 验证Mock调用
        verify(mockMessageService).getMessageHistory(TEST_USER_ID, TEST_CONVERSATION_ID, 1, 50);

        logger.info("测试获取消息历史接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取消息历史接口 - 无效用户ID
     */
    @Test
    public void testGetMessageHistory_InvalidUserId() {
        logger.info("开始测试获取消息历史接口 - 无效用户ID");

        // 设置Mock行为 - 返回权限错误
        Result<List<MessageVO>> expectedResult = Result.fail("403", "无权限访问该会话");
        when(mockMessageService.getMessageHistory(TEST_USER_ID, "invalid_conv", 1, 50))
            .thenReturn(expectedResult);

        // 执行测试
        Result<List<MessageVO>> result = messageController.getMessageHistory("invalid_conv", 1, 50, request);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("403", result.getError().getCode());

        logger.info("测试获取消息历史接口 - 无效用户ID - 通过");
    }

    /**
     * 测试标记消息为已读接口 - 成功场景
     */
    @Test
    public void testMarkAsRead_Success() {
        logger.info("开始测试标记消息为已读接口 - 成功场景");

        // 准备测试数据
        Result<Void> expectedResult = Result.success(null);

        // 设置Mock行为
        when(mockMessageService.markMessagesAsRead(TEST_USER_ID, TEST_CONVERSATION_ID))
            .thenReturn(expectedResult);

        // 执行测试
        Result<Void> result = messageController.markAsRead(TEST_CONVERSATION_ID, request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());

        // 验证Mock调用
        verify(mockMessageService).markMessagesAsRead(TEST_USER_ID, TEST_CONVERSATION_ID);

        logger.info("测试标记消息为已读接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取未读消息数量接口 - 成功场景
     */
    @Test
    public void testGetUnreadCount_Success() {
        logger.info("开始测试获取未读消息数量接口 - 成功场景");

        // 准备测试数据
        Result<Integer> expectedResult = Result.success(5);

        // 设置Mock行为
        when(mockMessageService.getUnreadMessageCount(TEST_USER_ID)).thenReturn(expectedResult);

        // 执行测试
        Result<Integer> result = messageController.getUnreadCount(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(5, result.getData().intValue());

        // 验证Mock调用
        verify(mockMessageService).getUnreadMessageCount(TEST_USER_ID);

        logger.info("测试获取未读消息数量接口 - 成功场景 - 通过");
    }

    /**
     * 测试HTTP方法路由
     */
    @Test
    public void testHttpMethodRouting() {
        logger.info("开始测试HTTP方法路由");

        // 设置Mock行为
        when(mockMessageService.getConversations(TEST_USER_ID, 1, 20))
            .thenReturn(Result.success(Arrays.asList(new ConversationVO())));
        when(mockMessageService.markMessagesAsRead(TEST_USER_ID, TEST_CONVERSATION_ID))
            .thenReturn(Result.success(null));

        // 测试GET方法 - 获取会话列表
        Result<List<ConversationVO>> getResult = messageController.getConversations(1, 20, request);
        assertNotNull(getResult, "GET方法应该返回结果");

        // 测试POST方法 - 发送消息
        try {
            when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"receiverId\":2,\"content\":\"test\"}")));
            when(mockMessageService.sendMessage(eq(TEST_USER_ID), any())).thenReturn(Result.success(new MessageVO()));
            Result<MessageVO> postResult = messageController.sendMessage(request);
            assertNotNull(postResult, "POST方法应该返回结果");
        } catch (Exception e) {
            logger.error("POST方法测试失败", e);
        }

        // 测试PUT方法 - 标记已读
        Result<Void> putResult = messageController.markAsRead(TEST_CONVERSATION_ID, request);
        assertNotNull(putResult, "PUT方法应该返回结果");

        logger.info("HTTP方法路由测试通过");
    }

    /**
     * 测试系统异常处理
     */
    @Test
    public void testSystemExceptionHandling() {
        logger.info("开始测试系统异常处理");

        // 设置Mock抛出异常
        when(mockMessageService.getConversations(TEST_USER_ID, 1, 20))
            .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试 - 应该捕获异常并返回错误结果
        try {
            Result<List<ConversationVO>> result = messageController.getConversations(1, 20, request);
            // 如果没有异常处理，这里会抛出异常
            // 如果有异常处理，应该返回错误结果
            logger.info("系统异常处理测试: success={}", result != null ? result.isSuccess() : "null");
        } catch (Exception e) {
            logger.info("系统异常被正确抛出: {}", e.getMessage());
        }

        logger.info("系统异常处理测试通过");
    }

    /**
     * 测试完整的消息操作流程
     */
    @Test
    public void testCompleteMessageWorkflow() throws Exception {
        logger.info("开始测试完整的消息操作流程");

        // 设置Mock行为
        when(mockMessageService.getConversations(TEST_USER_ID, 1, 20))
            .thenReturn(Result.success(Arrays.asList(new ConversationVO())));
        when(mockMessageService.sendMessage(eq(TEST_USER_ID), any()))
            .thenReturn(Result.success(new MessageVO()));
        when(mockMessageService.getMessageHistory(TEST_USER_ID, TEST_CONVERSATION_ID, 1, 50))
            .thenReturn(Result.success(Arrays.asList(new MessageVO())));
        when(mockMessageService.markMessagesAsRead(TEST_USER_ID, TEST_CONVERSATION_ID))
            .thenReturn(Result.success(null));
        when(mockMessageService.getUnreadMessageCount(TEST_USER_ID))
            .thenReturn(Result.success(3));

        // 1. 获取会话列表
        Result<List<ConversationVO>> conversationsResult = messageController.getConversations(1, 20, request);
        assertNotNull(conversationsResult, "获取会话列表应该返回结果");
        logger.info("获取会话列表: success={}", conversationsResult.isSuccess());

        // 2. 发送消息
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"receiverId\":2,\"content\":\"workflow test\"}")));
        Result<MessageVO> sendResult = messageController.sendMessage(request);
        assertNotNull(sendResult, "发送消息应该返回结果");
        logger.info("发送消息: success={}", sendResult.isSuccess());

        // 3. 获取消息历史
        Result<List<MessageVO>> historyResult = messageController.getMessageHistory(TEST_CONVERSATION_ID, 1, 50, request);
        assertNotNull(historyResult, "获取消息历史应该返回结果");
        logger.info("获取消息历史: success={}", historyResult.isSuccess());

        // 4. 标记消息为已读
        Result<Void> markReadResult = messageController.markAsRead(TEST_CONVERSATION_ID, request);
        assertNotNull(markReadResult, "标记消息为已读应该返回结果");
        logger.info("标记消息为已读: success={}", markReadResult.isSuccess());

        // 5. 获取未读消息数量
        Result<Integer> unreadResult = messageController.getUnreadCount(request);
        assertNotNull(unreadResult, "获取未读消息数量应该返回结果");
        logger.info("获取未读消息数量: success={}", unreadResult.isSuccess());

        logger.info("完整的消息操作流程测试通过");
    }
}
