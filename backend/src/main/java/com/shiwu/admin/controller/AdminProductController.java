package com.shiwu.admin.controller;

import com.shiwu.admin.model.AdminProductQueryDTO;
import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.impl.AdminServiceImpl;
import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.product.service.AdminProductService;
import com.shiwu.product.service.impl.AdminProductServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 管理员商品管理控制器 - 重构为MVC框架
 * 使用注解驱动，大幅简化路由分发代码
 */
@Controller
@WebServlet("/api/admin/products/*")
public class AdminProductController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);

    private final AdminProductService adminProductService;
    private final AdminService adminService;

    public AdminProductController() {
        this.adminProductService = new AdminProductServiceImpl();
        this.adminService = new AdminServiceImpl();
        logger.info("AdminProductController初始化完成 - 使用MVC框架");
    }

    // 用于测试的构造函数，支持依赖注入
    public AdminProductController(AdminProductService adminProductService, AdminService adminService) {
        this.adminProductService = adminProductService;
        this.adminService = adminService;
        logger.info("AdminProductController初始化完成 - 使用依赖注入");
    }

    /**
     * 查询商品列表
     * API: GET /api/admin/products/
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Map<String, Object>> getProducts(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                   @RequestParam(value = "size", defaultValue = "10") Integer size,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   HttpServletRequest request) {
        logger.info("查询商品列表请求: page={}, size={}, keyword={}, status={}", page, size, keyword, status);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 构建查询条件
            AdminProductQueryDTO queryDTO = new AdminProductQueryDTO();
            queryDTO.setPageNum(page);
            queryDTO.setPageSize(size);
            queryDTO.setKeyword(keyword);
            if (status != null && !status.trim().isEmpty()) {
                try {
                    queryDTO.setStatus(Integer.parseInt(status));
                } catch (NumberFormatException e) {
                    logger.warn("无效的状态参数: {}", status);
                }
            }

            // 查询商品列表
            Map<String, Object> result = adminProductService.findProducts(queryDTO);

            logger.info("查询商品列表成功: page={}, size={}, total={}", page, size, result.get("total"));
            return Result.success(result);

        } catch (Exception e) {
            logger.error("查询商品列表失败", e);
            return Result.fail("PRODUCT001", "查询商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品详情
     * API: GET /api/admin/products/{productId}
     */
    @RequestMapping(value = "/{productId}", method = "GET")
    public Result<Object> getProductDetail(@PathVariable("productId") Long productId,
                                          HttpServletRequest request) {
        logger.info("获取商品详情请求: productId={}", productId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 获取商品详情
            Map<String, Object> productDetail = adminProductService.getProductDetail(productId, adminId);
            if (productDetail == null) {
                logger.warn("商品不存在: productId={}", productId);
                return Result.fail("PRODUCT002", "商品不存在");
            }

            logger.info("获取商品详情成功: productId={}", productId);
            return Result.success(productDetail);

        } catch (Exception e) {
            logger.error("获取商品详情失败: productId={}", productId, e);
            return Result.fail("PRODUCT003", "获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过商品
     * API: PUT /api/admin/products/{productId}/approve
     */
    @RequestMapping(value = "/{productId}/approve", method = "PUT")
    public Result<String> approveProduct(@PathVariable("productId") Long productId,
                                        HttpServletRequest request) {
        logger.info("审核通过商品请求: productId={}", productId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体获取审核备注
            String reason = "";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = parseRequestBody(request, Map.class);
                reason = requestMap != null ? (String) requestMap.get("reason") : "";
            } catch (Exception e) {
                logger.debug("解析请求体失败，使用默认值: {}", e.getMessage());
                reason = "";
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行审核通过操作
            boolean success = adminProductService.approveProduct(productId, adminId, reason, ipAddress, userAgent);

            if (success) {
                logger.info("审核通过商品成功: productId={}, adminId={}, reason={}", productId, adminId, reason);
                return Result.success("商品审核通过");
            } else {
                logger.warn("审核通过商品失败: productId={}, adminId={}", productId, adminId);
                return Result.fail("PRODUCT004", "审核通过商品失败");
            }

        } catch (Exception e) {
            logger.error("审核通过商品异常: productId={}", productId, e);
            return Result.fail("PRODUCT005", "审核通过商品失败: " + e.getMessage());
        }
    }

    /**
     * 审核拒绝商品
     * API: PUT /api/admin/products/{productId}/reject
     */
    @RequestMapping(value = "/{productId}/reject", method = "PUT")
    public Result<String> rejectProduct(@PathVariable("productId") Long productId,
                                       HttpServletRequest request) {
        logger.info("审核拒绝商品请求: productId={}", productId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体获取拒绝原因
            String reason = "不符合平台规范";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = parseRequestBody(request, Map.class);
                reason = requestMap != null ? (String) requestMap.get("reason") : "不符合平台规范";
            } catch (Exception e) {
                logger.debug("解析请求体失败，使用默认值: {}", e.getMessage());
                reason = "不符合平台规范";
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行审核拒绝操作
            boolean success = adminProductService.rejectProduct(productId, adminId, reason, ipAddress, userAgent);

            if (success) {
                logger.info("审核拒绝商品成功: productId={}, adminId={}, reason={}", productId, adminId, reason);
                return Result.success("商品审核拒绝");
            } else {
                logger.warn("审核拒绝商品失败: productId={}, adminId={}", productId, adminId);
                return Result.fail("PRODUCT006", "审核拒绝商品失败");
            }

        } catch (Exception e) {
            logger.error("审核拒绝商品异常: productId={}", productId, e);
            return Result.fail("PRODUCT007", "审核拒绝商品失败: " + e.getMessage());
        }
    }

    /**
     * 下架商品
     * API: PUT /api/admin/products/{productId}/delist
     */
    @RequestMapping(value = "/{productId}/delist", method = "PUT")
    public Result<String> delistProduct(@PathVariable("productId") Long productId,
                                       HttpServletRequest request) {
        logger.info("下架商品请求: productId={}", productId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 解析请求体获取下架原因
            String reason = "违规商品";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = parseRequestBody(request, Map.class);
                reason = requestMap != null ? (String) requestMap.get("reason") : "违规商品";
            } catch (Exception e) {
                logger.debug("解析请求体失败，使用默认值: {}", e.getMessage());
                reason = "违规商品";
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行下架操作
            boolean success = adminProductService.delistProduct(productId, adminId, reason, ipAddress, userAgent);

            if (success) {
                logger.info("下架商品成功: productId={}, adminId={}, reason={}", productId, adminId, reason);
                return Result.success("商品下架成功");
            } else {
                logger.warn("下架商品失败: productId={}, adminId={}", productId, adminId);
                return Result.fail("PRODUCT008", "下架商品失败");
            }

        } catch (Exception e) {
            logger.error("下架商品异常: productId={}", productId, e);
            return Result.fail("PRODUCT009", "下架商品失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     * API: DELETE /api/admin/products/{productId}
     */
    @RequestMapping(value = "/{productId}", method = "DELETE")
    public Result<String> deleteProduct(@PathVariable("productId") Long productId,
                                       HttpServletRequest request) {
        logger.info("删除商品请求: productId={}", productId);

        try {
            // 验证管理员权限
            Long adminId = validateAdminPermission(request);
            if (adminId == null) {
                return Result.fail("AUTH001", "权限验证失败");
            }

            // 获取客户端信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行删除操作
            boolean success = adminProductService.deleteProduct(productId, adminId, ipAddress, userAgent);

            if (success) {
                logger.info("删除商品成功: productId={}, adminId={}", productId, adminId);
                return Result.success("商品删除成功");
            } else {
                logger.warn("删除商品失败: productId={}, adminId={}", productId, adminId);
                return Result.fail("PRODUCT010", "删除商品失败");
            }

        } catch (Exception e) {
            logger.error("删除商品异常: productId={}", productId, e);
            return Result.fail("PRODUCT011", "删除商品失败: " + e.getMessage());
        }
    }

    /**
     * 验证管理员权限
     */
    private Long validateAdminPermission(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!JwtUtil.validateToken(token)) {
                logger.warn("Token验证失败");
                return null;
            }

            Long adminId = JwtUtil.getUserIdFromToken(token);
            if (adminId == null) {
                logger.warn("无效的令牌");
                return null;
            }

            if (!adminService.hasPermission(adminId, "ADMIN")) {
                logger.warn("权限不足，管理员ID: {}", adminId);
                return null;
            }

            return adminId;
        } catch (Exception e) {
            logger.error("权限验证异常", e);
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}