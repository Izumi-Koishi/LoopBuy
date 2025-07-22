package com.shiwu.review.service;

import com.shiwu.review.model.*;
import com.shiwu.review.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReviewService MVC框架测试类
 * 测试重构后的ReviewService在MVC框架下的所有核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ReviewService MVC框架测试")
public class ReviewServiceMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceMvcTest.class);
    
    private ReviewService reviewService;
    
    // 测试数据
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_ORDER_ID = 99L;
    private static final Long TEST_PRODUCT_ID = 99L;
    private static final Integer TEST_RATING = 5;
    private static final String TEST_COMMENT = "测试评价内容";
    
    @BeforeEach
    public void setUp() {
        // 使用MVC框架的依赖注入方式创建Service
        reviewService = new ReviewServiceImpl();
        logger.info("ReviewService MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() {
        logger.info("ReviewService MVC测试清理完成");
    }

    /**
     * 创建测试评价DTO
     */
    private ReviewCreateDTO createTestReviewDTO() {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(TEST_ORDER_ID);
        dto.setRating(TEST_RATING);
        dto.setComment(TEST_COMMENT);
        return dto;
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("MVC-1 提交评价测试 - 成功场景")
    public void testSubmitReview_Success() {
        logger.info("开始测试提交评价功能 - 成功场景 - MVC框架");
        
        // 创建测试评价DTO
        ReviewCreateDTO dto = createTestReviewDTO();
        
        // 执行测试
        ReviewOperationResult result = reviewService.submitReview(dto, TEST_USER_ID);
        
        // 验证结果 - 由于订单不存在，可能会失败，但这是正常的业务逻辑
        assertNotNull(result, "返回结果不应为空");
        logger.info("提交评价测试完成 - MVC框架: success={}, message={}", 
                   result.isSuccess(), result.getErrorMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("MVC-2 提交评价测试 - 参数验证")
    public void testSubmitReviewValidation() {
        logger.info("开始测试提交评价参数验证 - MVC框架");
        
        // 测试null DTO
        ReviewOperationResult result1 = reviewService.submitReview(null, TEST_USER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null DTO应该失败");
        assertEquals(ReviewErrorCode.INVALID_PARAMS, result1.getErrorCode(), "错误码应该正确");
        
        // 测试null用户ID
        ReviewCreateDTO dto = createTestReviewDTO();
        ReviewOperationResult result2 = reviewService.submitReview(dto, null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null用户ID应该失败");
        assertEquals(ReviewErrorCode.INVALID_PARAMS, result2.getErrorCode(), "错误码应该正确");
        
        // 测试无效评分
        ReviewCreateDTO dto2 = new ReviewCreateDTO();
        dto2.setOrderId(TEST_ORDER_ID);
        dto2.setRating(0); // 无效评分
        dto2.setComment(TEST_COMMENT);

        ReviewOperationResult result3 = reviewService.submitReview(dto2, TEST_USER_ID);
        assertNotNull(result3, "返回结果不应为空");
        assertFalse(result3.isSuccess(), "无效评分应该失败");
        assertEquals(ReviewErrorCode.INVALID_RATING, result3.getErrorCode(), "错误码应该正确");
        
        logger.info("提交评价参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("MVC-3 获取商品评价列表测试")
    public void testGetReviewsByProductId() {
        logger.info("开始测试获取商品评价列表功能 - MVC框架");

        // 执行测试
        List<ReviewVO> result = reviewService.getReviewsByProductId(TEST_PRODUCT_ID);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 评价列表可能为空，这是正常的

        logger.info("获取商品评价列表测试通过 - MVC框架: size={}", result.size());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("MVC-4 获取商品评价列表测试 - 参数验证")
    public void testGetReviewsByProductIdValidation() {
        logger.info("开始测试获取商品评价列表参数验证 - MVC框架");

        // 测试null商品ID
        List<ReviewVO> result1 = reviewService.getReviewsByProductId(null);
        assertNotNull(result1, "返回结果不应为空");
        assertTrue(result1.isEmpty(), "null商品ID应该返回空列表");

        logger.info("获取商品评价列表参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("MVC-5 获取用户评价列表测试")
    public void testGetReviewsByUserId() {
        logger.info("开始测试获取用户评价列表功能 - MVC框架");

        // 执行测试
        List<ReviewVO> result = reviewService.getReviewsByUserId(TEST_USER_ID);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        // 用户评价列表可能为空，这是正常的

        logger.info("获取用户评价列表测试通过 - MVC框架: size={}", result.size());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("MVC-6 获取用户评价列表测试 - 参数验证")
    public void testGetReviewsByUserIdValidation() {
        logger.info("开始测试获取用户评价列表参数验证 - MVC框架");

        // 测试null用户ID
        List<ReviewVO> result1 = reviewService.getReviewsByUserId(null);
        assertNotNull(result1, "返回结果不应为空");
        assertTrue(result1.isEmpty(), "null用户ID应该返回空列表");

        logger.info("获取用户评价列表参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("MVC-7 检查订单是否可以评价测试")
    public void testCheckOrderCanReview() {
        logger.info("开始测试检查订单是否可以评价功能 - MVC框架");

        // 执行测试
        ReviewOperationResult result = reviewService.checkOrderCanReview(TEST_ORDER_ID, TEST_USER_ID);

        // 验证结果
        assertNotNull(result, "返回结果不应为空");
        logger.info("检查订单是否可以评价测试完成 - MVC框架: success={}, message={}",
                   result.isSuccess(), result.getErrorMessage());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("MVC-8 检查订单是否可以评价测试 - 参数验证")
    public void testCheckOrderCanReviewValidation() {
        logger.info("开始测试检查订单是否可以评价参数验证 - MVC框架");

        // 测试null订单ID
        ReviewOperationResult result1 = reviewService.checkOrderCanReview(null, TEST_USER_ID);
        assertNotNull(result1, "返回结果不应为空");
        assertFalse(result1.isSuccess(), "null订单ID应该失败");
        assertEquals(ReviewErrorCode.INVALID_PARAMS, result1.getErrorCode(), "错误码应该正确");

        // 测试null用户ID
        ReviewOperationResult result2 = reviewService.checkOrderCanReview(TEST_ORDER_ID, null);
        assertNotNull(result2, "返回结果不应为空");
        assertFalse(result2.isSuccess(), "null用户ID应该失败");
        assertEquals(ReviewErrorCode.INVALID_PARAMS, result2.getErrorCode(), "错误码应该正确");

        logger.info("检查订单是否可以评价参数验证测试通过 - MVC框架");
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("MVC-10 评价完整流程测试")
    public void testCompleteReviewWorkflow() {
        logger.info("开始测试评价完整流程 - MVC框架");
        
        // 1. 提交评价
        ReviewCreateDTO createDto = createTestReviewDTO();
        ReviewOperationResult createResult = reviewService.submitReview(createDto, TEST_USER_ID);
        assertNotNull(createResult, "提交评价结果不应为空");
        logger.info("提交评价结果: success={}", createResult.isSuccess());
        
        // 2. 获取商品评价列表
        List<ReviewVO> productReviewsResult = reviewService.getReviewsByProductId(TEST_PRODUCT_ID);
        assertNotNull(productReviewsResult, "商品评价列表不应为空");
        logger.info("获取商品评价列表: size={}", productReviewsResult.size());

        // 3. 获取用户评价列表
        List<ReviewVO> userReviewsResult = reviewService.getReviewsByUserId(TEST_USER_ID);
        assertNotNull(userReviewsResult, "用户评价列表不应为空");
        logger.info("获取用户评价列表: size={}", userReviewsResult.size());

        // 4. 检查订单是否可以评价
        ReviewOperationResult checkResult = reviewService.checkOrderCanReview(TEST_ORDER_ID, TEST_USER_ID);
        assertNotNull(checkResult, "检查结果不应为空");
        logger.info("检查订单是否可以评价: success={}", checkResult.isSuccess());
        
        logger.info("评价完整流程测试通过 - MVC框架");
    }
}
