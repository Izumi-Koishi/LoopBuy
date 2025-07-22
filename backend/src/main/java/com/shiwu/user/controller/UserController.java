package com.shiwu.user.controller;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JsonUtil;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.PathVariable;
import com.shiwu.framework.annotation.RequestMapping;
import com.shiwu.framework.annotation.RequestParam;
import com.shiwu.framework.web.BaseController;
import com.shiwu.user.model.*;
import com.shiwu.user.service.UserService;
import com.shiwu.user.vo.FeedResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器 - MVC框架版本
 *
 * 使用MVC框架的注解驱动方式，大幅简化代码
 * 支持用户登录、注册、个人信息管理、关注功能等
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Controller
public class UserController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    public UserController() {
        logger.info("UserController初始化完成 - 使用MVC框架依赖注入");
    }

    /**
     * 用户登录接口
     */
    @RequestMapping(value = "/api/user/login", method = "POST")
    public Result<Object> login(HttpServletRequest request) {
        logger.debug("处理用户登录请求");

        try {
            // 解析请求体
            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            LoginRequest loginRequest = JsonUtil.fromJson(requestBody, LoginRequest.class);
            if (loginRequest == null) {
                return Result.fail("400", "请求格式错误");
            }

            // 调用服务层
            LoginResult loginResult = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

            // 转换为统一的Result格式
            if (loginResult.getSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("user", loginResult.getUserVO());
                // 生成JWT Token
                String token = JwtUtil.generateToken(loginResult.getUserVO().getId(), loginResult.getUserVO().getUsername());
                data.put("token", token);
                return Result.success(data);
            } else {
                return Result.fail(loginResult.getError().getCode(), loginResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("用户登录时发生异常", e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 用户注册接口
     */
    @RequestMapping(value = "/api/user/register", method = "POST")
    public Result<Object> register(HttpServletRequest request) {
        logger.debug("处理用户注册请求");

        try {
            // 解析请求体
            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return Result.fail("400", "请求体不能为空");
            }

            RegisterRequest registerRequest = JsonUtil.fromJson(requestBody, RegisterRequest.class);
            if (registerRequest == null) {
                return Result.fail("400", "请求格式错误");
            }

            // 调用服务层
            RegisterResult registerResult = userService.register(registerRequest);

            // 转换为统一的Result格式
            if (registerResult.getSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("user", registerResult.getUserVO());
                return Result.success(data);
            } else {
                return Result.fail(registerResult.getError().getCode(), registerResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("用户注册时发生异常", e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 获取用户个人信息接口
     */
    @RequestMapping(value = "/api/user/{userId}", method = "GET")
    public Result<Object> getUserProfile(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.debug("处理获取用户个人信息请求: userId={}", userId);

        try {
            // 获取当前用户ID（可能为null，表示未登录用户）
            Long currentUserId = getUserIdFromToken(request);

            // 调用服务层
            UserProfileVO userProfile = userService.getUserProfile(userId, currentUserId);

            if (userProfile != null) {
                return Result.success(userProfile);
            } else {
                return Result.fail("404", "用户不存在");
            }

        } catch (Exception e) {
            logger.error("获取用户个人信息时发生异常: userId={}", userId, e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 获取关注状态接口
     */
    @RequestMapping(value = "/api/user/{userId}/follow", method = "GET")
    public Result<Object> getFollowStatus(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.debug("处理获取关注状态请求: userId={}", userId);

        try {
            // 验证用户身份
            Long currentUserId = getUserIdFromToken(request);
            if (currentUserId == null) {
                return Result.fail("401", "未授权访问");
            }

            // 调用服务层
            FollowStatusVO followStatus = userService.getFollowStatus(currentUserId, userId);

            if (followStatus != null) {
                return Result.success(followStatus);
            } else {
                return Result.fail("404", "用户不存在");
            }

        } catch (Exception e) {
            logger.error("获取关注状态时发生异常: userId={}", userId, e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 获取关注动态接口
     */
    @RequestMapping(value = "/api/user/follow/feed", method = "GET")
    public Result<FeedResponseVO> getFollowingFeed(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            HttpServletRequest request) {

        logger.debug("处理获取关注动态请求: page={}, size={}", page, size);

        try {
            // 验证用户身份
            Long currentUserId = getUserIdFromToken(request);
            if (currentUserId == null) {
                return Result.fail("401", "未授权访问");
            }

            // 调用服务层
            return userService.getFollowingFeed(currentUserId, page, size, "ALL");

        } catch (Exception e) {
            logger.error("获取关注动态时发生异常: page={}, size={}", page, size, e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 关注用户接口
     */
    @RequestMapping(value = "/api/user/{userId}/follow", method = "POST")
    public Result<Object> followUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.debug("处理关注用户请求: userId={}", userId);

        try {
            // 验证用户身份
            Long currentUserId = getUserIdFromToken(request);
            if (currentUserId == null) {
                return Result.fail("401", "未授权访问");
            }

            // 调用服务层
            FollowResult followResult = userService.followUser(currentUserId, userId);

            // 转换为统一的Result格式
            if (followResult.getSuccess()) {
                return Result.success("关注成功");
            } else {
                return Result.fail(followResult.getError().getCode(), followResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("关注用户时发生异常: userId={}", userId, e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    /**
     * 取消关注用户接口
     */
    @RequestMapping(value = "/api/user/{userId}/follow", method = "DELETE")
    public Result<Object> unfollowUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        logger.debug("处理取消关注用户请求: userId={}", userId);

        try {
            // 验证用户身份
            Long currentUserId = getUserIdFromToken(request);
            if (currentUserId == null) {
                return Result.fail("401", "未授权访问");
            }

            // 调用服务层
            FollowResult unfollowResult = userService.unfollowUser(currentUserId, userId);

            // 转换为统一的Result格式
            if (unfollowResult.getSuccess()) {
                return Result.success("取消关注成功");
            } else {
                return Result.fail(unfollowResult.getError().getCode(), unfollowResult.getError().getMessage());
            }

        } catch (Exception e) {
            logger.error("取消关注用户时发生异常: userId={}", userId, e);
            return Result.fail("500", "服务器内部错误");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return JwtUtil.getUserIdFromToken(token);
            }

            // 兼容性：也支持从X-User-Id头获取
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                return Long.parseLong(userIdHeader);
            }

            return null;
        } catch (Exception e) {
            logger.warn("获取用户ID失败", e);
            return null;
        }
    }
}