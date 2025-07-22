package com.shiwu.payment.service.impl;

import com.shiwu.common.util.JsonUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.order.dao.OrderDao;
import com.shiwu.order.model.Order;
import com.shiwu.order.model.OrderOperationResult;
import com.shiwu.order.service.OrderService;
import com.shiwu.order.service.impl.OrderServiceImpl;
import com.shiwu.payment.dao.PaymentDao;
import com.shiwu.payment.model.*;
import com.shiwu.payment.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务实现类 - MVC框架版本
 */
@Service
public class PaymentServiceImpl extends BaseService implements PaymentService {

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderService orderService;

    // 无参构造函数，用于MVC框架依赖注入
    public PaymentServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.paymentDao = new PaymentDao();
        this.orderDao = new OrderDao();
        this.orderService = new OrderServiceImpl();
        logger.info("PaymentServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public PaymentServiceImpl(PaymentDao paymentDao, OrderDao orderDao, OrderService orderService) {
        this.paymentDao = paymentDao;
        this.orderDao = orderDao;
        this.orderService = orderService;
        logger.info("PaymentServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public PaymentOperationResult createPayment(PaymentDTO dto, Long userId) {
        // 参数验证
        if (dto == null || userId == null) {
            logger.warn("创建支付失败: 请求参数不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }
        
        if (dto.getOrderIds() == null || dto.getOrderIds().isEmpty()) {
            logger.warn("创建支付失败: 订单列表不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.EMPTY_ORDER_LIST, PaymentErrorCode.MSG_EMPTY_ORDER_LIST);
        }
        
        if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("创建支付失败: 支付金额无效");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_AMOUNT, PaymentErrorCode.MSG_INVALID_AMOUNT);
        }
        
        if (dto.getPaymentMethod() == null || dto.getPaymentMethod() < 1 || dto.getPaymentMethod() > 3) {
            logger.warn("创建支付失败: 支付方式无效");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PAYMENT_METHOD, PaymentErrorCode.MSG_INVALID_PAYMENT_METHOD);
        }
        
        try {
            // 验证订单状态和金额
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Long orderId : dto.getOrderIds()) {
                Order order = orderDao.findById(orderId);
                if (order == null) {
                    logger.warn("创建支付失败: 订单不存在, orderId={}", orderId);
                    return PaymentOperationResult.failure(PaymentErrorCode.ORDER_NOT_FOUND, PaymentErrorCode.MSG_ORDER_NOT_FOUND);
                }
                
                // 检查订单是否属于当前用户
                if (!order.getBuyerId().equals(userId)) {
                    logger.warn("创建支付失败: 无权限操作订单, orderId={}, userId={}", orderId, userId);
                    return PaymentOperationResult.failure(PaymentErrorCode.ORDER_PERMISSION_DENIED, PaymentErrorCode.MSG_ORDER_PERMISSION_DENIED);
                }
                
                // 检查订单状态
                if (!order.getStatus().equals(Order.STATUS_AWAITING_PAYMENT)) {
                    logger.warn("创建支付失败: 订单状态不正确, orderId={}, status={}", orderId, order.getStatus());
                    return PaymentOperationResult.failure(PaymentErrorCode.ORDER_STATUS_INVALID, PaymentErrorCode.MSG_ORDER_STATUS_INVALID);
                }
                
                totalAmount = totalAmount.add(order.getPriceAtPurchase());
            }
            
            // 验证金额
            if (totalAmount.compareTo(dto.getTotalAmount()) != 0) {
                logger.warn("创建支付失败: 订单金额与支付金额不匹配, orderAmount={}, paymentAmount={}", 
                           totalAmount, dto.getTotalAmount());
                return PaymentOperationResult.failure(PaymentErrorCode.ORDER_AMOUNT_MISMATCH, PaymentErrorCode.MSG_ORDER_AMOUNT_MISMATCH);
            }
            
            // 生成支付流水号
            String paymentId = generatePaymentId();
            
            // 设置支付超时时间（15分钟后）
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
            
            // 创建支付记录
            String orderIdsJson = JsonUtil.toJson(dto.getOrderIds());
            Payment payment = new Payment(paymentId, userId, orderIdsJson, dto.getTotalAmount(), 
                                        dto.getPaymentMethod(), expireTime);
            
            Long paymentRecordId = paymentDao.createPayment(payment);
            if (paymentRecordId == null) {
                logger.error("创建支付失败: 数据库操作失败, userId={}", userId);
                return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
            }
            
            // 构造支付页面URL
            String paymentUrl = "/payment/page?paymentId=" + paymentId;
            
            // 构造返回数据
            PaymentVO paymentVO = new PaymentVO();
            paymentVO.setPaymentId(paymentId);
            paymentVO.setOrderIds(dto.getOrderIds());
            paymentVO.setPaymentStatus(Payment.STATUS_PENDING);
            paymentVO.setPaymentStatusText("待支付");
            paymentVO.setPaymentAmount(dto.getTotalAmount());
            paymentVO.setPaymentMethod(dto.getPaymentMethod());
            paymentVO.setPaymentMethodText(getPaymentMethodText(dto.getPaymentMethod()));
            paymentVO.setPaymentUrl(paymentUrl);
            paymentVO.setExpireTime(expireTime);
            
            logger.info("创建支付成功: paymentId={}, userId={}, amount={}, orderCount={}", 
                       paymentId, userId, dto.getTotalAmount(), dto.getOrderIds().size());
            
            return PaymentOperationResult.success(paymentVO);
            
        } catch (Exception e) {
            logger.error("创建支付时发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public PaymentOperationResult processPayment(String paymentId, String paymentPassword, Long userId) {
        // 参数验证
        if (paymentId == null || paymentPassword == null || userId == null) {
            logger.warn("处理支付失败: 请求参数不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }
        
        if (paymentPassword.trim().isEmpty()) {
            logger.warn("处理支付失败: 支付密码不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PAYMENT_PASSWORD, PaymentErrorCode.MSG_INVALID_PAYMENT_PASSWORD);
        }
        
        try {
            // 查询支付记录
            Payment payment = paymentDao.findByPaymentId(paymentId);
            if (payment == null) {
                logger.warn("处理支付失败: 支付记录不存在, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }
            
            // 检查用户权限
            if (!payment.getUserId().equals(userId)) {
                logger.warn("处理支付失败: 无权限操作支付, paymentId={}, userId={}", paymentId, userId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }
            
            // 检查支付状态
            if (!payment.getPaymentStatus().equals(Payment.STATUS_PENDING)) {
                logger.warn("处理支付失败: 支付已处理, paymentId={}, status={}", paymentId, payment.getPaymentStatus());
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED, PaymentErrorCode.MSG_PAYMENT_ALREADY_PROCESSED);
            }
            
            // 检查是否过期
            if (LocalDateTime.now().isAfter(payment.getExpireTime())) {
                logger.warn("处理支付失败: 支付已过期, paymentId={}, expireTime={}", paymentId, payment.getExpireTime());
                // 自动处理超时
                handlePaymentTimeout(paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_TIMEOUT, PaymentErrorCode.MSG_PAYMENT_TIMEOUT);
            }
            
            // 支付密码验证（简单验证，实际应该加密比较）
            if (!"123456".equals(paymentPassword)) {
                logger.warn("处理支付失败: 支付密码错误, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_PASSWORD_ERROR, PaymentErrorCode.MSG_PAYMENT_PASSWORD_ERROR);
            }

            // 直接处理支付成功
            String transactionId = generateTransactionId();
            LocalDateTime paymentTime = LocalDateTime.now();

            // 更新支付状态为成功
            boolean updateSuccess = paymentDao.updatePaymentStatus(paymentId, Payment.STATUS_SUCCESS, transactionId, null);
            if (!updateSuccess) {
                logger.error("处理支付失败: 更新支付状态失败, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
            }

            // 更新订单状态为待发货
            @SuppressWarnings("unchecked")
            List<Long> orderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);
            OrderOperationResult orderUpdateResult = orderService.updateOrderStatusAfterPayment(orderIds, paymentId);
            if (!orderUpdateResult.isSuccess()) {
                logger.error("支付成功后更新订单状态失败: paymentId={}, error={}",
                            paymentId, orderUpdateResult.getErrorMessage());
                // 这里可以考虑回滚支付状态，但为了简化，我们只记录错误
            }

            // 构造返回数据
            PaymentVO paymentVO = new PaymentVO();
            paymentVO.setPaymentId(paymentId);
            paymentVO.setOrderIds(orderIds);
            paymentVO.setPaymentStatus(Payment.STATUS_SUCCESS);
            paymentVO.setPaymentStatusText(getPaymentStatusText(Payment.STATUS_SUCCESS));
            paymentVO.setPaymentAmount(payment.getPaymentAmount());
            paymentVO.setPaymentMethod(payment.getPaymentMethod());
            paymentVO.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
            paymentVO.setPaymentTime(paymentTime);
            paymentVO.setThirdPartyTransactionId(transactionId);

            logger.info("支付处理成功: paymentId={}, userId={}, amount={}, orderCount={}",
                       paymentId, userId, payment.getPaymentAmount(), orderIds.size());

            return PaymentOperationResult.success(paymentVO);
            
        } catch (Exception e) {
            logger.error("处理支付时发生异常: paymentId={}, userId={}, error={}", paymentId, userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    

    
    /**
     * 生成支付流水号
     */
    private String generatePaymentId() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 生成交易号
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 获取支付方式描述
     */
    private String getPaymentMethodText(Integer paymentMethod) {
        if (paymentMethod == null) {
            return "未知";
        }
        
        switch (paymentMethod) {
            case 1: return "支付宝";
            case 2: return "微信支付";
            case 3: return "银行卡";
            default: return "未知";
        }
    }
    
    /**
     * 获取支付状态描述
     */
    private String getPaymentStatusText(Integer paymentStatus) {
        if (paymentStatus == null) {
            return "未知状态";
        }

        switch (paymentStatus) {
            case 0: return "待支付";
            case 1: return "支付成功";
            case 2: return "支付失败";
            case 3: return "支付取消";
            case 4: return "支付超时";
            default: return "未知状态";
        }
    }

    @Override
    public PaymentOperationResult getPaymentStatus(String paymentId, Long userId) {
        // 参数验证
        if (paymentId == null || userId == null) {
            logger.warn("查询支付状态失败: 请求参数不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询支付记录
            Payment payment = paymentDao.findByPaymentId(paymentId);
            if (payment == null) {
                logger.warn("查询支付状态失败: 支付记录不存在, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }

            // 检查用户权限
            if (!payment.getUserId().equals(userId)) {
                logger.warn("查询支付状态失败: 无权限操作支付, paymentId={}, userId={}", paymentId, userId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }

            // 构造返回数据
            PaymentVO paymentVO = new PaymentVO();
            paymentVO.setPaymentId(payment.getPaymentId());
            @SuppressWarnings("unchecked")
            List<Long> orderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);
            paymentVO.setOrderIds(orderIds);
            paymentVO.setPaymentStatus(payment.getPaymentStatus());
            paymentVO.setPaymentStatusText(getPaymentStatusText(payment.getPaymentStatus()));
            paymentVO.setPaymentAmount(payment.getPaymentAmount());
            paymentVO.setPaymentMethod(payment.getPaymentMethod());
            paymentVO.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
            paymentVO.setPaymentTime(payment.getPaymentTime());
            paymentVO.setThirdPartyTransactionId(payment.getThirdPartyTransactionId());
            paymentVO.setFailureReason(payment.getFailureReason());
            paymentVO.setExpireTime(payment.getExpireTime());

            logger.info("查询支付状态成功: paymentId={}, userId={}, status={}", paymentId, userId, payment.getPaymentStatus());
            return PaymentOperationResult.success(paymentVO);

        } catch (Exception e) {
            logger.error("查询支付状态时发生异常: paymentId={}, userId={}, error={}", paymentId, userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public PaymentOperationResult cancelPayment(String paymentId, Long userId) {
        // 参数验证
        if (paymentId == null || userId == null) {
            logger.warn("取消支付失败: 请求参数不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询支付记录
            Payment payment = paymentDao.findByPaymentId(paymentId);
            if (payment == null) {
                logger.warn("取消支付失败: 支付记录不存在, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }

            // 检查用户权限
            if (!payment.getUserId().equals(userId)) {
                logger.warn("取消支付失败: 无权限操作支付, paymentId={}, userId={}", paymentId, userId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }

            // 检查支付状态
            if (!payment.getPaymentStatus().equals(Payment.STATUS_PENDING)) {
                logger.warn("取消支付失败: 支付已处理, paymentId={}, status={}", paymentId, payment.getPaymentStatus());
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED, PaymentErrorCode.MSG_PAYMENT_ALREADY_PROCESSED);
            }

            // 更新支付状态为取消
            boolean updateSuccess = paymentDao.updatePaymentStatus(paymentId, Payment.STATUS_CANCELLED, null, "用户主动取消");
            if (!updateSuccess) {
                logger.error("取消支付失败: 更新支付状态失败, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
            }

            // 取消订单并解锁商品
            @SuppressWarnings("unchecked")
            List<Long> orderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);
            OrderOperationResult cancelResult = orderService.cancelOrdersAfterPaymentFailure(orderIds, "用户主动取消");
            if (!cancelResult.isSuccess()) {
                logger.error("取消支付后取消订单失败: paymentId={}, error={}", paymentId, cancelResult.getErrorMessage());
            }

            logger.info("取消支付成功: paymentId={}, userId={}, orderCount={}", paymentId, userId, orderIds.size());
            return PaymentOperationResult.success(null);

        } catch (Exception e) {
            logger.error("取消支付时发生异常: paymentId={}, userId={}, error={}", paymentId, userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public PaymentOperationResult handlePaymentTimeout(String paymentId) {
        // 参数验证
        if (paymentId == null) {
            logger.warn("处理支付超时失败: 支付ID不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询支付记录
            Payment payment = paymentDao.findByPaymentId(paymentId);
            if (payment == null) {
                logger.warn("处理支付超时失败: 支付记录不存在, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);
            }

            // 检查支付状态
            if (!payment.getPaymentStatus().equals(Payment.STATUS_PENDING)) {
                logger.warn("处理支付超时失败: 支付已处理, paymentId={}, status={}", paymentId, payment.getPaymentStatus());
                return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED, PaymentErrorCode.MSG_PAYMENT_ALREADY_PROCESSED);
            }

            // 更新支付状态为超时
            boolean updateSuccess = paymentDao.updatePaymentStatus(paymentId, Payment.STATUS_TIMEOUT, null, "支付超时");
            if (!updateSuccess) {
                logger.error("处理支付超时失败: 更新支付状态失败, paymentId={}", paymentId);
                return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
            }

            // 取消订单并解锁商品
            @SuppressWarnings("unchecked")
            List<Long> orderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);
            OrderOperationResult cancelResult = orderService.cancelOrdersAfterPaymentFailure(orderIds, "支付超时");
            if (!cancelResult.isSuccess()) {
                logger.error("支付超时后取消订单失败: paymentId={}, error={}", paymentId, cancelResult.getErrorMessage());
            }

            logger.info("处理支付超时成功: paymentId={}, orderCount={}", paymentId, orderIds.size());
            return PaymentOperationResult.success(null);

        } catch (Exception e) {
            logger.error("处理支付超时时发生异常: paymentId={}, error={}", paymentId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public PaymentOperationResult getUserPayments(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("获取用户支付记录失败: 用户ID不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            List<Payment> payments = paymentDao.findPaymentsByUserId(userId);
            List<PaymentVO> paymentVOs = new ArrayList<>();

            for (Payment payment : payments) {
                PaymentVO paymentVO = new PaymentVO();
                paymentVO.setPaymentId(payment.getPaymentId());
                @SuppressWarnings("unchecked")
                List<Long> orderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);
                paymentVO.setOrderIds(orderIds);
                paymentVO.setPaymentStatus(payment.getPaymentStatus());
                paymentVO.setPaymentStatusText(getPaymentStatusText(payment.getPaymentStatus()));
                paymentVO.setPaymentAmount(payment.getPaymentAmount());
                paymentVO.setPaymentMethod(payment.getPaymentMethod());
                paymentVO.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
                paymentVO.setPaymentTime(payment.getPaymentTime());
                paymentVO.setThirdPartyTransactionId(payment.getThirdPartyTransactionId());
                paymentVO.setFailureReason(payment.getFailureReason());
                paymentVO.setExpireTime(payment.getExpireTime());

                paymentVOs.add(paymentVO);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("payments", paymentVOs);
            data.put("total", paymentVOs.size());

            logger.info("获取用户支付记录成功: userId={}, paymentCount={}", userId, paymentVOs.size());
            return PaymentOperationResult.success(data);

        } catch (Exception e) {
            logger.error("获取用户支付记录时发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public PaymentOperationResult getPaymentByOrderIds(List<Long> orderIds, Long userId) {
        // 参数验证
        if (orderIds == null || orderIds.isEmpty() || userId == null) {
            logger.warn("根据订单ID获取支付信息失败: 请求参数不能为空");
            return PaymentOperationResult.failure(PaymentErrorCode.INVALID_PARAMS, PaymentErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 查询用户的所有支付记录
            List<Payment> payments = paymentDao.findPaymentsByUserId(userId);

            // 查找包含指定订单的支付记录
            for (Payment payment : payments) {
                @SuppressWarnings("unchecked")
                List<Long> paymentOrderIds = JsonUtil.fromJson(payment.getOrderIds(), List.class);

                // 检查是否包含所有指定的订单ID
                if (paymentOrderIds.containsAll(orderIds)) {
                    PaymentVO paymentVO = new PaymentVO();
                    paymentVO.setPaymentId(payment.getPaymentId());
                    paymentVO.setOrderIds(paymentOrderIds);
                    paymentVO.setPaymentStatus(payment.getPaymentStatus());
                    paymentVO.setPaymentStatusText(getPaymentStatusText(payment.getPaymentStatus()));
                    paymentVO.setPaymentAmount(payment.getPaymentAmount());
                    paymentVO.setPaymentMethod(payment.getPaymentMethod());
                    paymentVO.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
                    paymentVO.setPaymentTime(payment.getPaymentTime());
                    paymentVO.setThirdPartyTransactionId(payment.getThirdPartyTransactionId());
                    paymentVO.setFailureReason(payment.getFailureReason());
                    paymentVO.setExpireTime(payment.getExpireTime());

                    logger.info("根据订单ID获取支付信息成功: orderIds={}, paymentId={}", orderIds, payment.getPaymentId());
                    return PaymentOperationResult.success(paymentVO);
                }
            }

            logger.warn("根据订单ID获取支付信息失败: 未找到对应的支付记录, orderIds={}, userId={}", orderIds, userId);
            return PaymentOperationResult.failure(PaymentErrorCode.PAYMENT_NOT_FOUND, PaymentErrorCode.MSG_PAYMENT_NOT_FOUND);

        } catch (Exception e) {
            logger.error("根据订单ID获取支付信息时发生异常: orderIds={}, userId={}, error={}", orderIds, userId, e.getMessage(), e);
            return PaymentOperationResult.failure(PaymentErrorCode.SYSTEM_ERROR, PaymentErrorCode.MSG_SYSTEM_ERROR);
        }
    }
}
