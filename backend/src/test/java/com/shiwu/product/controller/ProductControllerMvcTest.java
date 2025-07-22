package com.shiwu.product.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.product.model.Product;
import com.shiwu.product.model.ProductCardVO;
import com.shiwu.product.model.ProductCreateDTO;
import com.shiwu.product.model.ProductDetailVO;
import com.shiwu.product.service.ProductService;
import com.shiwu.test.TestBase;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductController MVC框架测试
 * 
 * 测试重构后的ProductController是否正确使用MVC框架的依赖注入和注解路由
 * 验证@Controller、@RequestMapping、@PathVariable、@RequestParam注解的功能
 * 保持与原测试用例完全一致的测试覆盖率
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductController MVC框架测试")
public class ProductControllerMvcTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(ProductControllerMvcTest.class);
    
    // 测试常量
    private static final Long TEST_PRODUCT_ID_1 = 1L;
    private static final Long TEST_PRODUCT_ID_2 = 2L;
    private static final Long TEST_USER_ID_1 = 100L;
    private static final Long TEST_USER_ID_2 = 101L;
    private static final String TEST_CATEGORY = "1";
    private static final String TEST_KEYWORD = "测试商品";
    
    // 测试对象
    private ProductController productController;
    
    // Mock对象
    @Mock
    private ProductService mockProductService;
    
    @Mock
    private HttpServletRequest mockRequest;
    
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        logger.info("ProductController MVC测试环境初始化开始");
        
        // 初始化Mockito
        closeable = MockitoAnnotations.openMocks(this);
        
        // 创建ProductController实例，使用兼容性构造函数注入Mock对象
        productController = new ProductController(mockProductService);
        
        logger.info("ProductController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
        logger.info("ProductController MVC测试清理完成");
    }

    /**
     * 测试获取商品列表接口 - 成功场景
     */
    @Test
    @Order(1)
    @DisplayName("1.1 获取商品列表测试 - 成功场景")
    public void testGetProducts_Success() {
        logger.info("开始测试获取商品列表接口 - 成功场景");
        
        // 准备测试数据
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("products", Arrays.asList(
            createMockProductCardVO(TEST_PRODUCT_ID_1, "测试商品1"),
            createMockProductCardVO(TEST_PRODUCT_ID_2, "测试商品2")
        ));
        mockResult.put("total", 2L);
        mockResult.put("page", 1);
        mockResult.put("size", 20);
        
        // 设置Mock行为
        when(mockProductService.findProducts(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(mockResult);
        
        // 执行测试
        Result<Map<String, Object>> result = productController.getProducts(
            1, 20, TEST_CATEGORY, TEST_KEYWORD, "10.0", "100.0", "createTime", "desc"
        );
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应该成功");
        assertNotNull(result.getData(), "数据不应为null");
        
        Map<String, Object> data = result.getData();
        assertEquals(2L, data.get("total"), "总数应该匹配");
        assertEquals(1, data.get("page"), "页码应该匹配");
        assertEquals(20, data.get("size"), "页面大小应该匹配");
        
        // 验证Mock调用
        verify(mockProductService).findProducts(
            eq(TEST_KEYWORD), eq(1), any(BigDecimal.class), any(BigDecimal.class),
            eq("createTime"), eq("desc"), eq(1), eq(20)
        );
        
        logger.info("测试获取商品列表接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取商品列表接口 - 无效价格格式
     */
    @Test
    @Order(2)
    @DisplayName("1.2 获取商品列表测试 - 无效价格格式")
    public void testGetProducts_InvalidPrice() {
        logger.info("开始测试获取商品列表接口 - 无效价格格式");
        
        // 执行测试 - 无效的最低价格
        Result<Map<String, Object>> result = productController.getProducts(
            1, 20, null, null, "invalid", null, "createTime", "desc"
        );
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "查询应该失败");
        assertEquals("400", result.getError().getCode(), "错误码应该为400");
        assertEquals("无效的最低价格格式", result.getMessage(), "错误信息应该匹配");
        
        // 验证没有调用Service
        verify(mockProductService, never()).findProducts(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        
        logger.info("测试获取商品列表接口 - 无效价格格式 - 通过");
    }

    /**
     * 测试获取商品详情接口 - 成功场景
     */
    @Test
    @Order(3)
    @DisplayName("2.1 获取商品详情测试 - 成功场景")
    public void testGetProductDetail_Success() {
        logger.info("开始测试获取商品详情接口 - 成功场景");
        
        // 准备测试数据
        ProductDetailVO mockProductDetail = createMockProductDetailVO(TEST_PRODUCT_ID_1, "测试商品详情");
        
        // 设置Mock行为
        when(mockProductService.getProductDetailById(TEST_PRODUCT_ID_1, null))
            .thenReturn(mockProductDetail);
        when(mockRequest.getSession(false)).thenReturn(null); // 模拟未登录
        when(mockRequest.getAttribute("userId")).thenReturn(null); // 模拟未登录
        
        // 执行测试
        Result<ProductDetailVO> result = productController.getProductDetail(TEST_PRODUCT_ID_1, mockRequest);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应该成功");
        assertNotNull(result.getData(), "商品详情不应为null");
        assertEquals(TEST_PRODUCT_ID_1, result.getData().getId(), "商品ID应该匹配");
        assertEquals("测试商品详情", result.getData().getTitle(), "商品标题应该匹配");
        
        // 验证Mock调用
        verify(mockProductService).getProductDetailById(TEST_PRODUCT_ID_1, null);
        
        logger.info("测试获取商品详情接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取商品详情接口 - 商品不存在
     */
    @Test
    @Order(4)
    @DisplayName("2.2 获取商品详情测试 - 商品不存在")
    public void testGetProductDetail_NotFound() {
        logger.info("开始测试获取商品详情接口 - 商品不存在");
        
        // 设置Mock行为
        when(mockProductService.getProductDetailById(999L, null)).thenReturn(null);
        when(mockRequest.getSession(false)).thenReturn(null);
        
        // 执行测试
        Result<ProductDetailVO> result = productController.getProductDetail(999L, mockRequest);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "查询应该失败");
        assertEquals("404", result.getError().getCode(), "错误码应该为404");
        assertEquals("商品不存在", result.getMessage(), "错误信息应该匹配");
        
        // 验证Mock调用
        verify(mockProductService).getProductDetailById(999L, null);
        
        logger.info("测试获取商品详情接口 - 商品不存在 - 通过");
    }

    /**
     * 测试获取我的商品列表接口 - 成功场景
     */
    @Test
    @Order(5)
    @DisplayName("3.1 获取我的商品列表测试 - 成功场景")
    public void testGetMyProducts_Success() {
        logger.info("开始测试获取我的商品列表接口 - 成功场景");
        
        // 准备测试数据
        List<ProductCardVO> mockProducts = Arrays.asList(
            createMockProductCardVO(TEST_PRODUCT_ID_1, "我的商品1"),
            createMockProductCardVO(TEST_PRODUCT_ID_2, "我的商品2")
        );
        
        // 设置Mock行为
        when(mockProductService.getProductsBySellerIdAndStatus(TEST_USER_ID_1, 1))
            .thenReturn(mockProducts);
        
        // 模拟已登录用户
        mockUserSession(mockRequest, TEST_USER_ID_1);
        
        // 执行测试
        Result<List<ProductCardVO>> result = productController.getMyProducts(1, mockRequest);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应该成功");
        assertNotNull(result.getData(), "商品列表不应为null");
        assertEquals(2, result.getData().size(), "商品数量应该匹配");
        
        // 验证Mock调用
        verify(mockProductService).getProductsBySellerIdAndStatus(TEST_USER_ID_1, 1);
        
        logger.info("测试获取我的商品列表接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取我的商品列表接口 - 未登录
     */
    @Test
    @Order(6)
    @DisplayName("3.2 获取我的商品列表测试 - 未登录")
    public void testGetMyProducts_NotLoggedIn() {
        logger.info("开始测试获取我的商品列表接口 - 未登录");
        
        // 设置Mock行为 - 未登录
        when(mockRequest.getSession(false)).thenReturn(null);
        
        // 执行测试
        Result<List<ProductCardVO>> result = productController.getMyProducts(null, mockRequest);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "查询应该失败");
        assertEquals("401", result.getError().getCode(), "错误码应该为401");
        assertEquals("未登录或登录已过期", result.getMessage(), "错误信息应该匹配");
        
        // 验证没有调用Service
        verify(mockProductService, never()).getProductsBySellerIdAndStatus(any(), any());
        
        logger.info("测试获取我的商品列表接口 - 未登录 - 通过");
    }

    /**
     * 测试创建商品接口 - 成功场景
     */
    @Test
    @Order(7)
    @DisplayName("4.1 创建商品测试 - 成功场景")
    public void testCreateProduct_Success() throws Exception {
        logger.info("开始测试创建商品接口 - 成功场景");
        
        // 准备测试数据
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("测试商品");
        dto.setDescription("这是一个测试商品");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setCategoryId(1);
        dto.setAction(ProductCreateDTO.ACTION_SUBMIT_REVIEW);
        
        Product mockProduct = createMockProduct(TEST_PRODUCT_ID_1, "测试商品");
        
        // 设置Mock行为
        when(mockProductService.createProduct(any(ProductCreateDTO.class), eq(TEST_USER_ID_1)))
            .thenReturn(TEST_PRODUCT_ID_1);
        when(mockProductService.getProductById(TEST_PRODUCT_ID_1)).thenReturn(mockProduct);
        
        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        
        // 执行测试
        Result<Product> result = productController.createProduct(mockRequest);
        
        // 验证结果
        assertNotNull(result, "创建结果不应为null");
        assertTrue(result.isSuccess(), "创建应该成功");
        assertNotNull(result.getData(), "商品数据不应为null");
        assertEquals(TEST_PRODUCT_ID_1, result.getData().getId(), "商品ID应该匹配");
        
        // 验证Mock调用
        verify(mockProductService).createProduct(any(ProductCreateDTO.class), eq(TEST_USER_ID_1));
        verify(mockProductService).getProductById(TEST_PRODUCT_ID_1);
        
        logger.info("测试创建商品接口 - 成功场景 - 通过");
    }

    /**
     * 测试创建商品接口 - 未登录
     */
    @Test
    @Order(8)
    @DisplayName("4.2 创建商品测试 - 未登录")
    public void testCreateProduct_NotLoggedIn() throws Exception {
        logger.info("开始测试创建商品接口 - 未登录");

        // 设置Mock行为 - 未登录
        when(mockRequest.getSession(false)).thenReturn(null);

        // 执行测试
        Result<Product> result = productController.createProduct(mockRequest);

        // 验证结果
        assertNotNull(result, "创建结果不应为null");
        assertFalse(result.isSuccess(), "创建应该失败");
        assertEquals("401", result.getError().getCode(), "错误码应该为401");
        assertEquals("未登录或登录已过期", result.getMessage(), "错误信息应该匹配");

        // 验证没有调用Service
        verify(mockProductService, never()).createProduct(any(), any());

        logger.info("测试创建商品接口 - 未登录 - 通过");
    }

    /**
     * 测试创建商品接口 - 空请求体
     */
    @Test
    @Order(9)
    @DisplayName("4.3 创建商品测试 - 空请求体")
    public void testCreateProduct_EmptyBody() throws Exception {
        logger.info("开始测试创建商品接口 - 空请求体");

        // 模拟已登录用户和空请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        // 执行测试
        Result<Product> result = productController.createProduct(mockRequest);

        // 验证结果
        assertNotNull(result, "创建结果不应为null");
        assertFalse(result.isSuccess(), "创建应该失败");
        assertEquals("400", result.getError().getCode(), "错误码应该为400");
        assertEquals("请求体不能为空", result.getMessage(), "错误信息应该匹配");

        // 验证没有调用Service
        verify(mockProductService, never()).createProduct(any(), any());

        logger.info("测试创建商品接口 - 空请求体 - 通过");
    }

    /**
     * 测试创建商品接口 - 无效JSON
     */
    @Test
    @Order(10)
    @DisplayName("4.4 创建商品测试 - 无效JSON")
    public void testCreateProduct_InvalidJson() throws Exception {
        logger.info("开始测试创建商品接口 - 无效JSON");

        // 模拟已登录用户和无效JSON
        mockUserSession(mockRequest, TEST_USER_ID_1);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader("invalid json")));

        // 执行测试
        Result<Product> result = productController.createProduct(mockRequest);

        // 验证结果
        assertNotNull(result, "创建结果不应为null");
        assertFalse(result.isSuccess(), "创建应该失败");
        assertEquals("400", result.getError().getCode(), "错误码应该为400");
        assertEquals("无效的JSON格式", result.getMessage(), "错误信息应该匹配");

        // 验证没有调用Service
        verify(mockProductService, never()).createProduct(any(), any());

        logger.info("测试创建商品接口 - 无效JSON - 通过");
    }

    /**
     * 测试创建商品接口 - 标题为空
     */
    @Test
    @Order(11)
    @DisplayName("4.5 创建商品测试 - 标题为空")
    public void testCreateProduct_EmptyTitle() throws Exception {
        logger.info("开始测试创建商品接口 - 标题为空");

        // 准备测试数据 - 标题为空
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("");
        dto.setDescription("这是一个测试商品");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setCategoryId(1);

        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 执行测试
        Result<Product> result = productController.createProduct(mockRequest);

        // 验证结果
        assertNotNull(result, "创建结果不应为null");
        assertFalse(result.isSuccess(), "创建应该失败");
        assertEquals("400", result.getError().getCode(), "错误码应该为400");
        assertEquals("商品标题不能为空", result.getMessage(), "错误信息应该匹配");

        // 验证没有调用Service
        verify(mockProductService, never()).createProduct(any(), any());

        logger.info("测试创建商品接口 - 标题为空 - 通过");
    }

    /**
     * 测试更新商品接口 - 成功场景
     */
    @Test
    @Order(12)
    @DisplayName("5.1 更新商品测试 - 成功场景")
    public void testUpdateProduct_Success() throws Exception {
        logger.info("开始测试更新商品接口 - 成功场景");

        // 准备测试数据
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("更新后的商品");
        dto.setDescription("更新后的描述");
        dto.setPrice(new BigDecimal("199.99"));
        dto.setCategoryId(2);

        Product existingProduct = createMockProduct(TEST_PRODUCT_ID_1, "原商品");

        // 设置Mock行为
        when(mockProductService.getProductById(TEST_PRODUCT_ID_1)).thenReturn(existingProduct);
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);
        when(mockProductService.updateProduct(any(Product.class), eq(TEST_USER_ID_1))).thenReturn(true);

        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 执行测试
        Result<Product> result = productController.updateProduct(TEST_PRODUCT_ID_1, mockRequest);

        // 验证结果
        assertNotNull(result, "更新结果不应为null");
        assertTrue(result.isSuccess(), "更新应该成功");
        assertNotNull(result.getData(), "商品数据不应为null");
        assertEquals("更新后的商品", result.getData().getTitle(), "商品标题应该已更新");

        // 验证Mock调用
        verify(mockProductService).getProductById(TEST_PRODUCT_ID_1);
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService).updateProduct(any(Product.class), eq(TEST_USER_ID_1));

        logger.info("测试更新商品接口 - 成功场景 - 通过");
    }

    /**
     * 测试更新商品接口 - 商品不存在
     */
    @Test
    @Order(13)
    @DisplayName("5.2 更新商品测试 - 商品不存在")
    public void testUpdateProduct_NotFound() throws Exception {
        logger.info("开始测试更新商品接口 - 商品不存在");

        // 准备测试数据
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("更新后的商品");
        dto.setDescription("更新后的描述");
        dto.setPrice(new BigDecimal("199.99"));
        dto.setCategoryId(2);

        // 设置Mock行为 - 商品不存在
        when(mockProductService.getProductById(999L)).thenReturn(null);

        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 执行测试
        Result<Product> result = productController.updateProduct(999L, mockRequest);

        // 验证结果
        assertNotNull(result, "更新结果不应为null");
        assertFalse(result.isSuccess(), "更新应该失败");
        assertEquals("404", result.getError().getCode(), "错误码应该为404");
        assertEquals("商品不存在", result.getMessage(), "错误信息应该匹配");

        // 验证Mock调用
        verify(mockProductService).getProductById(999L);
        verify(mockProductService, never()).updateProduct(any(), any());

        logger.info("测试更新商品接口 - 商品不存在 - 通过");
    }

    /**
     * 测试更新商品接口 - 无权限
     */
    @Test
    @Order(14)
    @DisplayName("5.3 更新商品测试 - 无权限")
    public void testUpdateProduct_NoPermission() throws Exception {
        logger.info("开始测试更新商品接口 - 无权限");

        // 准备测试数据
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("更新后的商品");
        dto.setDescription("更新后的描述");
        dto.setPrice(new BigDecimal("199.99"));
        dto.setCategoryId(2);

        Product existingProduct = createMockProduct(TEST_PRODUCT_ID_1, "原商品");

        // 设置Mock行为 - 商品存在但无权限
        when(mockProductService.getProductById(TEST_PRODUCT_ID_1)).thenReturn(existingProduct);
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(false);

        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 执行测试
        Result<Product> result = productController.updateProduct(TEST_PRODUCT_ID_1, mockRequest);

        // 验证结果
        assertNotNull(result, "更新结果不应为null");
        assertFalse(result.isSuccess(), "更新应该失败");
        assertEquals("403", result.getError().getCode(), "错误码应该为403");
        assertEquals("无权限修改此商品", result.getMessage(), "错误信息应该匹配");

        // 验证Mock调用
        verify(mockProductService).getProductById(TEST_PRODUCT_ID_1);
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService, never()).updateProduct(any(), any());

        logger.info("测试更新商品接口 - 无权限 - 通过");
    }

    /**
     * 测试系统异常处理
     */
    @Test
    @Order(15)
    @DisplayName("6.1 系统异常处理测试")
    public void testSystemException() {
        logger.info("开始测试系统异常处理");

        // 设置Mock行为 - 抛出异常
        when(mockProductService.findProducts(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试
        Result<Map<String, Object>> result = productController.getProducts(
            1, 20, null, null, null, null, "createTime", "desc"
        );

        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "查询应该失败");
        assertEquals("500", result.getError().getCode(), "错误码应该为500");
        assertEquals("系统错误，请稍后再试", result.getMessage(), "错误信息应该匹配");

        logger.info("测试系统异常处理 - 通过");
    }

    /**
     * 测试完整的商品操作流程
     */
    @Test
    @Order(16)
    @DisplayName("7.1 完整商品操作流程测试")
    public void testCompleteProductWorkflow() throws Exception {
        logger.info("开始测试完整的商品操作流程");

        // 1. 获取商品列表
        Map<String, Object> mockListResult = new HashMap<>();
        mockListResult.put("products", Arrays.asList(createMockProductCardVO(TEST_PRODUCT_ID_1, "测试商品")));
        mockListResult.put("total", 1L);

        when(mockProductService.findProducts(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(mockListResult);

        Result<Map<String, Object>> listResult = productController.getProducts(1, 20, null, null, null, null, "createTime", "desc");
        assertTrue(listResult.isSuccess(), "获取商品列表应该成功");
        logger.info("获取商品列表: success=" + listResult.isSuccess());

        // 2. 创建商品
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("测试商品");
        dto.setDescription("这是一个测试商品");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setCategoryId(1);
        dto.setAction(ProductCreateDTO.ACTION_SUBMIT_REVIEW);

        Product mockProduct = createMockProduct(TEST_PRODUCT_ID_1, "测试商品");

        when(mockProductService.createProduct(any(ProductCreateDTO.class), eq(TEST_USER_ID_1)))
            .thenReturn(TEST_PRODUCT_ID_1);
        when(mockProductService.getProductById(TEST_PRODUCT_ID_1)).thenReturn(mockProduct);

        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        Result<Product> createResult = productController.createProduct(mockRequest);
        assertTrue(createResult.isSuccess(), "创建商品应该成功");
        logger.info("创建商品: success=" + createResult.isSuccess());

        // 3. 获取商品详情
        ProductDetailVO mockDetail = createMockProductDetailVO(TEST_PRODUCT_ID_1, "测试商品");
        when(mockProductService.getProductDetailById(TEST_PRODUCT_ID_1, TEST_USER_ID_1))
            .thenReturn(mockDetail);

        Result<ProductDetailVO> detailResult = productController.getProductDetail(TEST_PRODUCT_ID_1, mockRequest);
        assertTrue(detailResult.isSuccess(), "获取商品详情应该成功");
        logger.info("获取商品详情: success=" + detailResult.isSuccess());

        // 4. 更新商品
        dto.setTitle("更新后的商品");
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);
        when(mockProductService.updateProduct(any(Product.class), eq(TEST_USER_ID_1))).thenReturn(true);

        String updateRequestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(updateRequestBody)));

        Result<Product> updateResult = productController.updateProduct(TEST_PRODUCT_ID_1, mockRequest);
        assertTrue(updateResult.isSuccess(), "更新商品应该成功");
        logger.info("更新商品: success=" + updateResult.isSuccess());

        // 5. 获取我的商品列表
        List<ProductCardVO> mockMyProducts = Arrays.asList(createMockProductCardVO(TEST_PRODUCT_ID_1, "更新后的商品"));
        when(mockProductService.getProductsBySellerIdAndStatus(TEST_USER_ID_1, null))
            .thenReturn(mockMyProducts);

        Result<List<ProductCardVO>> myProductsResult = productController.getMyProducts(null, mockRequest);
        assertTrue(myProductsResult.isSuccess(), "获取我的商品列表应该成功");
        logger.info("获取我的商品列表: success=" + myProductsResult.isSuccess());

        logger.info("完整的商品操作流程测试通过");
    }

    // ==================== 工具方法 ====================

    /**
     * 模拟用户会话
     */
    private void mockUserSession(HttpServletRequest request, Long userId) {
        javax.servlet.http.HttpSession session = mock(javax.servlet.http.HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(userId);
        // RequestUtil.getCurrentUserId从request.getAttribute获取userId
        when(request.getAttribute("userId")).thenReturn(userId);
    }

    /**
     * 创建Mock商品卡片VO
     */
    private ProductCardVO createMockProductCardVO(Long id, String title) {
        ProductCardVO vo = new ProductCardVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setPrice(new BigDecimal("99.99"));
        vo.setSellerId(TEST_USER_ID_1);
        vo.setMainImageUrl("/images/test.jpg");
        vo.setCreateTime(java.time.LocalDateTime.now());
        vo.setStatus(1);
        return vo;
    }
    
    /**
     * 创建Mock商品详情VO
     */
    private ProductDetailVO createMockProductDetailVO(Long id, String title) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setDescription("测试商品描述");
        vo.setPrice(new BigDecimal("99.99"));
        vo.setSellerId(TEST_USER_ID_1);
        vo.setSellerName("测试卖家");
        vo.setCategoryId(1);
        vo.setCategoryName("测试分类");
        vo.setStatus(1);
        vo.setCreateTime(java.time.LocalDateTime.now());
        return vo;
    }
    
    /**
     * 创建Mock商品对象
     */
    private Product createMockProduct(Long id, String title) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setDescription("测试商品描述");
        product.setPrice(new BigDecimal("99.99"));
        product.setSellerId(TEST_USER_ID_1);
        product.setCategoryId(1);
        product.setStatus(1);
        product.setCreateTime(java.time.LocalDateTime.now());
        return product;
    }

    // ==================== 遗漏功能测试 ====================

    /**
     * 测试保存草稿接口 - 成功场景
     */
    @Test
    @Order(8)
    @DisplayName("5.1 保存草稿测试 - 成功场景")
    public void testSaveDraft_Success() throws Exception {
        logger.info("开始测试保存草稿接口 - 成功场景");

        // 准备测试数据
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("测试草稿商品");
        dto.setDescription("这是一个测试草稿");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setCategoryId(1);

        Product mockProduct = createMockProduct(TEST_PRODUCT_ID_1, "测试草稿商品");
        mockProduct.setStatus(Product.STATUS_DRAFT);

        // 设置Mock行为
        when(mockProductService.createProduct(any(ProductCreateDTO.class), eq(TEST_USER_ID_1)))
            .thenReturn(TEST_PRODUCT_ID_1);
        when(mockProductService.getProductById(TEST_PRODUCT_ID_1)).thenReturn(mockProduct);

        // 模拟已登录用户和请求体
        mockUserSession(mockRequest, TEST_USER_ID_1);
        String requestBody = JsonUtil.toJson(dto);
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 执行测试
        Result<Product> result = productController.saveDraft(mockRequest);

        // 验证结果
        assertNotNull(result, "保存草稿结果不应为null");
        assertTrue(result.isSuccess(), "保存草稿应该成功");
        assertNotNull(result.getData(), "草稿数据不应为null");
        assertEquals(TEST_PRODUCT_ID_1, result.getData().getId(), "商品ID应该匹配");
        assertEquals(Product.STATUS_DRAFT, result.getData().getStatus(), "状态应该是草稿");

        // 验证Mock调用
        verify(mockProductService).createProduct(any(ProductCreateDTO.class), eq(TEST_USER_ID_1));
        verify(mockProductService).getProductById(TEST_PRODUCT_ID_1);

        logger.info("保存草稿测试通过 - 成功场景");
    }

    /**
     * 测试上传商品图片接口 - 成功场景
     */
    @Test
    @Order(9)
    @DisplayName("5.2 上传商品图片测试 - 成功场景")
    public void testUploadProductImage_Success() throws Exception {
        logger.info("开始测试上传商品图片接口 - 成功场景");

        // 模拟已登录用户
        mockUserSession(mockRequest, TEST_USER_ID_1);

        // 模拟请求参数
        when(mockRequest.getParameter("productId")).thenReturn(TEST_PRODUCT_ID_1.toString());
        when(mockRequest.getParameter("imageName")).thenReturn("test-image.jpg");
        when(mockRequest.getParameter("contentType")).thenReturn("image/jpeg");
        when(mockRequest.getParameter("isMain")).thenReturn("true");

        // 设置Mock行为
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);
        String mockImageUrl = "http://example.com/images/test-image.jpg";
        when(mockProductService.uploadProductImage(eq(TEST_PRODUCT_ID_1), eq("test-image.jpg"),
                isNull(), eq("image/jpeg"), eq(true), eq(TEST_USER_ID_1))).thenReturn(mockImageUrl);

        // 执行测试
        Result<Map<String, Object>> result = productController.uploadProductImage(mockRequest);

        // 验证结果
        assertNotNull(result, "上传图片结果不应为null");
        assertTrue(result.isSuccess(), "上传图片应该成功");
        assertNotNull(result.getData(), "图片数据不应为null");
        assertEquals(mockImageUrl, result.getData().get("imageUrl"), "图片URL应该匹配");
        assertEquals(TEST_PRODUCT_ID_1, result.getData().get("productId"), "商品ID应该匹配");
        assertEquals(true, result.getData().get("isMain"), "主图标识应该匹配");

        // 验证Mock调用
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService).uploadProductImage(eq(TEST_PRODUCT_ID_1), eq("test-image.jpg"),
                isNull(), eq("image/jpeg"), eq(true), eq(TEST_USER_ID_1));

        logger.info("上传商品图片测试通过 - 成功场景");
    }

    /**
     * 测试更新商品状态接口 - 成功场景
     */
    @Test
    @Order(10)
    @DisplayName("5.3 更新商品状态测试 - 成功场景")
    public void testUpdateProductStatus_Success() throws Exception {
        logger.info("开始测试更新商品状态接口 - 成功场景");

        // 模拟已登录用户
        mockUserSession(mockRequest, TEST_USER_ID_1);

        // 模拟请求体
        String requestBody = "{\"status\":1}";
        when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // 设置Mock行为
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);
        when(mockProductService.updateProductStatus(TEST_PRODUCT_ID_1, 1, TEST_USER_ID_1)).thenReturn(true);

        // 执行测试
        Result<String> result = productController.updateProductStatus(TEST_PRODUCT_ID_1, mockRequest);

        // 验证结果
        assertNotNull(result, "更新状态结果不应为null");
        assertTrue(result.isSuccess(), "更新状态应该成功");
        assertNotNull(result.getData(), "状态数据不应为null");
        assertTrue(result.getData().contains("在售"), "应该包含状态描述");

        // 验证Mock调用
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService).updateProductStatus(TEST_PRODUCT_ID_1, 1, TEST_USER_ID_1);

        logger.info("更新商品状态测试通过 - 成功场景");
    }

    /**
     * 测试删除商品接口 - 成功场景
     */
    @Test
    @Order(11)
    @DisplayName("5.4 删除商品测试 - 成功场景")
    public void testDeleteProduct_Success() throws Exception {
        logger.info("开始测试删除商品接口 - 成功场景");

        // 模拟已登录用户
        mockUserSession(mockRequest, TEST_USER_ID_1);

        // 设置Mock行为
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);
        when(mockProductService.deleteProduct(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(true);

        // 执行测试
        Result<String> result = productController.deleteProduct(TEST_PRODUCT_ID_1, mockRequest);

        // 验证结果
        assertNotNull(result, "删除商品结果不应为null");
        assertTrue(result.isSuccess(), "删除商品应该成功");
        assertNotNull(result.getData(), "删除数据不应为null");
        assertEquals("商品删除成功", result.getData(), "返回消息应该正确");

        // 验证Mock调用
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService).deleteProduct(TEST_PRODUCT_ID_1, TEST_USER_ID_1);

        logger.info("删除商品测试通过 - 成功场景");
    }

    /**
     * 测试权限验证 - 失败场景
     */
    @Test
    @Order(12)
    @DisplayName("5.5 权限验证测试 - 失败场景")
    public void testPermissionValidation_Failure() throws Exception {
        logger.info("开始测试权限验证 - 失败场景");

        // 模拟已登录用户
        mockUserSession(mockRequest, TEST_USER_ID_1);

        // 设置Mock行为 - 权限检查失败
        when(mockProductService.isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1)).thenReturn(false);

        // 测试删除商品权限验证
        Result<String> deleteResult = productController.deleteProduct(TEST_PRODUCT_ID_1, mockRequest);
        assertNotNull(deleteResult, "删除结果不应为null");
        assertFalse(deleteResult.isSuccess(), "删除应该失败");
        assertEquals("403", deleteResult.getError().getCode(), "错误码应该是403");
        assertTrue(deleteResult.getError().getMessage().contains("无权限"), "错误信息应该包含权限提示");

        // 验证Mock调用
        verify(mockProductService).isProductOwnedBySeller(TEST_PRODUCT_ID_1, TEST_USER_ID_1);
        verify(mockProductService, never()).deleteProduct(any(), any());

        logger.info("权限验证测试通过 - 失败场景");
    }
}
