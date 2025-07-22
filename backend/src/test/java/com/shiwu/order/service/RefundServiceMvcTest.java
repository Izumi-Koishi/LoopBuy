package com.shiwu.order.service;

import com.shiwu.order.model.Order;
import com.shiwu.order.model.RefundTransaction;
import com.shiwu.order.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RefundService MVC框架测试类
 * 测试重构后的RefundService在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("RefundService MVC框架测试")
public class RefundServiceMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(RefundServiceMvcTest.class);
    
    private RefundService refundService;
    
    // 测试数据
    private static final Long TEST_ORDER_ID = 1L;
    private static final Long TEST_BUYER_ID = 1L;
    private static final Long TEST_SELLER_ID = 2L;
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("99.99");
    private static final String TEST_REASON = "商品质量问题";
    
    @BeforeEach
    public void setUp() {
        // 使用MVC框架的依赖注入方式创建Service
        refundService = new RefundServiceImpl();
        logger.info("RefundService MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        // 清理测试数据
        if (refundService instanceof RefundServiceImpl) {
            ((RefundServiceImpl) refundService).clearAllRefundTransactions();
        }
        logger.info("RefundService MVC测试清理完成");
    }

    /**
     * 创建测试订单
     */
    private Order createTestOrder() {
        Order order = new Order();
        order.setId(TEST_ORDER_ID);
        order.setBuyerId(TEST_BUYER_ID);
        order.setSellerId(TEST_SELLER_ID);
        order.setPriceAtPurchase(TEST_AMOUNT);
        order.setStatus(3); // 已完成状态
        return order;
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 处理退款测试 - 成功场景")
    public void testProcessRefund_Success() {
        logger.info("开始测试处理退款功能 - 成功场景 - MVC框架");
        
        // 创建测试订单
        Order order = createTestOrder();
        
        // 执行测试
        RefundTransaction result = refundService.processRefund(order, TEST_REASON);
        
        // 验证结果
        assertNotNull(result, "退款交易记录不应为空");
        assertNotNull(result.getRefundId(), "退款ID不应为空");
        assertEquals(TEST_ORDER_ID, result.getOrderId(), "订单ID应该正确");
        assertEquals(TEST_BUYER_ID, result.getBuyerId(), "买家ID应该正确");
        assertEquals(TEST_SELLER_ID, result.getSellerId(), "卖家ID应该正确");
        assertEquals(TEST_AMOUNT, result.getRefundAmount(), "退款金额应该正确");
        assertEquals(TEST_REASON, result.getReason(), "退款原因应该正确");
        assertNotNull(result.getCreateTime(), "创建时间不应为空");
        
        logger.info("测试处理退款功能 - 成功场景 - 通过: refundId={}, status={}", 
                   result.getRefundId(), result.getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 处理退款测试 - 订单为空")
    public void testProcessRefund_NullOrder() {
        logger.info("开始测试处理退款功能 - 订单为空 - MVC框架");
        
        // 执行测试
        RefundTransaction result = refundService.processRefund(null, TEST_REASON);
        
        // 验证结果
        assertNull(result, "订单为空时应该返回null");
        
        logger.info("测试处理退款功能 - 订单为空 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 根据退款ID查询退款记录测试")
    public void testGetRefundTransaction() {
        logger.info("开始测试根据退款ID查询退款记录功能 - MVC框架");
        
        // 先创建一个退款记录
        Order order = createTestOrder();
        RefundTransaction refundTransaction = refundService.processRefund(order, TEST_REASON);
        assertNotNull(refundTransaction, "退款记录创建应该成功");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundTransaction(refundTransaction.getRefundId());
        
        // 验证结果
        assertNotNull(result, "查询结果不应为空");
        assertEquals(refundTransaction.getRefundId(), result.getRefundId(), "退款ID应该匹配");
        assertEquals(refundTransaction.getOrderId(), result.getOrderId(), "订单ID应该匹配");
        assertEquals(refundTransaction.getRefundAmount(), result.getRefundAmount(), "退款金额应该匹配");
        
        logger.info("测试根据退款ID查询退款记录功能 - 通过: refundId={}", result.getRefundId());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 根据退款ID查询退款记录测试 - ID为空")
    public void testGetRefundTransaction_NullId() {
        logger.info("开始测试根据退款ID查询退款记录功能 - ID为空 - MVC框架");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundTransaction(null);
        
        // 验证结果
        assertNull(result, "ID为空时应该返回null");
        
        logger.info("测试根据退款ID查询退款记录功能 - ID为空 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 根据退款ID查询退款记录测试 - ID不存在")
    public void testGetRefundTransaction_NotFound() {
        logger.info("开始测试根据退款ID查询退款记录功能 - ID不存在 - MVC框架");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundTransaction("NOT_EXIST_ID");
        
        // 验证结果
        assertNull(result, "不存在的ID应该返回null");
        
        logger.info("测试根据退款ID查询退款记录功能 - ID不存在 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 根据订单ID查询退款记录测试")
    public void testGetRefundByOrderId() {
        logger.info("开始测试根据订单ID查询退款记录功能 - MVC框架");
        
        // 先创建一个退款记录
        Order order = createTestOrder();
        RefundTransaction refundTransaction = refundService.processRefund(order, TEST_REASON);
        assertNotNull(refundTransaction, "退款记录创建应该成功");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundByOrderId(TEST_ORDER_ID);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为空");
        assertEquals(refundTransaction.getRefundId(), result.getRefundId(), "退款ID应该匹配");
        assertEquals(TEST_ORDER_ID, result.getOrderId(), "订单ID应该匹配");
        assertEquals(refundTransaction.getRefundAmount(), result.getRefundAmount(), "退款金额应该匹配");
        
        logger.info("测试根据订单ID查询退款记录功能 - 通过: orderId={}, refundId={}", 
                   TEST_ORDER_ID, result.getRefundId());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 根据订单ID查询退款记录测试 - ID为空")
    public void testGetRefundByOrderId_NullId() {
        logger.info("开始测试根据订单ID查询退款记录功能 - ID为空 - MVC框架");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundByOrderId(null);
        
        // 验证结果
        assertNull(result, "ID为空时应该返回null");
        
        logger.info("测试根据订单ID查询退款记录功能 - ID为空 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 根据订单ID查询退款记录测试 - ID不存在")
    public void testGetRefundByOrderId_NotFound() {
        logger.info("开始测试根据订单ID查询退款记录功能 - ID不存在 - MVC框架");
        
        // 执行测试
        RefundTransaction result = refundService.getRefundByOrderId(999L);
        
        // 验证结果
        assertNull(result, "不存在的订单ID应该返回null");
        
        logger.info("测试根据订单ID查询退款记录功能 - ID不存在 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("MVC-9 多次退款测试")
    public void testMultipleRefunds() {
        logger.info("开始测试多次退款功能 - MVC框架");
        
        // 创建多个订单并处理退款
        for (int i = 1; i <= 3; i++) {
            Order order = createTestOrder();
            order.setId((long) i);
            
            RefundTransaction result = refundService.processRefund(order, "测试退款原因" + i);
            assertNotNull(result, "第" + i + "次退款应该成功");
            assertEquals((long) i, result.getOrderId(), "订单ID应该正确");
        }
        
        // 验证所有退款记录都能查询到
        for (int i = 1; i <= 3; i++) {
            RefundTransaction result = refundService.getRefundByOrderId((long) i);
            assertNotNull(result, "第" + i + "个退款记录应该能查询到");
            assertEquals((long) i, result.getOrderId(), "订单ID应该正确");
        }
        
        logger.info("测试多次退款功能 - 通过");
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 退款完整流程测试")
    public void testCompleteRefundWorkflow() {
        logger.info("开始测试退款完整流程 - MVC框架");
        
        // 1. 创建订单
        Order order = createTestOrder();
        
        // 2. 处理退款
        RefundTransaction refundResult = refundService.processRefund(order, TEST_REASON);
        assertNotNull(refundResult, "退款处理应该成功");
        assertNotNull(refundResult.getRefundId(), "退款ID不应为空");
        logger.info("退款处理完成: refundId={}", refundResult.getRefundId());
        
        // 3. 根据退款ID查询
        RefundTransaction queryByRefundId = refundService.getRefundTransaction(refundResult.getRefundId());
        assertNotNull(queryByRefundId, "根据退款ID查询应该成功");
        assertEquals(refundResult.getRefundId(), queryByRefundId.getRefundId(), "退款ID应该匹配");
        logger.info("根据退款ID查询成功: refundId={}", queryByRefundId.getRefundId());
        
        // 4. 根据订单ID查询
        RefundTransaction queryByOrderId = refundService.getRefundByOrderId(TEST_ORDER_ID);
        assertNotNull(queryByOrderId, "根据订单ID查询应该成功");
        assertEquals(TEST_ORDER_ID, queryByOrderId.getOrderId(), "订单ID应该匹配");
        assertEquals(refundResult.getRefundId(), queryByOrderId.getRefundId(), "应该是同一个退款记录");
        logger.info("根据订单ID查询成功: orderId={}, refundId={}", 
                   TEST_ORDER_ID, queryByOrderId.getRefundId());
        
        // 5. 验证退款记录的完整性
        assertEquals(TEST_BUYER_ID, queryByOrderId.getBuyerId(), "买家ID应该正确");
        assertEquals(TEST_SELLER_ID, queryByOrderId.getSellerId(), "卖家ID应该正确");
        assertEquals(TEST_AMOUNT, queryByOrderId.getRefundAmount(), "退款金额应该正确");
        assertEquals(TEST_REASON, queryByOrderId.getReason(), "退款原因应该正确");
        assertNotNull(queryByOrderId.getCreateTime(), "创建时间不应为空");
        
        logger.info("退款完整流程测试通过 - MVC框架");
    }
}
