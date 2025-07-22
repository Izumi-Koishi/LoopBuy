package com.shiwu.user.controller;

import com.shiwu.common.result.Result;
import com.shiwu.framework.core.ApplicationContext;
import com.shiwu.test.TestBase;
import com.shiwu.user.model.*;
import com.shiwu.user.service.UserService;
import com.shiwu.user.vo.FeedResponseVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserController MVC框架测试
 * 
 * 测试重构后的UserController是否正确使用MVC框架的注解驱动方式
 * 验证依赖注入、路由映射、参数绑定等功能
 */
public class UserControllerMvcTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerMvcTest.class);
    
    // 测试常量
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_TARGET_USER_ID = 2L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    
    // 测试对象
    private UserController userController;
    
    // Mock对象
    @Mock
    private UserService mockUserService;
    
    @Mock
    private HttpServletRequest request;
    
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        logger.info("UserController MVC测试环境初始化开始");
        
        // 初始化Mockito
        closeable = MockitoAnnotations.openMocks(this);
        
        // 创建UserController实例
        userController = new UserController();
        
        // 使用反射注入Mock的UserService
        try {
            java.lang.reflect.Field userServiceField = UserController.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(userController, mockUserService);
        } catch (Exception e) {
            throw new RuntimeException("注入Mock UserService失败", e);
        }
        
        logger.info("UserController MVC测试环境初始化完成");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
        logger.info("UserController MVC测试清理完成");
    }

    /**
     * 测试用户登录接口 - 成功场景
     */
    @Test
    public void testLogin_Success() throws Exception {
        logger.info("开始测试用户登录接口 - 成功场景");
        
        // 准备测试数据
        UserVO userVO = new UserVO();
        userVO.setId(TEST_USER_ID);
        userVO.setUsername(TEST_USERNAME);
        
        LoginResult loginResult = LoginResult.success(userVO);
        
        // 设置Mock行为
        when(mockUserService.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(loginResult);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\"}")));
        
        // 执行测试
        Result<Object> result = userController.login(request);
        
        // 验证结果
        assertNotNull(result, "登录结果不应为null");
        assertTrue(result.isSuccess(), "登录应该成功");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data.get("user"), "返回数据应包含用户信息");
        assertNotNull(data.get("token"), "返回数据应包含JWT Token");
        
        // 验证Mock调用
        verify(mockUserService).login(TEST_USERNAME, TEST_PASSWORD);
        
        logger.info("测试用户登录接口 - 成功场景 - 通过");
    }

    /**
     * 测试用户登录接口 - 失败场景
     */
    @Test
    public void testLogin_Failure() throws Exception {
        logger.info("开始测试用户登录接口 - 失败场景");
        
        // 准备测试数据
        LoginResult loginResult = LoginResult.fail(LoginErrorEnum.WRONG_PASSWORD);
        
        // 设置Mock行为
        when(mockUserService.login(TEST_USERNAME, "wrongpassword")).thenReturn(loginResult);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"wrongpassword\"}")));
        
        // 执行测试
        Result<Object> result = userController.login(request);
        
        // 验证结果
        assertNotNull(result, "登录结果不应为null");
        assertFalse(result.isSuccess(), "登录应该失败");
        assertTrue(result.getMessage().contains("密码错误"), "错误消息应该包含密码错误信息");
        
        // 验证Mock调用
        verify(mockUserService).login(TEST_USERNAME, "wrongpassword");
        
        logger.info("测试用户登录接口 - 失败场景 - 通过");
    }

    /**
     * 测试用户登录接口 - 空请求体
     */
    @Test
    public void testLogin_EmptyBody() throws Exception {
        logger.info("开始测试用户登录接口 - 空请求体场景");
        
        // 设置Mock行为
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        
        // 执行测试
        Result<Object> result = userController.login(request);
        
        // 验证结果
        assertNotNull(result, "登录结果不应为null");
        assertFalse(result.isSuccess(), "空请求体应该失败");
        assertTrue(result.getMessage().contains("请求体不能为空"), "错误消息应该包含请求体为空的提示");
        
        logger.info("测试用户登录接口 - 空请求体场景 - 通过");
    }

    /**
     * 测试用户注册接口 - 成功场景
     */
    @Test
    public void testRegister_Success() throws Exception {
        logger.info("开始测试用户注册接口 - 成功场景");
        
        // 准备测试数据
        UserVO userVO = new UserVO();
        userVO.setId(TEST_USER_ID);
        userVO.setUsername(TEST_USERNAME);
        
        RegisterResult registerResult = RegisterResult.success(userVO);
        
        // 设置Mock行为
        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(registerResult);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\",\"email\":\"test@example.com\"}")));
        
        // 执行测试
        Result<Object> result = userController.register(request);
        
        // 验证结果
        assertNotNull(result, "注册结果不应为null");
        assertTrue(result.isSuccess(), "注册应该成功");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data.get("user"), "返回数据应包含用户信息");
        
        // 验证Mock调用
        verify(mockUserService).register(any(RegisterRequest.class));
        
        logger.info("测试用户注册接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取用户个人信息接口 - 成功场景
     */
    @Test
    public void testGetUserProfile_Success() {
        logger.info("开始测试获取用户个人信息接口 - 成功场景");
        
        // 准备测试数据
        UserVO targetUser = new UserVO();
        targetUser.setId(TEST_TARGET_USER_ID);
        targetUser.setUsername("targetuser");

        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUser(targetUser);
        
        // 设置Mock行为
        when(mockUserService.getUserProfile(TEST_TARGET_USER_ID, TEST_USER_ID)).thenReturn(userProfile);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());
        
        // 执行测试
        Result<Object> result = userController.getUserProfile(TEST_TARGET_USER_ID, request);
        
        // 验证结果
        assertNotNull(result, "获取用户信息结果不应为null");
        assertTrue(result.isSuccess(), "获取用户信息应该成功");
        assertNotNull(result.getData(), "返回数据不应为null");
        
        // 验证Mock调用
        verify(mockUserService).getUserProfile(TEST_TARGET_USER_ID, TEST_USER_ID);
        
        logger.info("测试获取用户个人信息接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取用户个人信息接口 - 用户不存在
     */
    @Test
    public void testGetUserProfile_UserNotFound() {
        logger.info("开始测试获取用户个人信息接口 - 用户不存在场景");
        
        // 设置Mock行为
        when(mockUserService.getUserProfile(999L, TEST_USER_ID)).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());
        
        // 执行测试
        Result<Object> result = userController.getUserProfile(999L, request);
        
        // 验证结果
        assertNotNull(result, "获取用户信息结果不应为null");
        assertFalse(result.isSuccess(), "用户不存在应该失败");
        assertTrue(result.getMessage().contains("用户不存在"), "错误消息应该包含用户不存在的提示");
        
        // 验证Mock调用
        verify(mockUserService).getUserProfile(999L, TEST_USER_ID);
        
        logger.info("测试获取用户个人信息接口 - 用户不存在场景 - 通过");
    }

    /**
     * 测试获取关注状态接口 - 成功场景
     */
    @Test
    public void testGetFollowStatus_Success() {
        logger.info("开始测试获取关注状态接口 - 成功场景");
        
        // 准备测试数据
        FollowStatusVO followStatus = new FollowStatusVO();
        followStatus.setIsFollowing(true);
        followStatus.setFollowerCount(100);
        
        // 设置Mock行为
        when(mockUserService.getFollowStatus(TEST_USER_ID, TEST_TARGET_USER_ID)).thenReturn(followStatus);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());
        
        // 执行测试
        Result<Object> result = userController.getFollowStatus(TEST_TARGET_USER_ID, request);
        
        // 验证结果
        assertNotNull(result, "获取关注状态结果不应为null");
        assertTrue(result.isSuccess(), "获取关注状态应该成功");
        assertNotNull(result.getData(), "返回数据不应为null");
        
        // 验证Mock调用
        verify(mockUserService).getFollowStatus(TEST_USER_ID, TEST_TARGET_USER_ID);
        
        logger.info("测试获取关注状态接口 - 成功场景 - 通过");
    }

    /**
     * 测试获取关注状态接口 - 未授权
     */
    @Test
    public void testGetFollowStatus_Unauthorized() {
        logger.info("开始测试获取关注状态接口 - 未授权场景");
        
        // 设置Mock行为 - 不设置用户ID头
        
        // 执行测试
        Result<Object> result = userController.getFollowStatus(TEST_TARGET_USER_ID, request);
        
        // 验证结果
        assertNotNull(result, "获取关注状态结果不应为null");
        assertFalse(result.isSuccess(), "未授权应该失败");
        assertTrue(result.getMessage().contains("未授权访问"), "错误消息应该包含未授权访问的提示");
        
        logger.info("测试获取关注状态接口 - 未授权场景 - 通过");
    }

    /**
     * 测试获取关注动态接口 - 成功场景
     */
    @Test
    public void testGetFollowingFeed_Success() {
        logger.info("开始测试获取关注动态接口 - 成功场景");
        
        // 准备测试数据
        FeedResponseVO feedResponse = new FeedResponseVO();
        Result<FeedResponseVO> feedResult = Result.success(feedResponse);
        
        // 设置Mock行为
        when(mockUserService.getFollowingFeed(TEST_USER_ID, 1, 20, "ALL")).thenReturn(feedResult);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());
        
        // 执行测试
        Result<FeedResponseVO> result = userController.getFollowingFeed(1, 20, request);
        
        // 验证结果
        assertNotNull(result, "获取关注动态结果不应为null");
        assertTrue(result.isSuccess(), "获取关注动态应该成功");
        
        // 验证Mock调用
        verify(mockUserService).getFollowingFeed(TEST_USER_ID, 1, 20, "ALL");
        
        logger.info("测试获取关注动态接口 - 成功场景 - 通过");
    }

    /**
     * 测试关注用户接口 - 成功场景
     */
    @Test
    public void testFollowUser_Success() {
        logger.info("开始测试关注用户接口 - 成功场景");

        // 准备测试数据
        FollowResult followResult = FollowResult.success(true, 101);

        // 设置Mock行为
        when(mockUserService.followUser(TEST_USER_ID, TEST_TARGET_USER_ID)).thenReturn(followResult);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());

        // 执行测试
        Result<Object> result = userController.followUser(TEST_TARGET_USER_ID, request);

        // 验证结果
        assertNotNull(result, "关注用户结果不应为null");
        assertTrue(result.isSuccess(), "关注用户应该成功");
        assertEquals("关注成功", result.getData(), "返回消息应该正确");

        // 验证Mock调用
        verify(mockUserService).followUser(TEST_USER_ID, TEST_TARGET_USER_ID);

        logger.info("测试关注用户接口 - 成功场景 - 通过");
    }

    /**
     * 测试关注用户接口 - 未授权
     */
    @Test
    public void testFollowUser_Unauthorized() {
        logger.info("开始测试关注用户接口 - 未授权场景");

        // 设置Mock行为 - 不设置用户ID头

        // 执行测试
        Result<Object> result = userController.followUser(TEST_TARGET_USER_ID, request);

        // 验证结果
        assertNotNull(result, "关注用户结果不应为null");
        assertFalse(result.isSuccess(), "未授权应该失败");
        assertEquals("401", result.getError().getCode(), "错误码应该为401");

        logger.info("测试关注用户接口 - 未授权场景 - 通过");
    }

    /**
     * 测试取消关注用户接口 - 成功场景
     */
    @Test
    public void testUnfollowUser_Success() {
        logger.info("开始测试取消关注用户接口 - 成功场景");

        // 准备测试数据
        FollowResult unfollowResult = FollowResult.success(false, 99);

        // 设置Mock行为
        when(mockUserService.unfollowUser(TEST_USER_ID, TEST_TARGET_USER_ID)).thenReturn(unfollowResult);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());

        // 执行测试
        Result<Object> result = userController.unfollowUser(TEST_TARGET_USER_ID, request);

        // 验证结果
        assertNotNull(result, "取消关注用户结果不应为null");
        assertTrue(result.isSuccess(), "取消关注用户应该成功");
        assertEquals("取消关注成功", result.getData(), "返回消息应该正确");

        // 验证Mock调用
        verify(mockUserService).unfollowUser(TEST_USER_ID, TEST_TARGET_USER_ID);

        logger.info("测试取消关注用户接口 - 成功场景 - 通过");
    }

    /**
     * 测试取消关注用户接口 - 未授权
     */
    @Test
    public void testUnfollowUser_Unauthorized() {
        logger.info("开始测试取消关注用户接口 - 未授权场景");

        // 设置Mock行为 - 不设置用户ID头

        // 执行测试
        Result<Object> result = userController.unfollowUser(TEST_TARGET_USER_ID, request);

        // 验证结果
        assertNotNull(result, "取消关注用户结果不应为null");
        assertFalse(result.isSuccess(), "未授权应该失败");
        assertEquals("401", result.getError().getCode(), "错误码应该为401");

        logger.info("测试取消关注用户接口 - 未授权场景 - 通过");
    }

    /**
     * 测试JSON解析异常处理
     */
    @Test
    public void testLogin_InvalidJson() throws Exception {
        logger.info("开始测试用户登录接口 - 无效JSON场景");

        // 设置Mock行为
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("invalid json")));

        // 执行测试
        Result<Object> result = userController.login(request);

        // 验证结果
        assertNotNull(result, "登录结果不应为null");
        assertFalse(result.isSuccess(), "无效JSON应该失败");
        assertTrue(result.getMessage().contains("请求格式错误") || result.getMessage().contains("服务器内部错误"), "错误消息应该包含格式错误或服务器错误信息");

        logger.info("测试用户登录接口 - 无效JSON场景 - 通过");
    }

    /**
     * 测试系统异常处理
     */
    @Test
    public void testSystemException() throws Exception {
        logger.info("开始测试系统异常处理");

        // 设置Mock行为 - 抛出异常
        when(mockUserService.login(anyString(), anyString())).thenThrow(new RuntimeException("数据库连接失败"));
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\"}")));

        // 执行测试
        Result<Object> result = userController.login(request);

        // 验证结果
        assertNotNull(result, "登录结果不应为null");
        assertFalse(result.isSuccess(), "系统异常应该失败");
        assertTrue(result.getMessage().contains("服务器内部错误"), "错误消息应该包含服务器内部错误信息");
        assertTrue(result.getMessage().contains("服务器内部错误"), "错误消息应该包含系统错误信息");

        logger.info("系统异常处理测试通过");
    }

    /**
     * 测试完整的用户操作流程
     */
    @Test
    public void testCompleteUserWorkflow() throws Exception {
        logger.info("开始测试完整的用户操作流程");

        // 1. 用户注册
        UserVO userVO = new UserVO();
        userVO.setId(TEST_USER_ID);
        userVO.setUsername(TEST_USERNAME);

        RegisterResult registerResult = RegisterResult.success(userVO);
        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(registerResult);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\",\"email\":\"test@example.com\"}")));

        Result<Object> regResult = userController.register(request);
        assertTrue(regResult.isSuccess(), "注册应该成功");
        logger.info("注册: success={}", regResult.isSuccess());

        // 2. 用户登录
        LoginResult loginResult = LoginResult.success(userVO);
        when(mockUserService.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(loginResult);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(
            "{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\"}")));

        Result<Object> loginRes = userController.login(request);
        assertTrue(loginRes.isSuccess(), "登录应该成功");
        logger.info("登录: success={}", loginRes.isSuccess());

        // 3. 获取用户信息
        UserVO targetUser = new UserVO();
        targetUser.setId(TEST_TARGET_USER_ID);
        targetUser.setUsername("targetuser");

        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUser(targetUser);

        when(mockUserService.getUserProfile(TEST_TARGET_USER_ID, TEST_USER_ID)).thenReturn(userProfile);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID.toString());

        Result<Object> profileResult = userController.getUserProfile(TEST_TARGET_USER_ID, request);
        assertTrue(profileResult.isSuccess(), "获取用户信息应该成功");
        logger.info("获取用户信息: success={}", profileResult.isSuccess());

        // 4. 关注用户
        FollowResult followResult = FollowResult.success(true, 101);
        when(mockUserService.followUser(TEST_USER_ID, TEST_TARGET_USER_ID)).thenReturn(followResult);

        Result<Object> followRes = userController.followUser(TEST_TARGET_USER_ID, request);
        assertTrue(followRes.isSuccess(), "关注用户应该成功");
        logger.info("关注用户: success={}", followRes.isSuccess());

        // 5. 获取关注动态
        FeedResponseVO feedResponse = new FeedResponseVO();
        Result<FeedResponseVO> feedResult = Result.success(feedResponse);
        when(mockUserService.getFollowingFeed(TEST_USER_ID, 1, 20, "ALL")).thenReturn(feedResult);

        Result<FeedResponseVO> feedRes = userController.getFollowingFeed(1, 20, request);
        assertTrue(feedRes.isSuccess(), "获取关注动态应该成功");
        logger.info("获取关注动态: success={}", feedRes.isSuccess());

        logger.info("完整的用户操作流程测试通过");
    }
}
