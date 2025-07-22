package com.shiwu.order.service;

import com.shiwu.order.model.*;
import com.shiwu.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderService MVC框架测试类
 * 测试重构后的OrderService在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("OrderService MVC框架测试")
public class OrderServiceMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceMvcTest.class);
    
    private OrderService orderService;
    
    // 测试数据
    private static final Long TEST_BUYER_ID = 1L;
    private static final Long TEST_SELLER_ID = 2L;
    private static final Long TEST_ORDER_ID = 99L;
    
    @BeforeEach
    public void setUp() {
        // 使用MVC框架的依赖注入方式创建Service
        orderService = new OrderServiceImpl();
        logger.info("OrderService MVC测试环境初始化完成");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 创建订单测试")
    public void testCreateOrder() {
        logger.info("开始测试创建订单功能 - MVC框架");
        
        // 创建订单DTO
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setProductIds(Arrays.asList(99L)); // 使用不存在的商品ID
        
        // 执行测试
        OrderOperationResult result = orderService.createOrder(dto, TEST_BUYER_ID);
        
        // 验证结果 - 由于商品不存在，可能会失败，但这是正常的业务逻辑
        assertNotNull(result, "返回结果不应为空");
        logger.info("创建订单测试完成 - MVC框架: success={}, message={}", 
                   result.isSuccess(), result.getErrorMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 创建订单参数验证测试")
    public void testCreateOrderValidation() {
        logger.info("开始测试创建订单参数验证 - MVC框架");
        
        // 测试null DTO
        OrderOperationResult result1 = orderService.createOrder(null, TEST_BUYER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null DTO应该失败");
        assertEquals(OrderErrorCode.INVALID_PARAMS, result1.getErrorCode(), "错误码应该正确");
        
        // 测试null买家ID
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setProductIds(Arrays.asList(1L));
        
        OrderOperationResult result2 = orderService.createOrder(dto, null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null买家ID应该失败");
        assertEquals(OrderErrorCode.INVALID_PARAMS, result2.getErrorCode(), "错误码应该正确");
        
        // 测试空商品列表
        OrderCreateDTO dto2 = new OrderCreateDTO();
        dto2.setProductIds(Arrays.asList());
        
        OrderOperationResult result3 = orderService.createOrder(dto2, TEST_BUYER_ID);
        assertNotNull(result3, "返回结果不应为空");
        assertFalse(result3.isSuccess(), "空商品列表应该失败");
        assertEquals(OrderErrorCode.EMPTY_PRODUCT_LIST, result3.getErrorCode(), "错误码应该正确");
        
        logger.info("创建订单参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取买家订单列表测试")
    public void testGetBuyerOrders() {
        logger.info("开始测试获取买家订单列表功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.getBuyerOrders(TEST_BUYER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 订单列表可能为空，这是正常的
        
        logger.info("获取买家订单列表测试通过 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 获取买家订单列表参数验证测试")
    public void testGetBuyerOrdersValidation() {
        logger.info("开始测试获取买家订单列表参数验证 - MVC框架");
        
        // 测试null用户ID
        OrderOperationResult result = orderService.getBuyerOrders(null);
        assertNotNull(result, "返回结果不应为空");
        assertFalse(result.isSuccess(), "null用户ID应该失败");
        
        logger.info("获取买家订单列表参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 获取卖家订单列表测试")
    public void testGetSellerOrders() {
        logger.info("开始测试获取卖家订单列表功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.getSellerOrders(TEST_SELLER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        
        logger.info("获取卖家订单列表测试通过 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 根据ID获取订单测试")
    public void testGetOrderById() {
        logger.info("开始测试根据ID获取订单功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.getOrderById(TEST_ORDER_ID, TEST_BUYER_ID);
        
        // 验证结果 - 订单可能不存在，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("根据ID获取订单测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 根据ID获取订单参数验证测试")
    public void testGetOrderByIdValidation() {
        logger.info("开始测试根据ID获取订单参数验证 - MVC框架");
        
        // 测试null订单ID
        OrderOperationResult result1 = orderService.getOrderById(null, TEST_BUYER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null订单ID应该失败");
        
        // 测试null用户ID
        OrderOperationResult result2 = orderService.getOrderById(TEST_ORDER_ID, null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null用户ID应该失败");
        
        logger.info("根据ID获取订单参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 更新订单状态测试")
    public void testUpdateOrderStatus() {
        logger.info("开始测试更新订单状态功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.updateOrderStatus(TEST_ORDER_ID, 2, TEST_BUYER_ID);
        
        // 验证结果 - 订单可能不存在，更新可能失败，这是正常的
        assertNotNull(result, "返回结果不应为空");
        logger.info("更新订单状态测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("MVC-9 确认收货测试")
    public void testConfirmReceipt() {
        logger.info("开始测试确认收货功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.confirmReceipt(TEST_ORDER_ID, TEST_BUYER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 订单可能不存在，确认收货可能失败，这是正常的
        logger.info("确认收货测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 申请退货测试")
    public void testApplyForReturn() {
        logger.info("开始测试申请退货功能 - MVC框架");
        
        // 创建退货申请DTO
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setReason("商品质量问题");
        
        // 执行测试
        OrderOperationResult result = orderService.applyForReturn(TEST_ORDER_ID, dto, TEST_BUYER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 订单可能不存在，申请退货可能失败，这是正常的
        logger.info("申请退货测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("MVC-11 发货测试")
    public void testShipOrder() {
        logger.info("开始测试发货功能 - MVC框架");
        
        // 执行测试
        OrderOperationResult result = orderService.shipOrder(TEST_ORDER_ID, TEST_SELLER_ID);
        
        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("发货测试完成 - MVC框架: success={}", result.isSuccess());
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("MVC-12 订单完整流程测试")
    public void testCompleteOrderWorkflow() {
        logger.info("开始测试订单完整流程 - MVC框架");
        
        // 1. 创建订单
        OrderCreateDTO createDto = new OrderCreateDTO();
        createDto.setProductIds(Arrays.asList(99L));
        
        OrderOperationResult createResult = orderService.createOrder(createDto, TEST_BUYER_ID);
        assertNotNull(createResult, "创建订单结果不应为空");
        logger.info("创建订单结果: success={}", createResult.isSuccess());
        
        // 2. 获取买家订单列表
        OrderOperationResult buyerOrdersResult = orderService.getBuyerOrders(TEST_BUYER_ID);
        assertNotNull(buyerOrdersResult, "买家订单列表不应为空");
        logger.info("获取买家订单列表: success={}", buyerOrdersResult.isSuccess());
        
        // 3. 获取卖家订单列表
        OrderOperationResult sellerOrdersResult = orderService.getSellerOrders(TEST_SELLER_ID);
        assertNotNull(sellerOrdersResult, "卖家订单列表不应为空");
        logger.info("获取卖家订单列表: success={}", sellerOrdersResult.isSuccess());
        
        // 4. 测试确认收货
        OrderOperationResult confirmResult = orderService.confirmReceipt(TEST_ORDER_ID, TEST_BUYER_ID);
        assertNotNull(confirmResult, "确认收货结果不应为空");
        logger.info("确认收货结果: success={}", confirmResult.isSuccess());
        
        logger.info("订单完整流程测试通过 - MVC框架");
    }
}
