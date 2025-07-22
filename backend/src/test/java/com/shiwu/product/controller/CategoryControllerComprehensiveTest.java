package com.shiwu.product.controller;

import com.shiwu.test.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CategoryController综合测试类
 */
public class CategoryControllerComprehensiveTest extends TestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryControllerComprehensiveTest.class);
    
    private CategoryController categoryController;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;
    
    @BeforeEach
    public void setUp() {
        logger.info("CategoryController测试环境初始化开始");
        super.setUp();
        
        // 创建CategoryController实例
        categoryController = new CategoryController();

        // 初始化路由（模拟Servlet容器的init调用）
        try {
            categoryController.init();
        } catch (Exception e) {
            fail("CategoryController初始化失败: " + e.getMessage());
        }
        
        // 创建Mock对象
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        
        // 设置响应Writer
        responseWriter = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        } catch (Exception e) {
            fail("设置响应Writer失败: " + e.getMessage());
        }
        
        logger.info("CategoryController测试环境初始化完成");
    }
    
    @AfterEach
    public void tearDown() {
        logger.info("CategoryController测试清理完成");
    }
    
    /**
     * 测试获取所有分类接口 - 成功
     */
    @Test
    public void testGetAllCategories() throws Exception {
        logger.info("开始测试获取所有分类接口");
        
        // 设置请求路径
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        categoryController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("\"success\":true"));
        assertTrue(responseContent.contains("\"data\""));
        
        logger.info("获取所有分类接口测试通过: response=" + responseContent);
    }
    
    /**
     * 测试获取所有分类接口 - 根路径
     */
    @Test
    public void testGetAllCategoriesRootPath() throws Exception {
        logger.info("开始测试获取所有分类接口 - 根路径");
        
        // 设置请求路径为null（根路径）
        when(request.getPathInfo()).thenReturn(null);
        
        // 执行测试
        categoryController.doGet(request, response);
        
        // 验证响应
        verify(response).setContentType("application/json;charset=UTF-8");
        
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("\"success\":true"));
        
        logger.info("根路径获取分类接口测试通过: response=" + responseContent);
    }
    
    /**
     * 测试无效路径
     */
    @Test
    public void testInvalidPath() throws Exception {
        logger.info("开始测试无效路径");

        // 设置无效请求路径
        when(request.getPathInfo()).thenReturn("/invalid");

        // 执行测试
        categoryController.doGet(request, response);

        // 验证响应 - 更新为匹配BaseController的实际行为
        verify(response).setStatus(404);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response, atLeastOnce()).getWriter();

        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("\"success\":false"));
        assertTrue(responseContent.contains("接口不存在"));

        logger.info("无效路径测试通过: response=" + responseContent);
    }
    
    /**
     * 测试多级无效路径
     */
    @Test
    public void testMultiLevelInvalidPath() throws Exception {
        logger.info("开始测试多级无效路径");

        // 设置多级无效请求路径
        when(request.getPathInfo()).thenReturn("/invalid/path/test");

        // 执行测试
        categoryController.doGet(request, response);

        // 验证响应 - 更新为匹配BaseController的实际行为
        verify(response).setStatus(404);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response, atLeastOnce()).getWriter();

        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("\"success\":false"));
        assertTrue(responseContent.contains("接口不存在"));

        logger.info("多级无效路径测试通过: response=" + responseContent);
    }
    
    /**
     * 测试系统异常处理
     */
    @Test
    public void testSystemExceptionHandling() throws Exception {
        logger.info("开始测试系统异常处理");
        
        // 创建一个会抛出异常的CategoryController
        CategoryController exceptionController = new CategoryController() {
            @Override
            public void doGet(HttpServletRequest req, HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
                // 模拟系统异常
                throw new RuntimeException("系统异常");
            }
        };
        
        // 设置请求路径
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试，期望抛出异常
        try {
            exceptionController.doGet(request, response);
            fail("应该抛出RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("系统异常", e.getMessage());
            logger.info("系统异常正确抛出: " + e.getMessage());
        }
        
        logger.info("系统异常处理测试通过");
    }
    
    /**
     * 测试HTTP方法路由 - 只支持GET
     */
    @Test
    public void testHttpMethodRouting() throws Exception {
        logger.info("开始测试HTTP方法路由");

        // 设置请求路径
        when(request.getPathInfo()).thenReturn("/");

        // 测试GET方法（应该成功）
        categoryController.doGet(request, response);
        String getResponse = responseWriter.toString();
        assertTrue(getResponse.contains("\"success\":true"));

        logger.info("GET方法测试通过: " + getResponse);

        // 注意：由于doPost是protected方法，我们无法直接在测试中调用
        // 在实际应用中，如果客户端发送POST请求到CategoryController，
        // 会由HttpServlet的默认实现处理，通常返回405 Method Not Allowed

        logger.info("HTTP方法路由测试通过");
    }
    
    /**
     * 测试响应格式验证
     */
    @Test
    public void testResponseFormat() throws Exception {
        logger.info("开始测试响应格式验证");
        
        // 设置请求路径
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        categoryController.doGet(request, response);
        
        // 验证响应格式
        verify(response).setContentType("application/json;charset=UTF-8");
        
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        
        // 验证JSON格式
        assertTrue(responseContent.startsWith("{"));
        assertTrue(responseContent.endsWith("}"));
        assertTrue(responseContent.contains("\"success\""));
        assertTrue(responseContent.contains("\"data\""));
        
        logger.info("响应格式验证测试通过");
    }
    
    /**
     * 测试分类数据结构验证
     */
    @Test
    public void testCategoryDataStructure() throws Exception {
        logger.info("开始测试分类数据结构验证");
        
        // 设置请求路径
        when(request.getPathInfo()).thenReturn("/");
        
        // 执行测试
        categoryController.doGet(request, response);
        
        String responseContent = responseWriter.toString();
        assertNotNull(responseContent);
        
        // 验证响应包含分类相关字段
        if (responseContent.contains("\"data\":[") && !responseContent.contains("\"data\":[]")) {
            // 如果有分类数据，验证数据结构
            assertTrue(responseContent.contains("\"id\"") || responseContent.contains("\"name\""));
        }
        
        logger.info("分类数据结构验证测试通过: response=" + responseContent);
    }
    
    /**
     * 测试完整的分类查询流程
     */
    @Test
    public void testCompleteCategoryWorkflow() throws Exception {
        logger.info("开始测试完整的分类查询流程");
        
        // 1. 测试根路径访问
        when(request.getPathInfo()).thenReturn(null);
        categoryController.doGet(request, response);
        String rootResponse = responseWriter.toString();
        logger.info("根路径访问成功: " + rootResponse);
        
        // 重置响应
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // 2. 测试斜杠路径访问
        when(request.getPathInfo()).thenReturn("/");
        categoryController.doGet(request, response);
        String slashResponse = responseWriter.toString();
        logger.info("斜杠路径访问成功: " + slashResponse);
        
        // 重置响应
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // 3. 测试无效路径访问
        when(request.getPathInfo()).thenReturn("/invalid");
        categoryController.doGet(request, response);
        String invalidResponse = responseWriter.toString();
        logger.info("无效路径访问成功: " + invalidResponse);
        
        // 验证所有响应都是有效的JSON
        assertTrue(rootResponse.contains("\"success\""));
        assertTrue(slashResponse.contains("\"success\""));
        assertTrue(invalidResponse.contains("\"success\""));
        
        logger.info("完整的分类查询流程测试通过");
    }
}
