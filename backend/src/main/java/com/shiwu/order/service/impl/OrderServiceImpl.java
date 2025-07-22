package com.shiwu.order.service.impl;

import com.shiwu.cart.dao.CartDao;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.notification.model.Notification;
import com.shiwu.notification.service.NotificationService;
import com.shiwu.notification.service.impl.NotificationServiceImpl;
import com.shiwu.order.dao.OrderDao;
import com.shiwu.order.model.*;
import com.shiwu.order.service.OrderService;
import com.shiwu.order.service.RefundService;
import com.shiwu.product.dao.ProductDao;
import com.shiwu.product.model.Product;
import com.shiwu.review.dao.ReviewDao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现类 - MVC框架版本
 *
 * Task4_3_1_2: 在订单状态变更时创建通知
 */
@Service
public class OrderServiceImpl extends BaseService implements OrderService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private CartDao cartDao;

    @Autowired
    private ReviewDao reviewDao;

    @Autowired
    private RefundService refundService;

    @Autowired
    private NotificationService notificationService;

    // 无参构造函数，用于MVC框架依赖注入
    public OrderServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.orderDao = new OrderDao();
        this.productDao = new ProductDao();
        this.cartDao = new CartDao();
        this.reviewDao = new ReviewDao();
        this.refundService = new com.shiwu.order.service.impl.RefundServiceImpl();
        this.notificationService = new NotificationServiceImpl();
        logger.info("OrderServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public OrderServiceImpl(OrderDao orderDao, ProductDao productDao, CartDao cartDao,
                           ReviewDao reviewDao, RefundService refundService,
                           NotificationService notificationService) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.cartDao = cartDao;
        this.reviewDao = reviewDao;
        this.refundService = refundService;
        this.notificationService = notificationService;
        logger.info("OrderServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public OrderOperationResult createOrder(OrderCreateDTO dto, Long buyerId) {
        // 参数验证
        if (dto == null || buyerId == null) {
            logger.warn("创建订单失败: 请求参数不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }
        
        if (dto.getProductIds() == null || dto.getProductIds().isEmpty()) {
            logger.warn("创建订单失败: 商品列表不能为空");
            return OrderOperationResult.failure(OrderErrorCode.EMPTY_PRODUCT_LIST, OrderErrorCode.MSG_EMPTY_PRODUCT_LIST);
        }
        
        List<Long> createdOrderIds = new ArrayList<>();
        List<Long> lockedProductIds = new ArrayList<>();
        
        try {
            // 遍历每个商品，为每个商品创建单独的订单
            for (Long productId : dto.getProductIds()) {
                // 1. 实时验证商品状态
                Product product = productDao.findById(productId);
                if (product == null) {
                    logger.warn("创建订单失败: 商品不存在, productId={}", productId);
                    rollbackOrders(createdOrderIds, lockedProductIds);
                    return OrderOperationResult.failure(OrderErrorCode.PRODUCT_NOT_FOUND, OrderErrorCode.MSG_PRODUCT_NOT_FOUND);
                }
                
                // 检查商品状态是否为在售
                if (!product.getStatus().equals(Product.STATUS_ONSALE)) {
                    logger.warn("创建订单失败: 商品当前不可购买, productId={}, status={}", productId, product.getStatus());
                    rollbackOrders(createdOrderIds, lockedProductIds);
                    return OrderOperationResult.failure(OrderErrorCode.PRODUCT_NOT_AVAILABLE, OrderErrorCode.MSG_PRODUCT_NOT_AVAILABLE);
                }
                
                // 检查是否购买自己的商品
                if (product.getSellerId().equals(buyerId)) {
                    logger.warn("创建订单失败: 不能购买自己的商品, productId={}, buyerId={}", productId, buyerId);
                    rollbackOrders(createdOrderIds, lockedProductIds);
                    return OrderOperationResult.failure(OrderErrorCode.CANT_BUY_OWN_PRODUCT, OrderErrorCode.MSG_CANT_BUY_OWN_PRODUCT);
                }
                
                // 2. 锁定商品（将商品状态设置为LOCKED）
                boolean lockSuccess = productDao.updateProductStatusBySystem(productId, Product.STATUS_LOCKED);
                if (!lockSuccess) {
                    logger.error("创建订单失败: 锁定商品失败, productId={}", productId);
                    rollbackOrders(createdOrderIds, lockedProductIds);
                    return OrderOperationResult.failure(OrderErrorCode.UPDATE_PRODUCT_STATUS_FAILED, OrderErrorCode.MSG_UPDATE_PRODUCT_STATUS_FAILED);
                }
                lockedProductIds.add(productId);
                
                // 3. 固化商品快照信息
                String imageUrlsSnapshot = getProductImageUrlsSnapshot(productId);
                
                // 4. 创建订单
                Order order = new Order(
                    buyerId,
                    product.getSellerId(),
                    productId,
                    product.getPrice(),
                    product.getTitle(),
                    product.getDescription(),
                    imageUrlsSnapshot
                );
                
                Long orderId = orderDao.createOrder(order);
                if (orderId == null) {
                    logger.error("创建订单失败: 数据库操作失败, productId={}", productId);
                    rollbackOrders(createdOrderIds, lockedProductIds);
                    return OrderOperationResult.failure(OrderErrorCode.CREATE_ORDER_FAILED, OrderErrorCode.MSG_CREATE_ORDER_FAILED);
                }
                
                createdOrderIds.add(orderId);
                
                // 5. 从购物车中移除该商品
                cartDao.removeFromCart(buyerId, productId);
                
                logger.info("创建订单成功: orderId={}, buyerId={}, sellerId={}, productId={}", 
                           orderId, buyerId, product.getSellerId(), productId);
            }
            
            // 返回成功结果
            Map<String, Object> data = new HashMap<>();
            data.put("orderIds", createdOrderIds);
            data.put("orderCount", createdOrderIds.size());
            
            logger.info("批量创建订单成功: buyerId={}, orderCount={}, orderIds={}", 
                       buyerId, createdOrderIds.size(), createdOrderIds);
            
            return OrderOperationResult.success(data);
            
        } catch (Exception e) {
            logger.error("创建订单时发生异常: buyerId={}, error={}", buyerId, e.getMessage(), e);
            rollbackOrders(createdOrderIds, lockedProductIds);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public OrderOperationResult getBuyerOrders(Long buyerId) {
        if (buyerId == null) {
            logger.warn("获取买家订单列表失败: 买家ID不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }
        
        try {
            List<OrderVO> orders = orderDao.findOrdersByBuyerId(buyerId);
            
            // 设置订单状态描述
            for (OrderVO order : orders) {
                order.setStatusText(getOrderStatusText(order.getStatus()));
                
                // 解析图片URL快照
                if (order.getProductImageUrlsSnapshot() != null && !order.getProductImageUrlsSnapshot().isEmpty()) {
                    // 如果productImageUrlsSnapshot已经是List类型，说明已经在DAO层解析过了
                    // 这里不需要再次解析
                } else {
                    order.setProductImageUrlsSnapshot(new ArrayList<>());
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("orders", orders);
            data.put("total", orders.size());
            
            logger.info("获取买家订单列表成功: buyerId={}, orderCount={}", buyerId, orders.size());
            return OrderOperationResult.success(data);
            
        } catch (Exception e) {
            logger.error("获取买家订单列表失败: buyerId={}, error={}", buyerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public OrderOperationResult getSellerOrders(Long sellerId) {
        if (sellerId == null) {
            logger.warn("获取卖家订单列表失败: 卖家ID不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }
        
        try {
            List<OrderVO> orders = orderDao.findOrdersBySellerId(sellerId);
            
            // 设置订单状态描述
            for (OrderVO order : orders) {
                order.setStatusText(getOrderStatusText(order.getStatus()));
                
                // 解析图片URL快照
                if (order.getProductImageUrlsSnapshot() != null && !order.getProductImageUrlsSnapshot().isEmpty()) {
                    // 如果productImageUrlsSnapshot已经是List类型，说明已经在DAO层解析过了
                    // 这里不需要再次解析
                } else {
                    order.setProductImageUrlsSnapshot(new ArrayList<>());
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("orders", orders);
            data.put("total", orders.size());
            
            logger.info("获取卖家订单列表成功: sellerId={}, orderCount={}", sellerId, orders.size());
            return OrderOperationResult.success(data);
            
        } catch (Exception e) {
            logger.error("获取卖家订单列表失败: sellerId={}, error={}", sellerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public OrderOperationResult getOrderById(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            logger.warn("获取订单详情失败: 参数不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }
        
        try {
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("获取订单详情失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }
            
            // 权限验证：只有买家或卖家才能查看订单
            if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                logger.warn("获取订单详情失败: 无权查看订单, orderId={}, userId={}", orderId, userId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }
            
            logger.info("获取订单详情成功: orderId={}, userId={}", orderId, userId);
            return OrderOperationResult.success(order);
            
        } catch (Exception e) {
            logger.error("获取订单详情失败: orderId={}, userId={}, error={}", orderId, userId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public OrderOperationResult updateOrderStatus(Long orderId, Integer status, Long userId) {
        if (orderId == null || status == null || userId == null) {
            logger.warn("更新订单状态失败: 参数不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }
        
        try {
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("更新订单状态失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }
            
            // 权限验证：只有买家或卖家才能更新订单状态
            if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                logger.warn("更新订单状态失败: 无权操作订单, orderId={}, userId={}", orderId, userId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }
            
            boolean success = orderDao.updateOrderStatus(orderId, status);
            if (!success) {
                logger.error("更新订单状态失败: 数据库操作失败, orderId={}, status={}", orderId, status);
                return OrderOperationResult.failure(OrderErrorCode.UPDATE_ORDER_STATUS_FAILED, OrderErrorCode.MSG_UPDATE_ORDER_STATUS_FAILED);
            }

            // Task4_3_1_2: 创建订单状态变更通知
            createOrderStatusNotification(order, status, userId);

            logger.info("更新订单状态成功: orderId={}, status={}, userId={}", orderId, status, userId);
            return OrderOperationResult.success(null);
            
        } catch (Exception e) {
            logger.error("更新订单状态失败: orderId={}, status={}, userId={}, error={}", orderId, status, userId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    /**
     * 获取商品图片URL快照（JSON格式）
     */
    private String getProductImageUrlsSnapshot(Long productId) {
        try {
            List<String> imageUrls = new ArrayList<>();

            // 查询商品的所有图片
            List<com.shiwu.product.model.ProductImage> productImages = productDao.findImagesByProductId(productId);
            for (com.shiwu.product.model.ProductImage image : productImages) {
                imageUrls.add(image.getImageUrl());
            }

            // 转换为JSON格式
            return JsonUtil.toJson(imageUrls);
        } catch (Exception e) {
            logger.warn("获取商品图片快照失败: productId={}, error={}", productId, e.getMessage());
            return "[]";
        }
    }
    
    /**
     * 获取订单状态描述
     */
    private String getOrderStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        
        switch (status) {
            case 0: return "待付款";
            case 1: return "待发货";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "申请退货";
            case 6: return "已退货";
            default: return "未知状态";
        }
    }
    
    /**
     * 回滚已创建的订单和已锁定的商品
     */
    private void rollbackOrders(List<Long> createdOrderIds, List<Long> lockedProductIds) {
        // 回滚已锁定的商品状态
        for (Long productId : lockedProductIds) {
            try {
                productDao.updateProductStatusBySystem(productId, Product.STATUS_ONSALE);
                logger.info("回滚商品状态成功: productId={}", productId);
            } catch (Exception e) {
                logger.error("回滚商品状态失败: productId={}, error={}", productId, e.getMessage(), e);
            }
        }
        
        // 注意：这里没有删除已创建的订单，因为在实际业务中，
        // 订单一旦创建就不应该被删除，而是应该标记为取消状态
        // 如果需要删除订单，可以在这里添加相应的逻辑
    }

    @Override
    public OrderOperationResult updateOrderStatusAfterPayment(List<Long> orderIds, String paymentId) {
        // 参数验证
        if (orderIds == null || orderIds.isEmpty()) {
            logger.warn("支付成功后更新订单状态失败: 订单ID列表不能为空, paymentId={}", paymentId);
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        if (paymentId == null || paymentId.trim().isEmpty()) {
            logger.warn("支付成功后更新订单状态失败: 支付ID不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            int successCount = 0;
            int failureCount = 0;

            for (Long orderId : orderIds) {
                // 验证订单状态
                Order order = orderDao.findById(orderId);
                if (order == null) {
                    logger.warn("订单不存在: orderId={}, paymentId={}", orderId, paymentId);
                    failureCount++;
                    continue;
                }

                if (!order.getStatus().equals(Order.STATUS_AWAITING_PAYMENT)) {
                    logger.warn("订单状态不正确: orderId={}, currentStatus={}, paymentId={}",
                               orderId, order.getStatus(), paymentId);
                    failureCount++;
                    continue;
                }

                // 更新订单状态为待发货
                boolean updateSuccess = orderDao.updateOrderStatus(orderId, Order.STATUS_AWAITING_SHIPPING);
                if (updateSuccess) {
                    successCount++;
                    logger.info("支付成功后更新订单状态成功: orderId={}, status=AWAITING_SHIPPING, paymentId={}",
                               orderId, paymentId);
                } else {
                    failureCount++;
                    logger.error("支付成功后更新订单状态失败: orderId={}, paymentId={}", orderId, paymentId);
                }
            }

            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("totalOrders", orderIds.size());
            data.put("successCount", successCount);
            data.put("failureCount", failureCount);
            data.put("paymentId", paymentId);

            if (failureCount > 0) {
                logger.warn("支付成功后批量更新订单状态部分失败: paymentId={}, success={}, failure={}",
                           paymentId, successCount, failureCount);
                return OrderOperationResult.failure(OrderErrorCode.UPDATE_ORDER_STATUS_FAILED,
                                                   "部分订单状态更新失败");
            } else {
                logger.info("支付成功后批量更新订单状态全部成功: paymentId={}, orderCount={}", paymentId, successCount);
                return OrderOperationResult.success(data);
            }

        } catch (Exception e) {
            logger.error("支付成功后更新订单状态时发生异常: paymentId={}, error={}", paymentId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public OrderOperationResult cancelOrdersAfterPaymentFailure(List<Long> orderIds, String reason) {
        // 参数验证
        if (orderIds == null || orderIds.isEmpty()) {
            logger.warn("支付失败后取消订单失败: 订单ID列表不能为空, reason={}", reason);
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        if (reason == null || reason.trim().isEmpty()) {
            reason = "支付失败";
        }

        try {
            int successCount = 0;
            int failureCount = 0;

            for (Long orderId : orderIds) {
                // 验证订单状态
                Order order = orderDao.findById(orderId);
                if (order == null) {
                    logger.warn("订单不存在: orderId={}, reason={}", orderId, reason);
                    failureCount++;
                    continue;
                }

                if (!order.getStatus().equals(Order.STATUS_AWAITING_PAYMENT)) {
                    logger.warn("订单状态不正确: orderId={}, currentStatus={}, reason={}",
                               orderId, order.getStatus(), reason);
                    failureCount++;
                    continue;
                }

                // 取消订单
                boolean orderUpdateSuccess = orderDao.updateOrderStatus(orderId, Order.STATUS_CANCELLED);
                if (orderUpdateSuccess) {
                    // 解锁商品
                    boolean productUnlockSuccess = productDao.updateProductStatusBySystem(order.getProductId(), Product.STATUS_ONSALE);
                    if (productUnlockSuccess) {
                        successCount++;
                        logger.info("支付失败后取消订单并解锁商品成功: orderId={}, productId={}, reason={}",
                                   orderId, order.getProductId(), reason);
                    } else {
                        logger.error("解锁商品失败: orderId={}, productId={}, reason={}",
                                    orderId, order.getProductId(), reason);
                        // 即使解锁商品失败，订单取消仍然算成功
                        successCount++;
                    }
                } else {
                    failureCount++;
                    logger.error("支付失败后取消订单失败: orderId={}, reason={}", orderId, reason);
                }
            }

            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("totalOrders", orderIds.size());
            data.put("successCount", successCount);
            data.put("failureCount", failureCount);
            data.put("reason", reason);

            if (failureCount > 0) {
                logger.warn("支付失败后批量取消订单部分失败: reason={}, success={}, failure={}",
                           reason, successCount, failureCount);
                return OrderOperationResult.failure(OrderErrorCode.UPDATE_ORDER_STATUS_FAILED,
                                                   "部分订单取消失败");
            } else {
                logger.info("支付失败后批量取消订单全部成功: reason={}, orderCount={}", reason, successCount);
                return OrderOperationResult.success(data);
            }

        } catch (Exception e) {
            logger.error("支付失败后取消订单时发生异常: reason={}, error={}", reason, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public OrderOperationResult shipOrder(Long orderId, Long sellerId) {
        // 参数验证
        if (orderId == null || sellerId == null) {
            logger.warn("发货失败: 请求参数不能为空, orderId={}, sellerId={}", orderId, sellerId);
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询订单
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("发货失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 验证卖家权限
            if (!order.getSellerId().equals(sellerId)) {
                logger.warn("发货失败: 无权限发货此订单, orderId={}, sellerId={}, actualSellerId={}",
                           orderId, sellerId, order.getSellerId());
                return OrderOperationResult.failure(OrderErrorCode.SHIP_PERMISSION_DENIED, OrderErrorCode.MSG_SHIP_PERMISSION_DENIED);
            }

            // 验证订单状态
            if (!order.getStatus().equals(Order.STATUS_AWAITING_SHIPPING)) {
                logger.warn("发货失败: 订单状态不正确, orderId={}, currentStatus={}", orderId, order.getStatus());
                return OrderOperationResult.failure(OrderErrorCode.ORDER_STATUS_NOT_AWAITING_SHIPPING, OrderErrorCode.MSG_ORDER_STATUS_NOT_AWAITING_SHIPPING);
            }

            // 更新订单状态为已发货
            boolean updateSuccess = orderDao.updateOrderStatus(orderId, Order.STATUS_SHIPPED);
            if (!updateSuccess) {
                logger.error("发货失败: 更新订单状态失败, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.SHIP_ORDER_FAILED, OrderErrorCode.MSG_SHIP_ORDER_FAILED);
            }

            // 构造返回数据 - 简化版本，只返回关键信息
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("productId", order.getProductId());
            result.put("priceAtPurchase", order.getPriceAtPurchase());
            result.put("status", Order.STATUS_SHIPPED);
            result.put("statusText", getOrderStatusText(Order.STATUS_SHIPPED));
            result.put("message", "发货成功");

            logger.info("发货成功: orderId={}, sellerId={}", orderId, sellerId);
            return OrderOperationResult.success(result);

        } catch (Exception e) {
            logger.error("发货时发生异常: orderId={}, sellerId={}, error={}", orderId, sellerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public OrderOperationResult confirmReceipt(Long orderId, Long buyerId) {
        // 参数验证
        if (orderId == null || buyerId == null) {
            logger.warn("确认收货失败: 请求参数不能为空, orderId={}, buyerId={}", orderId, buyerId);
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询订单
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("确认收货失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 验证买家权限
            if (!order.getBuyerId().equals(buyerId)) {
                logger.warn("确认收货失败: 无权限确认收货此订单, orderId={}, buyerId={}, actualBuyerId={}",
                           orderId, buyerId, order.getBuyerId());
                return OrderOperationResult.failure(OrderErrorCode.CONFIRM_RECEIPT_PERMISSION_DENIED, OrderErrorCode.MSG_CONFIRM_RECEIPT_PERMISSION_DENIED);
            }

            // 验证订单状态
            if (!order.getStatus().equals(Order.STATUS_SHIPPED)) {
                logger.warn("确认收货失败: 订单状态不正确, orderId={}, currentStatus={}", orderId, order.getStatus());
                return OrderOperationResult.failure(OrderErrorCode.ORDER_STATUS_NOT_SHIPPED, OrderErrorCode.MSG_ORDER_STATUS_NOT_SHIPPED);
            }

            // 更新订单状态为已完成
            boolean orderUpdateSuccess = orderDao.updateOrderStatus(orderId, Order.STATUS_COMPLETED);
            if (!orderUpdateSuccess) {
                logger.error("确认收货失败: 更新订单状态失败, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.CONFIRM_RECEIPT_FAILED, OrderErrorCode.MSG_CONFIRM_RECEIPT_FAILED);
            }

            // 更新商品状态为已售出
            boolean productUpdateSuccess = productDao.updateProductStatusBySystem(order.getProductId(), Product.STATUS_SOLD);
            if (!productUpdateSuccess) {
                logger.error("确认收货失败: 更新商品状态为已售失败, orderId={}, productId={}", orderId, order.getProductId());
                // 这里可以考虑回滚订单状态，但为了简化，我们只记录错误
                // 在实际生产环境中，应该使用事务来保证数据一致性
                return OrderOperationResult.failure(OrderErrorCode.UPDATE_PRODUCT_TO_SOLD_FAILED, OrderErrorCode.MSG_UPDATE_PRODUCT_TO_SOLD_FAILED);
            }

            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("productId", order.getProductId());
            result.put("priceAtPurchase", order.getPriceAtPurchase());
            result.put("orderStatus", Order.STATUS_COMPLETED);
            result.put("orderStatusText", getOrderStatusText(Order.STATUS_COMPLETED));
            result.put("productStatus", Product.STATUS_SOLD);
            result.put("productStatusText", "已售出");
            result.put("message", "确认收货成功");

            logger.info("确认收货成功: orderId={}, buyerId={}, productId={}", orderId, buyerId, order.getProductId());
            return OrderOperationResult.success(result);

        } catch (Exception e) {
            logger.error("确认收货时发生异常: orderId={}, buyerId={}, error={}", orderId, buyerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * Task4_3_1_2: 创建订单状态变更通知
     * 当订单状态发生变更时，为买家和卖家创建相应的通知
     */
    private void createOrderStatusNotification(Order order, Integer newStatus, Long operatorUserId) {
        try {
            // 检查通知服务是否可用（测试环境可能为null）
            if (notificationService == null) {
                logger.debug("通知服务不可用，跳过通知创建: orderId={}", order.getId());
                return;
            }

            String statusText = getOrderStatusText(newStatus);
            String title = "订单状态更新";
            String content = "您的订单 #" + order.getId() + " 状态已更新为：" + statusText;

            // 确定通知接收者（非操作者）
            Long recipientId = null;
            if (operatorUserId.equals(order.getBuyerId())) {
                // 买家操作，通知卖家
                recipientId = order.getSellerId();
                content = "买家已将订单 #" + order.getId() + " 状态更新为：" + statusText;
            } else if (operatorUserId.equals(order.getSellerId())) {
                // 卖家操作，通知买家
                recipientId = order.getBuyerId();
                content = "卖家已将订单 #" + order.getId() + " 状态更新为：" + statusText;
            }

            if (recipientId != null) {
                Notification notification = new Notification();
                notification.setRecipientId(recipientId);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setNotificationType(Notification.TYPE_ORDER_STATUS);
                notification.setSourceType(Notification.SOURCE_ORDER);
                notification.setSourceId(order.getId());
                notification.setRelatedUserId(operatorUserId);
                notification.setActionUrl("/order/" + order.getId());
                notification.setPriority(Notification.PRIORITY_NORMAL);
                notification.setExpireAfterHours(168); // 7天过期

                Result<Long> result = notificationService.createNotification(notification);

                if (result.isSuccess()) {
                    logger.info("创建订单状态变更通知成功: orderId={}, notificationId={}, recipientId={}",
                               order.getId(), result.getData(), recipientId);
                } else {
                    logger.warn("创建订单状态变更通知失败: orderId={}, recipientId={}, error={}",
                               order.getId(), recipientId, result.getMessage());
                }
            }

        } catch (Exception e) {
            // 通知创建失败不影响订单状态更新的主流程
            logger.error("创建订单状态变更通知时发生异常: orderId={}, newStatus={}, error={}",
                        order.getId(), newStatus, e.getMessage(), e);
        }
    }

    @Override
    public OrderOperationResult applyForReturn(Long orderId, ReturnRequestDTO returnRequestDTO, Long buyerId) {
        // 参数验证
        if (orderId == null || returnRequestDTO == null || buyerId == null) {
            logger.warn("申请退货失败: 参数不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        // 验证退货申请数据
        if (!returnRequestDTO.isValid()) {
            String validationError = returnRequestDTO.getValidationError();
            logger.warn("申请退货失败: 退货申请数据无效, error={}", validationError);

            if (validationError.contains("不能为空")) {
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_INVALID_REASON, OrderErrorCode.MSG_RETURN_REQUEST_INVALID_REASON);
            } else if (validationError.contains("不能超过")) {
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_REASON_TOO_LONG, OrderErrorCode.MSG_RETURN_REQUEST_REASON_TOO_LONG);
            } else {
                return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, validationError);
            }
        }

        try {
            // 检查订单是否存在
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("申请退货失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 权限验证：只有买家可以申请退货
            if (!order.getBuyerId().equals(buyerId)) {
                logger.warn("申请退货失败: 无权限申请退货此订单, orderId={}, buyerId={}, actualBuyerId={}",
                           orderId, buyerId, order.getBuyerId());
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_PERMISSION_DENIED, OrderErrorCode.MSG_RETURN_REQUEST_PERMISSION_DENIED);
            }

            // 检查订单状态：只有已完成的订单才能申请退货
            if (!Order.STATUS_COMPLETED.equals(order.getStatus())) {
                logger.warn("申请退货失败: 订单状态不是已完成, orderId={}, status={}", orderId, order.getStatus());
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_ORDER_NOT_COMPLETED, OrderErrorCode.MSG_RETURN_REQUEST_ORDER_NOT_COMPLETED);
            }

            // 检查是否已经申请过退货
            if (Order.STATUS_RETURN_REQUESTED.equals(order.getStatus()) || Order.STATUS_RETURNED.equals(order.getStatus())) {
                logger.warn("申请退货失败: 订单已经申请过退货, orderId={}, status={}", orderId, order.getStatus());
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_ALREADY_APPLIED, OrderErrorCode.MSG_RETURN_REQUEST_ALREADY_APPLIED);
            }

            // 检查申请时效：订单完成后7天内可以申请退货
            LocalDateTime orderUpdateTime = order.getUpdateTime();
            if (orderUpdateTime != null) {
                long daysBetween = ChronoUnit.DAYS.between(orderUpdateTime, LocalDateTime.now());
                if (daysBetween > 7) {
                    logger.warn("申请退货失败: 申请时间已过期, orderId={}, orderUpdateTime={}, daysBetween={}",
                               orderId, orderUpdateTime, daysBetween);
                    return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_TIME_EXPIRED, OrderErrorCode.MSG_RETURN_REQUEST_TIME_EXPIRED);
                }
            }

            // 检查订单是否已经评价：根据SRS文档，已评价的订单不能申请退货
            if (reviewDao.isOrderReviewed(orderId)) {
                logger.warn("申请退货失败: 订单已经评价过, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.RETURN_REQUEST_ORDER_ALREADY_REVIEWED, OrderErrorCode.MSG_RETURN_REQUEST_ORDER_ALREADY_REVIEWED);
            }

            // 更新订单状态为申请退货
            boolean updateSuccess = orderDao.updateOrderStatus(orderId, Order.STATUS_RETURN_REQUESTED);
            if (!updateSuccess) {
                logger.error("申请退货失败: 更新订单状态失败, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.APPLY_RETURN_FAILED, OrderErrorCode.MSG_APPLY_RETURN_FAILED);
            }

            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("productId", order.getProductId());
            result.put("sellerId", order.getSellerId());
            result.put("reason", returnRequestDTO.getReason());
            result.put("orderStatus", Order.STATUS_RETURN_REQUESTED);
            result.put("orderStatusText", getOrderStatusText(Order.STATUS_RETURN_REQUESTED));
            result.put("message", "申请退货成功，等待卖家处理");

            logger.info("申请退货成功: orderId={}, buyerId={}, reason={}", orderId, buyerId, returnRequestDTO.getReason());
            return OrderOperationResult.success(result);

        } catch (Exception e) {
            logger.error("申请退货时发生异常: orderId={}, buyerId={}, error={}", orderId, buyerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public OrderOperationResult processReturnRequest(Long orderId, ProcessReturnRequestDTO processReturnRequestDTO, Long sellerId) {
        // 参数验证
        if (orderId == null || processReturnRequestDTO == null || sellerId == null) {
            logger.warn("处理退货申请失败: 参数不能为空");
            return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, OrderErrorCode.MSG_INVALID_PARAMS);
        }

        // 验证处理退货申请数据
        if (!processReturnRequestDTO.isValid()) {
            String validationError = processReturnRequestDTO.getValidationError();
            logger.warn("处理退货申请失败: 处理数据无效, error={}", validationError);

            if (validationError.contains("处理决定不能为空")) {
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_INVALID_DECISION, OrderErrorCode.MSG_PROCESS_RETURN_INVALID_DECISION);
            } else if (validationError.contains("必须填写拒绝原因")) {
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_MISSING_REJECT_REASON, OrderErrorCode.MSG_PROCESS_RETURN_MISSING_REJECT_REASON);
            } else if (validationError.contains("不能超过")) {
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_REJECT_REASON_TOO_LONG, OrderErrorCode.MSG_PROCESS_RETURN_REJECT_REASON_TOO_LONG);
            } else {
                return OrderOperationResult.failure(OrderErrorCode.INVALID_PARAMS, validationError);
            }
        }

        try {
            // 检查订单是否存在
            Order order = orderDao.findById(orderId);
            if (order == null) {
                logger.warn("处理退货申请失败: 订单不存在, orderId={}", orderId);
                return OrderOperationResult.failure(OrderErrorCode.ORDER_NOT_FOUND, OrderErrorCode.MSG_ORDER_NOT_FOUND);
            }

            // 权限验证：只有卖家可以处理退货申请
            if (!order.getSellerId().equals(sellerId)) {
                logger.warn("处理退货申请失败: 无权限处理此退货申请, orderId={}, sellerId={}, actualSellerId={}",
                           orderId, sellerId, order.getSellerId());
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_PERMISSION_DENIED, OrderErrorCode.MSG_PROCESS_RETURN_PERMISSION_DENIED);
            }

            // 检查订单状态：只有申请退货状态的订单才能处理
            if (!Order.STATUS_RETURN_REQUESTED.equals(order.getStatus())) {
                logger.warn("处理退货申请失败: 订单状态不是申请退货, orderId={}, status={}", orderId, order.getStatus());
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_ORDER_NOT_RETURN_REQUESTED, OrderErrorCode.MSG_PROCESS_RETURN_ORDER_NOT_RETURN_REQUESTED);
            }

            // 根据处理决定执行不同的逻辑
            if (processReturnRequestDTO.isApproved()) {
                // 同意退货：RETURN_REQUESTED → RETURNED
                return processApproveReturn(order, processReturnRequestDTO);
            } else {
                // 拒绝退货：RETURN_REQUESTED → COMPLETED
                return processRejectReturn(order, processReturnRequestDTO);
            }

        } catch (Exception e) {
            logger.error("处理退货申请时发生异常: orderId={}, sellerId={}, error={}", orderId, sellerId, e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 处理同意退货的逻辑
     */
    private OrderOperationResult processApproveReturn(Order order, ProcessReturnRequestDTO processReturnRequestDTO) {
        try {
            // 执行模拟退款操作
            RefundTransaction refundTransaction = refundService.processRefund(order, "卖家同意退货申请");
            if (refundTransaction == null || !refundTransaction.isSuccess()) {
                logger.error("同意退货失败: 模拟退款操作失败, orderId={}", order.getId());
                return OrderOperationResult.failure(OrderErrorCode.SIMULATE_REFUND_FAILED, OrderErrorCode.MSG_SIMULATE_REFUND_FAILED);
            }

            // 更新订单状态为已退货
            boolean updateSuccess = orderDao.updateOrderStatus(order.getId(), Order.STATUS_RETURNED);
            if (!updateSuccess) {
                logger.error("同意退货失败: 更新订单状态失败, orderId={}", order.getId());
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_FAILED, OrderErrorCode.MSG_PROCESS_RETURN_FAILED);
            }

            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("productId", order.getProductId());
            result.put("buyerId", order.getBuyerId());
            result.put("approved", true);
            result.put("orderStatus", Order.STATUS_RETURNED);
            result.put("orderStatusText", getOrderStatusText(Order.STATUS_RETURNED));
            result.put("refundId", refundTransaction.getRefundId());
            result.put("refundAmount", refundTransaction.getRefundAmount());
            result.put("message", "已同意退货申请，退款已处理");

            logger.info("同意退货成功: orderId={}, sellerId={}, refundId={}",
                       order.getId(), order.getSellerId(), refundTransaction.getRefundId());
            return OrderOperationResult.success(result);

        } catch (Exception e) {
            logger.error("处理同意退货时发生异常: orderId={}, error={}", order.getId(), e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    /**
     * 处理拒绝退货的逻辑
     */
    private OrderOperationResult processRejectReturn(Order order, ProcessReturnRequestDTO processReturnRequestDTO) {
        try {
            // 更新订单状态为已完成（恢复原状态）
            boolean updateSuccess = orderDao.updateOrderStatus(order.getId(), Order.STATUS_COMPLETED);
            if (!updateSuccess) {
                logger.error("拒绝退货失败: 更新订单状态失败, orderId={}", order.getId());
                return OrderOperationResult.failure(OrderErrorCode.PROCESS_RETURN_FAILED, OrderErrorCode.MSG_PROCESS_RETURN_FAILED);
            }

            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("productId", order.getProductId());
            result.put("buyerId", order.getBuyerId());
            result.put("approved", false);
            result.put("rejectReason", processReturnRequestDTO.getRejectReason());
            result.put("orderStatus", Order.STATUS_COMPLETED);
            result.put("orderStatusText", getOrderStatusText(Order.STATUS_COMPLETED));
            result.put("message", "已拒绝退货申请，订单状态恢复为已完成");

            logger.info("拒绝退货成功: orderId={}, sellerId={}, rejectReason={}",
                       order.getId(), order.getSellerId(), processReturnRequestDTO.getRejectReason());
            return OrderOperationResult.success(result);

        } catch (Exception e) {
            logger.error("处理拒绝退货时发生异常: orderId={}, error={}", order.getId(), e.getMessage(), e);
            return OrderOperationResult.failure(OrderErrorCode.SYSTEM_ERROR, OrderErrorCode.MSG_SYSTEM_ERROR);
        }
    }
}
