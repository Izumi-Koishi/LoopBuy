package com.shiwu.cart.controller;

import com.shiwu.cart.model.CartAddDTO;
import com.shiwu.cart.service.CartService;
import com.shiwu.cart.service.impl.CartServiceImpl;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 大幅简化代码结构，提高可维护性
 */
@Controller
@WebServlet("/api/v2/cart/*")
public class CartController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    public CartController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.cartService = new CartServiceImpl();
        logger.info("CartController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public CartController(CartService cartService) {
        this.cartService = cartService;
        logger.info("CartController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 获取购物车 - MVC版本
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Object> getCart(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理获取购物车请求: userId={}", userId);
            com.shiwu.cart.model.CartOperationResult result = cartService.getCart(userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("获取购物车失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 添加商品到购物车 - MVC版本
     */
    @RequestMapping(value = "/add", method = "POST")
    public Result<Object> addToCart(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            CartAddDTO dto = JsonUtil.fromJson(requestBody, CartAddDTO.class);
            if (dto == null) {
                return Result.fail("400", "请求参数格式错误");
            }

            logger.debug("处理添加商品到购物车请求: userId={}, productId={}, quantity={}",
                        userId, dto.getProductId(), dto.getQuantity());

            com.shiwu.cart.model.CartOperationResult result = cartService.addToCart(dto, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("添加商品到购物车失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 从购物车移除商品 - MVC版本
     */
    @RequestMapping(value = "/remove/{productId}", method = "DELETE")
    public Result<Object> removeFromCart(@PathVariable("productId") Long productId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            if (productId == null || productId <= 0) {
                return Result.fail("400", "商品ID无效");
            }

            logger.debug("处理从购物车移除商品请求: userId={}, productId={}", userId, productId);

            com.shiwu.cart.model.CartOperationResult result = cartService.removeFromCart(productId, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("从购物车移除商品失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 批量从购物车移除商品 - MVC版本
     */
    @RequestMapping(value = "/batch-remove", method = "POST")
    public Result<Object> batchRemoveFromCart(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = JsonUtil.fromJson(requestBody, Map.class);
            if (requestMap == null || !requestMap.containsKey("productIds")) {
                return Result.fail("400", "请求参数格式错误");
            }

            @SuppressWarnings("unchecked")
            List<Integer> productIdInts = (List<Integer>) requestMap.get("productIds");
            List<Long> productIds = productIdInts.stream()
                    .map(Integer::longValue)
                    .collect(Collectors.toList());

            logger.debug("处理批量从购物车移除商品请求: userId={}, productIds={}", userId, productIds);

            com.shiwu.cart.model.CartOperationResult result = cartService.batchRemoveFromCart(productIds, userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("批量从购物车移除商品失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 清空购物车 - MVC版本
     */
    @RequestMapping(value = "/clear", method = "POST")
    public Result<Object> clearCart(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理清空购物车请求: userId={}", userId);

            com.shiwu.cart.model.CartOperationResult result = cartService.clearCart(userId);

            if (result.isSuccess()) {
                return Result.success(result.getData());
            } else {
                return Result.fail(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("清空购物车失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 获取购物车商品总数 - MVC版本
     */
    @RequestMapping(value = "/count", method = "GET")
    public Result<Object> getCartItemCount(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.fail("401", "用户未登录");
            }

            logger.debug("处理获取购物车商品总数请求: userId={}", userId);

            int count = cartService.getCartItemCount(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("totalItems", count);

            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取购物车商品总数失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
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
    protected String getRequestBody(HttpServletRequest request) {
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
