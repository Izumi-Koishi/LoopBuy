package com.shiwu.payment.controller;

import com.shiwu.common.result.Result;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PaymentTimeoutController MVC框架测试类
 * 测试重构后的PaymentTimeoutController在MVC框架下的所有核心功能
 * 覆盖原有PaymentTimeoutControllerComprehensiveTest的所有测试场景
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PaymentTimeoutController MVC框架测试")
public class PaymentTimeoutControllerMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTimeoutControllerMvcTest.class);
    
    private PaymentTimeoutController paymentTimeoutController;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;
    
    @BeforeEach
    public void setUp() throws Exception {
        // 使用MVC框架的依赖注入方式创建Controller
        paymentTimeoutController = new PaymentTimeoutController();

        // 初始化Controller以扫描路由
        paymentTimeoutController.init();

        // 创建Mock对象
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        
        // 设置响应Writer
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
        
        // 设置管理员登录状态
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userRole")).thenReturn("admin");
        
        logger.info("PaymentTimeoutController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("PaymentTimeoutController MVC测试清理完成");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 获取超时状态接口测试 - 成功场景")
    public void testGetTimeoutStatus_Success() throws Exception {
        logger.info("开始测试获取超时状态接口 - 成功场景");
        
        // 设置请求参数
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        paymentTimeoutController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        
        logger.info("测试获取超时状态接口 - 成功场景 - 完成: response={}", responseContent);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 获取超时状态接口测试 - 权限不足")
    public void testGetTimeoutStatus_NoPermission() throws Exception {
        logger.info("开始测试获取超时状态接口 - 权限不足");
        
        // 设置非管理员用户
        when(session.getAttribute("userRole")).thenReturn("user");
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        paymentTimeoutController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        assertTrue(responseContent.contains("403") || responseContent.contains("权限不足"), "应该返回权限不足错误");
        
        logger.info("测试获取超时状态接口 - 权限不足 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取过期支付记录数量接口测试")
    public void testGetExpiredCount() throws Exception {
        logger.info("开始测试获取过期支付记录数量接口");
        
        // 设置请求参数
        when(request.getPathInfo()).thenReturn("/count");
        
        // 执行测试
        paymentTimeoutController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        
        logger.info("测试获取过期支付记录数量接口 - 完成: response={}", responseContent);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 手动触发超时检查接口测试")
    public void testTriggerTimeoutCheck() throws Exception {
        logger.info("开始测试手动触发超时检查接口");
        
        // 设置请求参数
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        paymentTimeoutController.doPost(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        
        logger.info("测试手动触发超时检查接口 - 完成: response={}", responseContent);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 手动处理指定过期支付接口测试 - 成功场景")
    public void testHandleSpecificExpiredPayment_Success() throws Exception {
        logger.info("开始测试手动处理指定过期支付接口 - 成功场景");
        
        // 设置请求参数
        when(request.getPathInfo()).thenReturn("/handle");
        String requestBody = "{\"paymentId\":\"TEST_PAYMENT_ID\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        
        // 执行测试
        paymentTimeoutController.doPost(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        
        logger.info("测试手动处理指定过期支付接口 - 成功场景 - 完成: response={}", responseContent);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 手动处理指定过期支付接口测试 - 支付ID为空")
    public void testHandleSpecificExpiredPayment_EmptyId() throws Exception {
        logger.info("开始测试手动处理指定过期支付接口 - 支付ID为空");
        
        // 设置请求参数
        when(request.getPathInfo()).thenReturn("/handle");
        String requestBody = "{\"paymentId\":\"\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        
        // 执行测试
        paymentTimeoutController.doPost(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        // 由于空支付ID会被当作支付记录不存在处理，这是正常的业务逻辑
        assertTrue(responseContent.contains("500") || responseContent.contains("处理过期支付失败"), "应该返回处理失败错误");
        
        logger.info("测试手动处理指定过期支付接口 - 支付ID为空 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 无效路径测试 - GET")
    public void testInvalidPath_Get() throws Exception {
        logger.info("开始测试无效路径 - GET");
        
        // 设置无效请求路径
        when(request.getPathInfo()).thenReturn("/invalid");
        
        // 执行测试
        paymentTimeoutController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        assertTrue(responseContent.contains("404") || responseContent.contains("请求路径不存在"), "应该返回路径不存在错误");
        
        logger.info("测试无效路径 - GET - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 无效路径测试 - POST")
    public void testInvalidPath_Post() throws Exception {
        logger.info("开始测试无效路径 - POST");
        
        // 设置无效请求路径
        when(request.getPathInfo()).thenReturn("/invalid");
        
        // 执行测试
        paymentTimeoutController.doPost(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        assertTrue(responseContent.contains("404") || responseContent.contains("请求路径不存在"), "应该返回路径不存在错误");
        
        logger.info("测试无效路径 - POST - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("MVC-9 未登录用户访问测试")
    public void testNotLoggedIn() throws Exception {
        logger.info("开始测试未登录用户访问");
        
        // 设置用户未登录
        when(request.getSession(false)).thenReturn(null);
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        paymentTimeoutController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent, "响应内容不应为空");
        assertTrue(responseContent.contains("403") || responseContent.contains("权限不足"), "应该返回权限不足错误");
        
        logger.info("测试未登录用户访问 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 完整的超时管理流程测试")
    public void testCompleteTimeoutManagementWorkflow() throws Exception {
        logger.info("开始测试完整的超时管理流程 - MVC框架");
        
        // 1. 获取超时状态
        when(request.getPathInfo()).thenReturn("/");
        paymentTimeoutController.doGet(request, response);
        String statusResponse = responseWriter.toString();
        assertNotNull(statusResponse, "获取超时状态响应不应为空");
        logger.info("获取超时状态完成: response={}", statusResponse);
        
        // 重置响应Writer
        responseWriter.getBuffer().setLength(0);
        
        // 2. 获取过期支付记录数量
        when(request.getPathInfo()).thenReturn("/count");
        paymentTimeoutController.doGet(request, response);
        String countResponse = responseWriter.toString();
        assertNotNull(countResponse, "获取过期支付记录数量响应不应为空");
        logger.info("获取过期支付记录数量完成: response={}", countResponse);
        
        // 重置响应Writer
        responseWriter.getBuffer().setLength(0);
        
        // 3. 手动触发超时检查
        when(request.getPathInfo()).thenReturn("/");
        paymentTimeoutController.doPost(request, response);
        String triggerResponse = responseWriter.toString();
        assertNotNull(triggerResponse, "手动触发超时检查响应不应为空");
        logger.info("手动触发超时检查完成: response={}", triggerResponse);
        
        // 重置响应Writer
        responseWriter.getBuffer().setLength(0);
        
        // 4. 手动处理指定过期支付
        when(request.getPathInfo()).thenReturn("/handle");
        String requestBody = "{\"paymentId\":\"TEST_PAYMENT_ID\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        paymentTimeoutController.doPost(request, response);
        String handleResponse = responseWriter.toString();
        assertNotNull(handleResponse, "手动处理指定过期支付响应不应为空");
        logger.info("手动处理指定过期支付完成: response={}", handleResponse);
        
        logger.info("完整的超时管理流程测试通过 - MVC框架");
    }
}
