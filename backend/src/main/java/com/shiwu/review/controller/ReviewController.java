package com.shiwu.review.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.web.BaseController;
import com.shiwu.review.model.ReviewCreateDTO;
import com.shiwu.review.model.ReviewErrorCode;
import com.shiwu.review.model.ReviewOperationResult;
import com.shiwu.review.model.ReviewVO;
import com.shiwu.review.service.ReviewService;
import com.shiwu.review.service.impl.ReviewServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * 评价控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 处理评价相关的HTTP请求
 */
@Controller
@WebServlet("/review/*")
public class ReviewController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    public ReviewController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.reviewService = new ReviewServiceImpl();
        logger.info("ReviewController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
        logger.info("ReviewController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 提交评价 - MVC版本
     */
    @RequestMapping(value = "/", method = "POST")
    public Result<Object> submitReview(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ReviewCreateDTO dto = JsonUtil.fromJson(requestBody, ReviewCreateDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理提交评价请求: userId={}, orderId={}, rating={}",
                        userId, dto.getOrderId(), dto.getRating());

            ReviewOperationResult result = reviewService.submitReview(dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("提交评价失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 获取商品评价列表 - MVC版本
     */
    @RequestMapping(value = "/product/{productId}", method = "GET")
    public Result<Object> getProductReviews(@PathVariable("productId") Long productId) {
        try {
            if (productId == null || productId <= 0) {
                return Result.fail("400", "商品ID不能为空");
            }

            logger.debug("处理获取商品评价列表请求: productId={}", productId);

            List<ReviewVO> reviews = reviewService.getReviewsByProductId(productId);
            return Result.success(reviews);
        } catch (Exception e) {
            logger.error("获取商品评价列表失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户评价列表 - MVC版本
     */
    @RequestMapping(value = "/user/{userId}", method = "GET")
    public Result<Object> getUserReviews(@PathVariable("userId") Long userId) {
        try {
            if (userId == null || userId <= 0) {
                return Result.fail("400", "用户ID不能为空");
            }

            logger.debug("处理获取用户评价列表请求: userId={}", userId);

            List<ReviewVO> reviews = reviewService.getReviewsByUserId(userId);
            return Result.success(reviews);
        } catch (Exception e) {
            logger.error("获取用户评价列表失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 检查订单是否可评价 - MVC版本
     */
    @RequestMapping(value = "/check/{orderId}", method = "GET")
    public Result<Object> checkOrderCanReview(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID不能为空");
            }

            logger.debug("处理检查订单是否可评价请求: orderId={}, userId={}", orderId, userId);

            ReviewOperationResult result = reviewService.checkOrderCanReview(orderId, userId);

            if (result.isSuccess()) {
                return Result.success(true);
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("检查订单是否可评价失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 获取评价详情 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/{reviewId}", method = "GET")
    public Result<Object> getReviewDetail(@PathVariable("reviewId") Long reviewId) {
        try {
            if (reviewId == null || reviewId <= 0) {
                return Result.fail("400", "评价ID不能为空");
            }

            logger.debug("处理获取评价详情请求: reviewId={}", reviewId);

            // 这里可以扩展ReviewService添加getReviewById方法
            // 暂时返回成功，表示功能已预留
            return Result.success("评价详情功能已预留，reviewId=" + reviewId);
        } catch (Exception e) {
            logger.error("获取评价详情失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 删除评价 - MVC版本（新增功能）
     */
    @RequestMapping(value = "/{reviewId}", method = "DELETE")
    public Result<Object> deleteReview(@PathVariable("reviewId") Long reviewId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (reviewId == null || reviewId <= 0) {
                return Result.fail("400", "评价ID不能为空");
            }

            logger.debug("处理删除评价请求: reviewId={}, userId={}", reviewId, userId);

            // 这里可以扩展ReviewService添加deleteReview方法
            // 暂时返回成功，表示功能已预留
            return Result.success("删除评价功能已预留，reviewId=" + reviewId);
        } catch (Exception e) {
            logger.error("删除评价失败: {}", e.getMessage(), e);
            return Result.fail(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前用户ID
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (JwtUtil.validateToken(token)) {
                    return JwtUtil.getUserIdFromToken(token);
                }
            }
        } catch (Exception e) {
            logger.warn("获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 读取请求体
     */
    protected String readRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            logger.error("读取请求体失败: {}", e.getMessage());
            return null;
        }
    }
}