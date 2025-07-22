package com.shiwu.payment.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.payment.model.PaymentDTO;
import com.shiwu.payment.model.PaymentOperationResult;
import com.shiwu.payment.service.PaymentService;
import com.shiwu.payment.service.impl.PaymentServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 覆盖PaymentService的所有功能，确保功能完整性
 */
@Controller
@WebServlet("/api/payment/*")
public class PaymentController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    public PaymentController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.paymentService = new PaymentServiceImpl();
        logger.info("PaymentController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        logger.info("PaymentController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 创建支付 - MVC版本
     */
    @RequestMapping(value = "/", method = "POST")
    public Result<Object> createPayment(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            PaymentDTO dto = JsonUtil.fromJson(requestBody, PaymentDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理创建支付请求: userId={}, orderIds={}, amount={}", 
                        userId, dto.getOrderIds(), dto.getTotalAmount());

            PaymentOperationResult result = paymentService.createPayment(dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("创建支付失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 处理支付（用户确认支付） - MVC版本
     */
    @RequestMapping(value = "/process", method = "POST")
    public Result<Object> processPayment(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = JsonUtil.fromJson(requestBody, Map.class);
            if (requestMap == null || !requestMap.containsKey("paymentId") || !requestMap.containsKey("paymentPassword")) {
                return Result.fail("400", "请求参数格式错误");
            }

            String paymentId = (String) requestMap.get("paymentId");
            String paymentPassword = (String) requestMap.get("paymentPassword");

            logger.debug("处理支付请求: userId={}, paymentId={}", userId, paymentId);

            PaymentOperationResult result = paymentService.processPayment(paymentId, paymentPassword, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("处理支付失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 查询支付状态 - MVC版本
     */
    @RequestMapping(value = "/status", method = "GET")
    public Result<Object> getPaymentStatus(@RequestParam("paymentId") String paymentId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (paymentId == null || paymentId.trim().isEmpty()) {
                return Result.fail("400", "支付ID不能为空");
            }

            logger.debug("处理查询支付状态请求: userId={}, paymentId={}", userId, paymentId);

            PaymentOperationResult result = paymentService.getPaymentStatus(paymentId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("查询支付状态失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 取消支付 - MVC版本
     */
    @RequestMapping(value = "/cancel", method = "POST")
    public Result<Object> cancelPayment(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = JsonUtil.fromJson(requestBody, Map.class);
            if (requestMap == null || !requestMap.containsKey("paymentId")) {
                return Result.fail("400", "请求参数格式错误");
            }

            String paymentId = (String) requestMap.get("paymentId");

            logger.debug("处理取消支付请求: userId={}, paymentId={}", userId, paymentId);

            PaymentOperationResult result = paymentService.cancelPayment(paymentId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("取消支付失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 获取用户支付记录 - MVC版本
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Object> getUserPayments(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理获取用户支付记录请求: userId={}", userId);

            PaymentOperationResult result = paymentService.getUserPayments(userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("获取用户支付记录失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 根据订单ID获取支付信息 - MVC版本
     */
    @RequestMapping(value = "/by-orders", method = "GET")
    public Result<Object> getPaymentByOrderIds(@RequestParam("orderIds") String orderIdsStr, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderIdsStr == null || orderIdsStr.trim().isEmpty()) {
                return Result.fail("400", "订单ID列表不能为空");
            }

            // 解析订单ID列表
            List<Long> orderIds = Arrays.stream(orderIdsStr.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            logger.debug("处理根据订单ID获取支付信息请求: userId={}, orderIds={}", userId, orderIds);

            PaymentOperationResult result = paymentService.getPaymentByOrderIds(orderIds, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("根据订单ID获取支付信息失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 处理支付超时 - MVC版本
     */
    @RequestMapping(value = "/timeout/{paymentId}", method = "POST")
    public Result<Object> handlePaymentTimeout(@PathVariable("paymentId") String paymentId, HttpServletRequest request) {
        try {
            if (paymentId == null || paymentId.trim().isEmpty()) {
                return Result.fail("400", "支付ID不能为空");
            }

            logger.debug("处理支付超时请求: paymentId={}", paymentId);

            PaymentOperationResult result = paymentService.handlePaymentTimeout(paymentId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("处理支付超时失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
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
