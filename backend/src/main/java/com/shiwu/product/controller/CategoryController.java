package com.shiwu.product.controller;

import com.shiwu.common.result.Result;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.web.BaseController;
import com.shiwu.product.model.CategoryVO;
import com.shiwu.product.service.ProductService;
import com.shiwu.product.service.impl.ProductServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import java.util.List;

/**
 * 商品分类控制器 - 精简版MVC框架
 *
 * 只包含MVC注解方法，移除传统Servlet代码
 * 大幅简化代码结构，提高可维护性
 */
@Controller
@WebServlet("/api/v2/categories/*")
public class CategoryController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private ProductService productService;

    public CategoryController() {
        // 为了兼容测试，在无参构造函数中初始化Service
        this.productService = new ProductServiceImpl();
        logger.info("CategoryController初始化完成 - 精简版MVC框架");
    }

    // 兼容性构造函数，支持渐进式迁移
    public CategoryController(ProductService productService) {
        this.productService = productService;
        logger.info("CategoryController初始化完成 - 使用兼容性构造函数");
    }

    // ==================== MVC框架注解方法 ====================

    /**
     * 获取所有商品分类 - MVC版本
     */
    @RequestMapping(value = "/", method = "GET")
    public Result<List<CategoryVO>> getAllCategories() {
        try {
            logger.debug("处理获取所有商品分类请求");
            List<CategoryVO> categories = productService.getAllCategories();
            return Result.success(categories);
        } catch (Exception e) {
            logger.error("获取商品分类失败: {}", e.getMessage(), e);
            return Result.fail("500", "系统错误，请稍后再试");
        }
    }
}