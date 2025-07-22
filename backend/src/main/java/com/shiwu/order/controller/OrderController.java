package com.shiwu.order.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.web.BaseController;
import com.shiwu.order.model.OrderCreateDTO;
import com.shiwu.order.model.OrderOperationResult;
import com.shiwu.order.model.ProcessReturnRequestDTO;
import com.shiwu.order.model.ReturnRequestDTO;
import com.shiwu.order.service.OrderService;
import com.shiwu.order.service.impl.OrderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 覆盖OrderService的所有功能，确保功能完整性
 */
@Controller
@WebServlet("/api/orders/*")
public class OrderController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    public OrderController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.orderService = new OrderServiceImpl();
        logger.info("OrderController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        logger.info("OrderController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 创建订单 - MVC版本
     */
    @RequestMapping(value = "/", method = "POST")
    public Result<Object> createOrder(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            OrderCreateDTO dto = JsonUtil.fromJson(requestBody, OrderCreateDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理创建订单请求: userId={}, productIds={}", userId, dto.getProductIds());

            OrderOperationResult result = orderService.createOrder(dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("创建订单失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 获取买家订单列表 - MVC版本
     */
    @RequestMapping(value = "/buyer", method = "GET")
    public Result<Object> getBuyerOrders(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理获取买家订单列表请求: userId={}", userId);

            OrderOperationResult result = orderService.getBuyerOrders(userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("获取买家订单列表失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 获取卖家订单列表 - MVC版本
     */
    @RequestMapping(value = "/seller", method = "GET")
    public Result<Object> getSellerOrders(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理获取卖家订单列表请求: userId={}", userId);

            OrderOperationResult result = orderService.getSellerOrders(userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("获取卖家订单列表失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 根据ID获取订单详情 - MVC版本
     */
    @RequestMapping(value = "/{id}", method = "GET")
    public Result<Object> getOrderById(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            logger.debug("处理获取订单详情请求: userId={}, orderId={}", userId, orderId);

            OrderOperationResult result = orderService.getOrderById(orderId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("获取订单详情失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 确认收货 - MVC版本
     */
    @RequestMapping(value = "/{id}/confirm", method = "POST")
    public Result<Object> confirmReceipt(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            logger.debug("处理确认收货请求: userId={}, orderId={}", userId, orderId);

            OrderOperationResult result = orderService.confirmReceipt(orderId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("确认收货失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 更新订单状态 - MVC版本
     */
    @RequestMapping(value = "/{id}/status", method = "PUT")
    public Result<Object> updateOrderStatus(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = JsonUtil.fromJson(requestBody, Map.class);
            if (requestMap == null || !requestMap.containsKey("status")) {
                return Result.fail("400", "请求参数格式错误");
            }

            Integer status = ((Number) requestMap.get("status")).intValue();

            logger.debug("处理更新订单状态请求: userId={}, orderId={}, status={}", userId, orderId, status);

            OrderOperationResult result = orderService.updateOrderStatus(orderId, status, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("更新订单状态失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 卖家发货 - MVC版本
     */
    @RequestMapping(value = "/{id}/ship", method = "POST")
    public Result<Object> shipOrder(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            logger.debug("处理卖家发货请求: userId={}, orderId={}", userId, orderId);

            OrderOperationResult result = orderService.shipOrder(orderId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("卖家发货失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 买家申请退货 - MVC版本
     */
    @RequestMapping(value = "/{id}/return", method = "POST")
    public Result<Object> applyForReturn(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ReturnRequestDTO dto = JsonUtil.fromJson(requestBody, ReturnRequestDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理买家申请退货请求: userId={}, orderId={}", userId, orderId);

            OrderOperationResult result = orderService.applyForReturn(orderId, dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("买家申请退货失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 卖家处理退货申请 - MVC版本
     */
    @RequestMapping(value = "/{id}/process-return", method = "POST")
    public Result<Object> processReturnRequest(@PathVariable("id") Long orderId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (orderId == null || orderId <= 0) {
                return Result.fail("400", "订单ID无效");
            }

            String requestBody = readRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ProcessReturnRequestDTO dto = JsonUtil.fromJson(requestBody, ProcessReturnRequestDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理卖家处理退货申请请求: userId={}, orderId={}", userId, orderId);

            OrderOperationResult result = orderService.processReturnRequest(orderId, dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("卖家处理退货申请失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 支付成功后更新订单状态 - MVC版本
     */
    @RequestMapping(value = "/payment-success", method = "POST")
    public Result<Object> updateOrderStatusAfterPayment(HttpServletRequest request) {
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
            if (requestMap == null || !requestMap.containsKey("orderIds") || !requestMap.containsKey("paymentId")) {
                return Result.fail("400", "请求参数格式错误");
            }

            @SuppressWarnings("unchecked")
            List<Integer> orderIdInts = (List<Integer>) requestMap.get("orderIds");
            List<Long> orderIds = orderIdInts.stream()
                    .map(Integer::longValue)
                    .collect(java.util.stream.Collectors.toList());
            String paymentId = (String) requestMap.get("paymentId");

            logger.debug("处理支付成功后更新订单状态请求: userId={}, orderIds={}, paymentId={}", userId, orderIds, paymentId);

            OrderOperationResult result = orderService.updateOrderStatusAfterPayment(orderIds, paymentId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("支付成功后更新订单状态失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误");
        }
    }

    /**
     * 支付失败后取消订单 - MVC版本
     */
    @RequestMapping(value = "/payment-cancel", method = "POST")
    public Result<Object> cancelOrdersAfterPaymentFailure(HttpServletRequest request) {
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
            if (requestMap == null || !requestMap.containsKey("orderIds") || !requestMap.containsKey("reason")) {
                return Result.fail("400", "请求参数格式错误");
            }

            @SuppressWarnings("unchecked")
            List<Integer> orderIdInts = (List<Integer>) requestMap.get("orderIds");
            List<Long> orderIds = orderIdInts.stream()
                    .map(Integer::longValue)
                    .collect(java.util.stream.Collectors.toList());
            String reason = (String) requestMap.get("reason");

            logger.debug("处理支付失败后取消订单请求: userId={}, orderIds={}, reason={}", userId, orderIds, reason);

            OrderOperationResult result = orderService.cancelOrdersAfterPaymentFailure(orderIds, reason);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("支付失败后取消订单失败: {}", e.getMessage(), e);
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
