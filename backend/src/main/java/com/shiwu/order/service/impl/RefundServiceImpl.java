package com.shiwu.order.service.impl;

import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.order.model.Order;
import com.shiwu.order.model.RefundTransaction;
import com.shiwu.order.service.RefundService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退款服务实现类 - MVC框架版本
 *
 * 实现模拟退款操作，根据SRS文档UC-18要求：
 * "系统执行模拟退款操作。例如，可以将订单金额返还到买家的虚拟平台余额（如果有这样的设计），
 * 或者简单地记录一笔退款交易。"
 *
 * 这里采用简单记录退款交易的方式，在实际项目中可以扩展为真实的退款操作。
 *
 * @author Shiwu Team
 * @version 1.0
 */
@Service
public class RefundServiceImpl extends BaseService implements RefundService {
    
    // 使用内存存储退款记录（在实际项目中应该使用数据库）
    private final Map<String, RefundTransaction> refundTransactions = new ConcurrentHashMap<>();
    private final Map<Long, RefundTransaction> orderRefundMap = new ConcurrentHashMap<>();
    
    @Override
    public RefundTransaction processRefund(Order order, String reason) {
        if (order == null) {
            logger.error("执行退款失败: 订单信息为空");
            return null;
        }
        
        try {
            // 生成退款交易ID
            String refundId = generateRefundId();
            
            // 创建退款交易记录
            RefundTransaction refundTransaction = new RefundTransaction(
                refundId,
                order.getId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getPriceAtPurchase(),
                reason
            );
            
            // 模拟退款处理
            boolean refundSuccess = simulateRefundProcess(refundTransaction);
            
            if (refundSuccess) {
                refundTransaction.markAsSuccess();
                logger.info("模拟退款成功: refundId={}, orderId={}, amount={}", 
                           refundId, order.getId(), order.getPriceAtPurchase());
            } else {
                refundTransaction.markAsFailed();
                logger.error("模拟退款失败: refundId={}, orderId={}", refundId, order.getId());
            }
            
            // 保存退款记录
            refundTransactions.put(refundId, refundTransaction);
            orderRefundMap.put(order.getId(), refundTransaction);
            
            return refundTransaction;
            
        } catch (Exception e) {
            logger.error("执行退款过程发生异常: orderId={}, error={}", order.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public RefundTransaction getRefundTransaction(String refundId) {
        if (refundId == null) {
            return null;
        }
        return refundTransactions.get(refundId);
    }
    
    @Override
    public RefundTransaction getRefundByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return orderRefundMap.get(orderId);
    }
    
    /**
     * 生成退款交易ID
     * @return 退款交易ID
     */
    private String generateRefundId() {
        return "REFUND_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 模拟退款处理过程
     * 在实际项目中，这里会调用真实的支付系统API进行退款
     * 
     * @param refundTransaction 退款交易记录
     * @return 是否退款成功
     */
    private boolean simulateRefundProcess(RefundTransaction refundTransaction) {
        try {
            // 模拟退款处理时间
            Thread.sleep(100);
            
            // 模拟退款成功率（95%成功率）
            double random = Math.random();
            boolean success = random < 0.95;
            
            if (success) {
                logger.info("模拟退款处理成功: refundId={}, amount={}", 
                           refundTransaction.getRefundId(), refundTransaction.getRefundAmount());
                
                // 在实际项目中，这里会：
                // 1. 调用支付系统退款API
                // 2. 更新买家账户余额
                // 3. 记录退款流水
                // 4. 发送退款通知
                
            } else {
                logger.warn("模拟退款处理失败: refundId={}", refundTransaction.getRefundId());
            }
            
            return success;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("模拟退款处理被中断: refundId={}", refundTransaction.getRefundId());
            return false;
        } catch (Exception e) {
            logger.error("模拟退款处理异常: refundId={}, error={}", 
                        refundTransaction.getRefundId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取所有退款记录（用于测试和调试）
     * @return 退款记录映射
     */
    public Map<String, RefundTransaction> getAllRefundTransactions() {
        return new ConcurrentHashMap<>(refundTransactions);
    }
    
    /**
     * 清空所有退款记录（用于测试）
     */
    public void clearAllRefundTransactions() {
        refundTransactions.clear();
        orderRefundMap.clear();
        logger.info("已清空所有退款记录");
    }
}
