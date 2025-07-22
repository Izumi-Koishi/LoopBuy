package com.shiwu.payment.dao;

import com.shiwu.common.util.DBUtil;
import com.shiwu.framework.annotation.Repository;
import com.shiwu.payment.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付数据访问对象 - MVC框架版本
 */
@Repository
public class PaymentDao {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDao.class);

    /**
     * 创建支付记录
     * @param payment 支付对象
     * @return 创建的支付记录ID，失败返回null
     */
    public Long createPayment(Payment payment) {
        // 参数验证
        if (payment == null) {
            logger.warn("创建支付记录失败: payment为null");
            return null;
        }
        if (payment.getPaymentId() == null || payment.getPaymentId().trim().isEmpty()) {
            logger.warn("创建支付记录失败: paymentId为空");
            return null;
        }
        if (payment.getUserId() == null) {
            logger.warn("创建支付记录失败: userId为null");
            return null;
        }
        if (payment.getPaymentAmount() == null) {
            logger.warn("创建支付记录失败: paymentAmount为null");
            return null;
        }

        String sql = "INSERT INTO payment (payment_id, user_id, order_ids, payment_amount, " +
                    "payment_method, payment_status, expire_time, is_deleted, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Long paymentRecordId = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, payment.getPaymentId());
            pstmt.setLong(2, payment.getUserId());
            pstmt.setString(3, payment.getOrderIds());
            pstmt.setBigDecimal(4, payment.getPaymentAmount());
            pstmt.setInt(5, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : 0);
            pstmt.setInt(6, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : 0);
            pstmt.setTimestamp(7, payment.getExpireTime() != null ? Timestamp.valueOf(payment.getExpireTime()) : null);

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    paymentRecordId = rs.getLong(1);
                    logger.info("创建支付记录成功: paymentId={}, userId={}, amount={}", 
                               payment.getPaymentId(), payment.getUserId(), payment.getPaymentAmount());
                }
            }
        } catch (SQLException e) {
            logger.error("创建支付记录失败: {}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return paymentRecordId;
    }

    /**
     * 根据支付流水号查询支付记录
     * @param paymentId 支付流水号
     * @return 支付记录，不存在返回null
     */
    public Payment findByPaymentId(String paymentId) {
        String sql = "SELECT id, payment_id, user_id, order_ids, payment_amount, payment_method, " +
                    "payment_status, third_party_transaction_id, failure_reason, payment_time, " +
                    "expire_time, is_deleted, create_time, update_time " +
                    "FROM payment WHERE payment_id = ? AND is_deleted = 0";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Payment payment = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, paymentId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                payment = new Payment();
                payment.setId(rs.getLong("id"));
                payment.setPaymentId(rs.getString("payment_id"));
                payment.setUserId(rs.getLong("user_id"));
                payment.setOrderIds(rs.getString("order_ids"));
                payment.setPaymentAmount(rs.getBigDecimal("payment_amount"));
                payment.setPaymentMethod(rs.getInt("payment_method"));
                payment.setPaymentStatus(rs.getInt("payment_status"));
                payment.setThirdPartyTransactionId(rs.getString("third_party_transaction_id"));
                payment.setFailureReason(rs.getString("failure_reason"));
                
                Timestamp paymentTime = rs.getTimestamp("payment_time");
                if (paymentTime != null) {
                    payment.setPaymentTime(paymentTime.toLocalDateTime());
                }
                
                payment.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
                payment.setDeleted(rs.getBoolean("is_deleted"));
                payment.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                payment.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            }
        } catch (SQLException e) {
            logger.error("查询支付记录失败: paymentId={}, error={}", paymentId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return payment;
    }

    /**
     * 更新支付状态
     * @param paymentId 支付流水号
     * @param status 新状态
     * @param thirdPartyTransactionId 第三方交易号
     * @param failureReason 失败原因
     * @return 是否更新成功
     */
    public boolean updatePaymentStatus(String paymentId, Integer status, String thirdPartyTransactionId, String failureReason) {
        // 参数验证
        if (paymentId == null || paymentId.trim().isEmpty()) {
            logger.warn("更新支付状态失败: paymentId为空");
            return false;
        }
        if (status == null) {
            logger.warn("更新支付状态失败: status为null");
            return false;
        }

        String sql = "UPDATE payment SET payment_status = ?, third_party_transaction_id = ?, " +
                    "failure_reason = ?, payment_time = ?, update_time = NOW() " +
                    "WHERE payment_id = ? AND is_deleted = 0";

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setString(2, thirdPartyTransactionId);
            pstmt.setString(3, failureReason);

            // 如果是成功状态，设置支付时间
            if (Payment.STATUS_SUCCESS.equals(status)) {
                pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setTimestamp(4, null);
            }

            pstmt.setString(5, paymentId);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                logger.info("更新支付状态成功: paymentId={}, status={}", paymentId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("更新支付状态失败: paymentId={}, status={}, error={}", paymentId, status, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, null);
        }
        
        return false;
    }

    /**
     * 查询用户的支付记录列表
     * @param userId 用户ID
     * @return 支付记录列表
     */
    public List<Payment> findPaymentsByUserId(Long userId) {
        // 参数验证
        if (userId == null) {
            logger.warn("查询用户支付记录失败: userId为null");
            return new ArrayList<>();
        }

        String sql = "SELECT id, payment_id, user_id, order_ids, payment_amount, payment_method, " +
                    "payment_status, third_party_transaction_id, failure_reason, payment_time, " +
                    "expire_time, is_deleted, create_time, update_time " +
                    "FROM payment WHERE user_id = ? AND is_deleted = 0 " +
                    "ORDER BY create_time DESC";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Payment> payments = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getLong("id"));
                payment.setPaymentId(rs.getString("payment_id"));
                payment.setUserId(rs.getLong("user_id"));
                payment.setOrderIds(rs.getString("order_ids"));
                payment.setPaymentAmount(rs.getBigDecimal("payment_amount"));
                payment.setPaymentMethod(rs.getInt("payment_method"));
                payment.setPaymentStatus(rs.getInt("payment_status"));
                payment.setThirdPartyTransactionId(rs.getString("third_party_transaction_id"));
                payment.setFailureReason(rs.getString("failure_reason"));
                
                Timestamp paymentTime = rs.getTimestamp("payment_time");
                if (paymentTime != null) {
                    payment.setPaymentTime(paymentTime.toLocalDateTime());
                }
                
                payment.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
                payment.setDeleted(rs.getBoolean("is_deleted"));
                payment.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                payment.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                
                payments.add(payment);
            }
        } catch (SQLException e) {
            logger.error("查询用户支付记录失败: userId={}, error={}", userId, e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return payments;
    }

    /**
     * 查询过期的待支付记录
     * @return 过期的支付记录列表
     */
    public List<Payment> findExpiredPayments() {
        String sql = "SELECT id, payment_id, user_id, order_ids, payment_amount, payment_method, " +
                    "payment_status, third_party_transaction_id, failure_reason, payment_time, " +
                    "expire_time, is_deleted, create_time, update_time " +
                    "FROM payment WHERE payment_status = ? AND expire_time < NOW() AND is_deleted = 0";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Payment> payments = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, Payment.STATUS_PENDING);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getLong("id"));
                payment.setPaymentId(rs.getString("payment_id"));
                payment.setUserId(rs.getLong("user_id"));
                payment.setOrderIds(rs.getString("order_ids"));
                payment.setPaymentAmount(rs.getBigDecimal("payment_amount"));
                payment.setPaymentMethod(rs.getInt("payment_method"));
                payment.setPaymentStatus(rs.getInt("payment_status"));
                payment.setThirdPartyTransactionId(rs.getString("third_party_transaction_id"));
                payment.setFailureReason(rs.getString("failure_reason"));
                
                Timestamp paymentTime = rs.getTimestamp("payment_time");
                if (paymentTime != null) {
                    payment.setPaymentTime(paymentTime.toLocalDateTime());
                }
                
                payment.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
                payment.setDeleted(rs.getBoolean("is_deleted"));
                payment.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                payment.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                
                payments.add(payment);
            }
        } catch (SQLException e) {
            logger.error("查询过期支付记录失败: error={}", e.getMessage(), e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        
        return payments;
    }

    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("关闭ResultSet失败: {}", e.getMessage());
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                logger.error("关闭PreparedStatement失败: {}", e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭Connection失败: {}", e.getMessage());
            }
        }
    }
}
