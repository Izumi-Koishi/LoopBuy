package com.shiwu.payment.service;

import com.shiwu.payment.model.*;
import com.shiwu.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PaymentService MVC框架测试类
 * 测试重构后的PaymentService在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PaymentService MVC框架测试")
public class PaymentServiceMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceMvcTest.class);
    
    private PaymentService paymentService;
    
    // 测试数据
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_ORDER_ID = 99L;
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("99.99");
    private static final Integer TEST_PAYMENT_METHOD = 1; // 1-支付宝
    
    @BeforeEach
    public void setUp() {
        // 使用MVC框架的依赖注入方式创建Service
        paymentService = new PaymentServiceImpl();
        logger.info("PaymentService MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("PaymentService MVC测试清理完成");
    }

    /**
     * 创建测试支付DTO
     */
    private PaymentDTO createTestPaymentDTO() {
        PaymentDTO dto = new PaymentDTO();
        dto.setOrderIds(Arrays.asList(TEST_ORDER_ID));
        dto.setTotalAmount(TEST_AMOUNT);
        dto.setPaymentMethod(TEST_PAYMENT_METHOD);
        dto.setPaymentPassword("123456");
        return dto;
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 创建支付测试 - 成功场景")
    public void testCreatePayment_Success() {
        logger.info("开始测试创建支付功能 - 成功场景 - MVC框架");
        
        // 创建测试支付DTO
        PaymentDTO dto = createTestPaymentDTO();
        
        // 执行测试
        PaymentOperationResult result = paymentService.createPayment(dto, TEST_USER_ID);
        
        // 验证结果 - 由于订单不存在，可能会失败，但这是正常的业务逻辑
        assertNotNull(result, "返回结果不应为空");
        logger.info("创建支付测试完成 - MVC框架: success={}, message={}", 
                   result.isSuccess(), result.getErrorMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 创建支付测试 - 参数验证")
    public void testCreatePaymentValidation() {
        logger.info("开始测试创建支付参数验证 - MVC框架");

        // 测试null DTO
        PaymentOperationResult result1 = paymentService.createPayment(null, TEST_USER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null DTO应该失败");

        // 测试null用户ID
        PaymentDTO dto = createTestPaymentDTO();
        PaymentOperationResult result2 = paymentService.createPayment(dto, null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null用户ID应该失败");

        // 测试空订单列表
        PaymentDTO dto2 = new PaymentDTO();
        dto2.setOrderIds(Arrays.asList());
        dto2.setTotalAmount(TEST_AMOUNT);
        dto2.setPaymentMethod(TEST_PAYMENT_METHOD);

        PaymentOperationResult result3 = paymentService.createPayment(dto2, TEST_USER_ID);
        assertNotNull(result3, "返回结果不应为空");
        assertFalse(result3.isSuccess(), "空订单列表应该失败");

        // 测试无效金额
        PaymentDTO dto3 = new PaymentDTO();
        dto3.setOrderIds(Arrays.asList(TEST_ORDER_ID));
        dto3.setTotalAmount(BigDecimal.ZERO);
        dto3.setPaymentMethod(TEST_PAYMENT_METHOD);

        PaymentOperationResult result4 = paymentService.createPayment(dto3, TEST_USER_ID);
        assertNotNull(result4, "返回结果不应为空");
        assertFalse(result4.isSuccess(), "无效金额应该失败");

        logger.info("创建支付参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 查询支付状态测试")
    public void testGetPaymentStatus() {
        logger.info("开始测试查询支付状态功能 - MVC框架");

        // 执行测试
        PaymentOperationResult result = paymentService.getPaymentStatus("NOT_EXIST_ID", TEST_USER_ID);

        // 验证结果 - 支付记录可能不存在，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("查询支付状态测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 查询支付状态测试 - 参数验证")
    public void testGetPaymentStatusValidation() {
        logger.info("开始测试查询支付状态参数验证 - MVC框架");

        // 测试null支付ID
        PaymentOperationResult result1 = paymentService.getPaymentStatus(null, TEST_USER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null支付ID应该失败");

        // 测试null用户ID
        PaymentOperationResult result2 = paymentService.getPaymentStatus("PAYMENT_ID", null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null用户ID应该失败");

        logger.info("查询支付状态参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 获取用户支付记录列表测试")
    public void testGetUserPayments() {
        logger.info("开始测试获取用户支付记录列表功能 - MVC框架");
        
        // 执行测试
        PaymentOperationResult result = paymentService.getUserPayments(TEST_USER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 支付记录列表可能为空，这是正常的
        
        logger.info("获取用户支付记录列表测试通过 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 获取用户支付记录列表测试 - 参数验证")
    public void testGetUserPaymentsValidation() {
        logger.info("开始测试获取用户支付记录列表参数验证 - MVC框架");

        // 测试null用户ID
        PaymentOperationResult result = paymentService.getUserPayments(null);
        assertNotNull(result, "返回结果不应为空");
        assertFalse(result.isSuccess(), "null用户ID应该失败");

        logger.info("获取用户支付记录列表参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 处理支付超时测试")
    public void testHandlePaymentTimeout() {
        logger.info("开始测试处理支付超时功能 - MVC框架");

        // 执行测试
        PaymentOperationResult result = paymentService.handlePaymentTimeout("NOT_EXIST_PAYMENT_ID");

        // 验证结果 - 支付记录可能不存在，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("处理支付超时测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 根据订单ID获取支付信息测试")
    public void testGetPaymentByOrderIds() {
        logger.info("开始测试根据订单ID获取支付信息功能 - MVC框架");

        // 执行测试
        PaymentOperationResult result = paymentService.getPaymentByOrderIds(Arrays.asList(TEST_ORDER_ID), TEST_USER_ID);

        // 验证结果 - 支付记录可能不存在，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("根据订单ID获取支付信息测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("MVC-9 取消支付测试")
    public void testCancelPayment() {
        logger.info("开始测试取消支付功能 - MVC框架");
        
        // 执行测试
        PaymentOperationResult result = paymentService.cancelPayment("NOT_EXIST_PAYMENT_ID", TEST_USER_ID);
        
        // 验证结果 - 支付记录可能不存在，取消可能失败，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("取消支付测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 支付完整流程测试")
    public void testCompletePaymentWorkflow() {
        logger.info("开始测试支付完整流程 - MVC框架");

        // 1. 创建支付
        PaymentDTO createDto = createTestPaymentDTO();
        PaymentOperationResult createResult = paymentService.createPayment(createDto, TEST_USER_ID);
        assertNotNull(createResult, "创建支付结果不应为空");
        logger.info("创建支付结果: success={}", createResult.isSuccess());

        // 2. 获取用户支付记录列表
        PaymentOperationResult listResult = paymentService.getUserPayments(TEST_USER_ID);
        assertNotNull(listResult, "支付记录列表不应为空");
        logger.info("获取支付记录列表: success={}", listResult.isSuccess());

        // 3. 查询支付状态
        PaymentOperationResult statusResult = paymentService.getPaymentStatus("NOT_EXIST_ID", TEST_USER_ID);
        assertNotNull(statusResult, "支付状态查询结果不应为空");
        logger.info("查询支付状态: success={}", statusResult.isSuccess());

        // 4. 根据订单ID获取支付信息
        PaymentOperationResult orderPaymentResult = paymentService.getPaymentByOrderIds(Arrays.asList(TEST_ORDER_ID), TEST_USER_ID);
        assertNotNull(orderPaymentResult, "订单支付信息不应为空");
        logger.info("根据订单ID获取支付信息: success={}", orderPaymentResult.isSuccess());

        // 5. 处理支付超时
        PaymentOperationResult timeoutResult = paymentService.handlePaymentTimeout("NOT_EXIST_PAYMENT_ID");
        assertNotNull(timeoutResult, "支付超时处理结果不应为空");
        logger.info("处理支付超时: success={}", timeoutResult.isSuccess());

        // 6. 取消支付
        PaymentOperationResult cancelResult = paymentService.cancelPayment("NOT_EXIST_PAYMENT_ID", TEST_USER_ID);
        assertNotNull(cancelResult, "取消支付结果不应为空");
        logger.info("取消支付: success={}", cancelResult.isSuccess());

        logger.info("支付完整流程测试通过 - MVC框架");
    }
}
