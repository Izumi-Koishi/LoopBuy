package com.shiwu.review.service.impl;

import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.order.dao.OrderDao;
import com.shiwu.order.model.Order;
import com.shiwu.review.dao.ReviewDao;
import com.shiwu.review.model.*;
import com.shiwu.review.service.ReviewService;
import com.shiwu.user.service.UserService;

import java.util.Collections;
import java.util.List;

/**
 * 评价服务实现类 - MVC框架版本
 *
 * 严格遵循项目规范：
 * 1. 模块解耦：通过Service接口调用其他模块
 * 2. 业务逻辑校验：订单状态、权限验证等
 * 3. 异常处理：所有方法都要处理异常
 * 4. 日志记录：记录关键操作和错误信息
 *
 * @author Shiwu Team
 * @version 1.0
 */
@Service
public class ReviewServiceImpl extends BaseService implements ReviewService {

    @Autowired
    private ReviewDao reviewDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private UserService userService;

    // 无参构造函数，用于MVC框架依赖注入
    public ReviewServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.reviewDao = new ReviewDao();
        this.orderDao = new OrderDao();
        this.userService = new com.shiwu.user.service.impl.UserServiceImpl();
        logger.info("ReviewServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public ReviewServiceImpl(ReviewDao reviewDao, OrderDao orderDao, UserService userService) {
        this.reviewDao = reviewDao;
        this.orderDao = orderDao;
        this.userService = userService;
        logger.info("ReviewServiceImpl初始化完成 - 使用兼容性构造函数");
    }

    @Override
    public ReviewOperationResult submitReview(ReviewCreateDTO reviewCreateDTO, Long userId) {
        // 参数校验
        if (reviewCreateDTO == null) {
            logger.warn("提交评价失败: 请求参数为空");
            return ReviewOperationResult.failure(ReviewErrorCode.INVALID_PARAMS, ReviewErrorCode.MSG_INVALID_PARAMS);
        }

        if (userId == null) {
            logger.warn("提交评价失败: 用户ID为空");
            return ReviewOperationResult.failure(ReviewErrorCode.INVALID_PARAMS, ReviewErrorCode.MSG_INVALID_PARAMS);
        }

        if (reviewCreateDTO.getOrderId() == null) {
            logger.warn("提交评价失败: 订单ID为空");
            return ReviewOperationResult.failure(ReviewErrorCode.INVALID_ORDER_ID, ReviewErrorCode.MSG_INVALID_ORDER_ID);
        }

        if (reviewCreateDTO.getRating() == null || 
            reviewCreateDTO.getRating() < Review.MIN_RATING || 
            reviewCreateDTO.getRating() > Review.MAX_RATING) {
            logger.warn("提交评价失败: 评分无效 rating={}", reviewCreateDTO.getRating());
            return ReviewOperationResult.failure(ReviewErrorCode.INVALID_RATING, ReviewErrorCode.MSG_INVALID_RATING);
        }

        if (reviewCreateDTO.getComment() != null && reviewCreateDTO.getComment().length() > 500) {
            logger.warn("提交评价失败: 评价内容过长 length={}", reviewCreateDTO.getComment().length());
            return ReviewOperationResult.failure(ReviewErrorCode.COMMENT_TOO_LONG, ReviewErrorCode.MSG_COMMENT_TOO_LONG);
        }

        try {
            // 检查订单是否可以评价
            ReviewOperationResult checkResult = checkOrderCanReview(reviewCreateDTO.getOrderId(), userId);
            if (!checkResult.isSuccess()) {
                return checkResult;
            }

            // 获取订单信息
            Order order = orderDao.findById(reviewCreateDTO.getOrderId());
            if (order == null) {
                logger.error("提交评价失败: 订单不存在 orderId={}", reviewCreateDTO.getOrderId());
                return ReviewOperationResult.failure(ReviewErrorCode.ORDER_NOT_FOUND, ReviewErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 创建评价对象
            Review review = new Review(
                reviewCreateDTO.getOrderId(),
                order.getProductId(),
                userId,
                reviewCreateDTO.getRating(),
                reviewCreateDTO.getComment()
            );

            // 保存评价
            Long reviewId = reviewDao.createReview(review);
            if (reviewId == null) {
                logger.error("提交评价失败: 数据库操作失败 orderId={}, userId={}", reviewCreateDTO.getOrderId(), userId);
                return ReviewOperationResult.failure(ReviewErrorCode.CREATE_REVIEW_FAILED, ReviewErrorCode.MSG_CREATE_REVIEW_FAILED);
            }

            // 更新卖家的平均评分
            boolean updateRatingSuccess = userService.updateUserAverageRating(order.getSellerId());
            if (!updateRatingSuccess) {
                logger.warn("提交评价成功但更新卖家评分失败: reviewId={}, sellerId={}", reviewId, order.getSellerId());
                // 注意：这里不返回失败，因为评价已经成功提交，只是更新评分失败
            }

            logger.info("提交评价成功: reviewId={}, orderId={}, userId={}, rating={}", 
                       reviewId, reviewCreateDTO.getOrderId(), userId, reviewCreateDTO.getRating());
            
            // 返回创建的评价ID
            return ReviewOperationResult.success(reviewId);

        } catch (Exception e) {
            logger.error("提交评价过程发生异常: orderId={}, userId={}, error={}", 
                        reviewCreateDTO.getOrderId(), userId, e.getMessage(), e);
            return ReviewOperationResult.failure(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public List<ReviewVO> getReviewsByProductId(Long productId) {
        if (productId == null) {
            logger.warn("获取商品评价列表失败: 商品ID为空");
            return Collections.emptyList();
        }

        try {
            List<ReviewVO> reviews = reviewDao.findReviewsByProductId(productId);
            logger.info("获取商品评价列表成功: productId={}, count={}", productId, reviews.size());
            return reviews;
        } catch (Exception e) {
            logger.error("获取商品评价列表失败: productId={}, error={}", productId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ReviewVO> getReviewsByUserId(Long userId) {
        if (userId == null) {
            logger.warn("获取用户评价列表失败: 用户ID为空");
            return Collections.emptyList();
        }

        try {
            List<ReviewVO> reviews = reviewDao.findReviewsByUserId(userId);
            logger.info("获取用户评价列表成功: userId={}, count={}", userId, reviews.size());
            return reviews;
        } catch (Exception e) {
            logger.error("获取用户评价列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public ReviewOperationResult checkOrderCanReview(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            logger.warn("检查订单是否可评价失败: 参数为空 orderId={}, userId={}", orderId, userId);
            return ReviewOperationResult.failure(ReviewErrorCode.INVALID_PARAMS, ReviewErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 检查订单是否存在
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("检查订单是否可评价失败: 订单不存在 orderId={}", orderId);
                return ReviewOperationResult.failure(ReviewErrorCode.ORDER_NOT_FOUND, ReviewErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 检查是否是买家
            if (!order.getBuyerId().equals(userId)) {
                logger.warn("检查订单是否可评价失败: 不是订单买家 orderId={}, userId={}, buyerId={}", 
                           orderId, userId, order.getBuyerId());
                return ReviewOperationResult.failure(ReviewErrorCode.NOT_ORDER_BUYER, ReviewErrorCode.MSG_NOT_ORDER_BUYER);
            }

            // 检查订单状态是否为已完成
            if (!Order.STATUS_COMPLETED.equals(order.getStatus())) {
                logger.warn("检查订单是否可评价失败: 订单状态不是已完成 orderId={}, status={}", orderId, order.getStatus());
                return ReviewOperationResult.failure(ReviewErrorCode.ORDER_NOT_COMPLETED, ReviewErrorCode.MSG_ORDER_NOT_COMPLETED);
            }

            // 检查订单是否已经评价过
            if (reviewDao.isOrderReviewed(orderId)) {
                logger.warn("检查订单是否可评价失败: 订单已评价 orderId={}", orderId);
                return ReviewOperationResult.failure(ReviewErrorCode.ORDER_ALREADY_REVIEWED, ReviewErrorCode.MSG_ORDER_ALREADY_REVIEWED);
            }

            logger.info("订单可以评价: orderId={}, userId={}", orderId, userId);
            return ReviewOperationResult.success(null);

        } catch (Exception e) {
            logger.error("检查订单是否可评价过程发生异常: orderId={}, userId={}, error={}", orderId, userId, e.getMessage(), e);
            return ReviewOperationResult.failure(ReviewErrorCode.SYSTEM_ERROR, ReviewErrorCode.MSG_SYSTEM_ERROR);
        }
    }
}
