package com.shiwu.product.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.RequestUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.product.model.Product;
import com.shiwu.product.model.ProductCardVO;
import com.shiwu.product.model.ProductCreateDTO;
import com.shiwu.product.model.ProductDetailVO;
import com.shiwu.product.service.ProductService;
import com.shiwu.product.service.impl.ProductServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 大幅简化代码结构，提高可维护性
 */
@Controller
@WebServlet("/api/v2/products/*")
public class ProductController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    public ProductController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.productService = new ProductServiceImpl();
        logger.info("ProductController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public ProductController(ProductService productService) {
        this.productService = productService;
        logger.info("ProductController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 获取商品列表 - MVC版本
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<Map<String, Object>> getProducts(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) String minPrice,
            @RequestParam(value = "maxPrice", required = false) String maxPrice,
            @RequestParam(value = "sortBy", defaultValue = "createTime") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {

        try {
            // 参数转换
            BigDecimal minPriceDecimal = parsePrice(minPrice);
            BigDecimal maxPriceDecimal = parsePrice(maxPrice);
            Integer categoryId = parseCategory(category);

            if (minPriceDecimal == null && minPrice != null && !minPrice.trim().isEmpty()) {
                return Result.fail("400", "无效的最低价格格式");
            }
            if (maxPriceDecimal == null && maxPrice != null && !maxPrice.trim().isEmpty()) {
                return Result.fail("400", "无效的最高价格格式");
            }
            if (categoryId == null && category != null && !category.trim().isEmpty()) {
                return Result.fail("400", "无效的分类ID格式");
            }

            Map<String, Object> result = productService.findProducts(
                keyword, categoryId, minPriceDecimal, maxPriceDecimal,
                sortBy, sortOrder, page, size
            );

            return Result.success(result);

        } catch (Exception e) {
            logger.error("获取商品列表失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 获取商品详情 - MVC版本
     */
    @RequestMapping(value = "/{id}", method = "GET")
    public Result<ProductDetailVO> getProductDetail(@PathVariable("id") Long productId, HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            ProductDetailVO productDetail = productService.getProductDetailById(productId, currentUserId);

            if (productDetail == null) {
                return Result.fail("404", "商品不存在");
            }

            return Result.success(productDetail);

        } catch (Exception e) {
            logger.error("获取商品详情失败: productId={}, error={}", productId, e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 获取我的商品列表 - MVC版本
     */
    @RequestMapping(value = "/my", method = "GET")
    public Result<List<ProductCardVO>> getMyProducts(
            @RequestParam(value = "status", required = false) Integer status,
            HttpServletRequest request) {

        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            List<ProductCardVO> products = productService.getProductsBySellerIdAndStatus(currentUserId, status);
            return Result.success(products);

        } catch (Exception e) {
            logger.error("获取我的商品列表失败: userId={}, error={}", RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 创建商品 - MVC版本
     */
    @RequestMapping(value = "/", method = "POST")
    public Result<Product> createProduct(HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ProductCreateDTO productCreateDTO = parseProductCreateDTO(requestBody);
            if (productCreateDTO == null) {
                return Result.fail("400", "无效的JSON格式");
            }

            if (productCreateDTO.getTitle() == null || productCreateDTO.getTitle().trim().isEmpty()) {
                return Result.fail("400", "商品标题不能为空");
            }

            Long productId = productService.createProduct(productCreateDTO, currentUserId);
            if (productId == null) {
                return Result.fail("500", "创建商品失败");
            }

            Product product = productService.getProductById(productId);
            return Result.success(product);

        } catch (Exception e) {
            logger.error("创建商品失败: userId={}, error={}", RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 保存草稿 - MVC版本
     */
    @RequestMapping(value = "/draft", method = "POST")
    public Result<Product> saveDraft(HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ProductCreateDTO productCreateDTO = parseProductCreateDTO(requestBody);
            if (productCreateDTO == null) {
                return Result.fail("400", "无效的JSON格式");
            }

            // 设置为草稿状态
            productCreateDTO.setAction(ProductCreateDTO.ACTION_SAVE_DRAFT);

            Long productId = productService.createProduct(productCreateDTO, currentUserId);
            if (productId == null) {
                return Result.fail("500", "保存草稿失败");
            }

            Product product = productService.getProductById(productId);
            return Result.success(product);

        } catch (Exception e) {
            logger.error("保存草稿失败: userId={}, error={}", RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 上传商品图片 - MVC版本
     */
    @RequestMapping(value = "/images", method = "POST")
    public Result<Map<String, Object>> uploadProductImage(HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            String productIdStr = request.getParameter("productId");
            if (productIdStr == null || productIdStr.trim().isEmpty()) {
                return Result.fail("400", "商品ID不能为空");
            }

            Long productId = parseProductId(productIdStr);
            if (productId == null) {
                return Result.fail("400", "无效的商品ID格式");
            }

            if (!productService.isProductOwnedBySeller(productId, currentUserId)) {
                return Result.fail("403", "无权限上传此商品的图片");
            }

            String imageName = request.getParameter("imageName");
            String contentType = request.getParameter("contentType");
            String isMainStr = request.getParameter("isMain");
            Boolean isMain = Boolean.parseBoolean(isMainStr);

            if (imageName == null || imageName.trim().isEmpty()) {
                return Result.fail("400", "图片名称不能为空");
            }

            String imageUrl = productService.uploadProductImage(productId, imageName, null, contentType, isMain, currentUserId);
            
            if (imageUrl == null) {
                return Result.fail("500", "图片上传失败");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("productId", productId);
            result.put("isMain", isMain);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("上传商品图片失败: userId={}, error={}", RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 更新商品 - MVC版本
     */
    @RequestMapping(value = "/{id}", method = "POST")
    public Result<Product> updateProduct(@PathVariable("id") Long productId, HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            ProductCreateDTO productUpdateDTO = parseProductCreateDTO(requestBody);
            if (productUpdateDTO == null) {
                return Result.fail("400", "无效的JSON格式");
            }

            if (productUpdateDTO.getTitle() == null || productUpdateDTO.getTitle().trim().isEmpty()) {
                return Result.fail("400", "商品标题不能为空");
            }

            Product existingProduct = productService.getProductById(productId);
            if (existingProduct == null) {
                return Result.fail("404", "商品不存在");
            }

            if (!productService.isProductOwnedBySeller(productId, currentUserId)) {
                return Result.fail("403", "无权限修改此商品");
            }

            // 更新商品信息
            existingProduct.setTitle(productUpdateDTO.getTitle());
            existingProduct.setDescription(productUpdateDTO.getDescription());
            existingProduct.setPrice(productUpdateDTO.getPrice());
            existingProduct.setCategoryId(productUpdateDTO.getCategoryId());

            boolean success = productService.updateProduct(existingProduct, currentUserId);
            if (!success) {
                return Result.fail("500", "更新商品失败");
            }

            return Result.success(existingProduct);

        } catch (Exception e) {
            logger.error("更新商品失败: productId={}, userId={}, error={}", productId, RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 更新商品状态 - MVC版本
     */
    @RequestMapping(value = "/{id}/status", method = "PUT")
    public Result<String> updateProductStatus(@PathVariable("id") Long productId, HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> statusData = JsonUtil.fromJson(requestBody, Map.class);
            if (statusData == null) {
                return Result.fail("400", "无效的JSON格式");
            }

            Object statusObj = statusData.get("status");
            if (statusObj == null) {
                return Result.fail("400", "状态不能为空");
            }

            Integer newStatus = parseStatus(statusObj.toString());
            if (newStatus == null) {
                return Result.fail("400", "无效的状态格式");
            }

            if (!productService.isProductOwnedBySeller(productId, currentUserId)) {
                return Result.fail("403", "无权限修改此商品状态");
            }

            boolean success = productService.updateProductStatus(productId, newStatus, currentUserId);
            if (!success) {
                return Result.fail("500", "更新商品状态失败");
            }

            String statusMessage = getStatusMessage(newStatus);
            return Result.success("商品状态已更新为：" + statusMessage);

        } catch (Exception e) {
            logger.error("更新商品状态失败: productId={}, userId={}, error={}", productId, RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    /**
     * 删除商品 - MVC版本
     */
    @RequestMapping(value = "/{id}", method = "DELETE")
    public Result<String> deleteProduct(@PathVariable("id") Long productId, HttpServletRequest request) {
        try {
            Long currentUserId = RequestUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.fail("401", "未登录或登录已过期");
            }

            if (!productService.isProductOwnedBySeller(productId, currentUserId)) {
                return Result.fail("403", "无权限删除此商品");
            }

            boolean success = productService.deleteProduct(productId, currentUserId);
            if (!success) {
                return Result.fail("500", "删除商品失败");
            }

            return Result.success("商品删除成功");

        } catch (Exception e) {
            logger.error("删除商品失败: productId={}, userId={}, error={}", productId, RequestUtil.getCurrentUserId(request), e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }

    // ==================== 工具方法 ====================

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

    /**
     * 解析商品创建DTO
     */
    private ProductCreateDTO parseProductCreateDTO(String requestBody) {
        try {
            return JsonUtil.fromJson(requestBody, ProductCreateDTO.class);
        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析价格参数
     */
    private BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析分类参数
     */
    private Integer parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(categoryStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析商品ID
     */
    private Long parseProductId(String productIdStr) {
        try {
            return Long.parseLong(productIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析状态参数
     */
    private Integer parseStatus(String statusStr) {
        try {
            return Integer.parseInt(statusStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取状态描述信息
     */
    private String getStatusMessage(Integer status) {
        switch (status) {
            case 0: return "待审核";
            case 1: return "在售";
            case 2: return "已售出";
            case 3: return "已下架";
            case 4: return "草稿";
            case 5: return "锁定";
            default: return "未知状态";
        }
    }
}
