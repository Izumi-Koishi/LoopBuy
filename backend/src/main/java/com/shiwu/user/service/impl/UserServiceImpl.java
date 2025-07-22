package com.shiwu.user.service.impl;

import com.shiwu.common.result.Result;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.common.util.PasswordUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.user.dao.FeedDao;
import com.shiwu.product.model.ProductCardVO;
import com.shiwu.product.service.ProductService;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.dao.UserFollowDao;
import com.shiwu.user.model.*;
import com.shiwu.user.service.UserService;
import com.shiwu.user.vo.FeedItemVO;
import com.shiwu.user.vo.FeedResponseVO;
import com.shiwu.user.vo.PaginationVO;


import java.math.BigDecimal;
import java.util.List;

/**
 * 用户服务实现类 - MVC框架版本
 *
 * 使用MVC框架的依赖注入，简化依赖管理
 * 支持用户登录、注册、个人信息管理、关注功能等
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Service
public class UserServiceImpl extends BaseService implements UserService {
    private static final Integer USER_STATUS_NORMAL = 0;
    private static final Integer USER_STATUS_BANNED = 1;
    private static final Integer USER_STATUS_MUTED = 2;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserFollowDao userFollowDao;

    @Autowired
    private FeedDao feedDao;

    @Autowired
    private ProductService productService;

    public UserServiceImpl() {
        logger.info("UserServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public UserServiceImpl(UserDao userDao, UserFollowDao userFollowDao, FeedDao feedDao, ProductService productService) {
        this.userDao = userDao;
        this.userFollowDao = userFollowDao;
        this.feedDao = feedDao;
        this.productService = productService;
        logger.info("UserServiceImpl初始化完成 - 使用兼容性构造函数");
    }

    @Override
    public LoginResult login(String username, String password) {
        logMethodStart("login", username, "***");

        try {
            // 参数校验
            validateNotBlank(username, "username");
            validateNotBlank(password, "password");
        } catch (IllegalArgumentException e) {
            logBusinessWarning("登录失败: {}", e.getMessage());
            return LoginResult.fail(LoginErrorEnum.PARAMETER_ERROR);
        }

        try {
            // 根据用户名查询用户
            User user = userDao.findByUsername(username);
            
            // 用户不存在
            if (user == null) {
                logBusinessWarning("登录失败: 用户 {} 不存在", username);
                return LoginResult.fail(LoginErrorEnum.USER_NOT_FOUND);
            }

            // 检查账户状态
            if (USER_STATUS_BANNED.equals(user.getStatus())) {
                logBusinessWarning("登录失败: 用户 {} 账户已被封禁", username);
                return LoginResult.fail(LoginErrorEnum.ACCOUNT_BANNED);
            }
            
            // 获取当前存储的密码哈希
            String storedHash = user.getPassword();
            
            // 验证密码
            boolean passwordMatches = false;
            
            // 检查是否是旧格式密码（非BCrypt格式）
            if (!PasswordUtil.isBCryptHash(storedHash)) {
                // 使用旧方式验证密码
                if (PasswordUtil.legacyMatches(password, storedHash)) {
                    passwordMatches = true;
                    
                    // 密码验证成功，升级到BCrypt格式
                    String bcryptHash = PasswordUtil.encrypt(password);
                    if (bcryptHash != null) {
                        // 更新数据库中的密码哈希
                        if (userDao.updatePassword(user.getId(), bcryptHash)) {
                            logger.info("用户 {} 的密码哈希已从旧格式升级到BCrypt格式", username);
                        } else {
                            logger.warn("用户 {} 的密码哈希升级失败", username);
                        }
                    }
                }
            } else {
                // 使用BCrypt验证密码
                passwordMatches = PasswordUtil.matches(password, storedHash);
            }
            
            // 密码不正确
            if (!passwordMatches) {
                logBusinessWarning("登录失败: 用户 {} 密码错误", username);
                return LoginResult.fail(LoginErrorEnum.WRONG_PASSWORD);
            }
            
            // 更新用户最后登录时间
            userDao.updateLastLoginTime(user.getId());
            
            // 登录成功，转换为VO对象
            UserVO userVO = convertToVO(user);
            
            // 生成JWT令牌
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            if (token == null) {
                logger.error("用户 {} 登录成功但生成JWT令牌失败", username);
                return LoginResult.fail(LoginErrorEnum.SYSTEM_ERROR);
            }
            
            userVO.setToken(token);
            logBusinessInfo("用户 {} 登录成功并生成JWT令牌", username);
            logMethodSuccess("login", userVO);
            return LoginResult.success(userVO);
        } catch (Exception e) {
            logMethodError("login", e, username, "***");
            return LoginResult.fail(LoginErrorEnum.SYSTEM_ERROR);
        }
    }
    
    @Override
    public RegisterResult register(RegisterRequest registerRequest) {
        logMethodStart("register", registerRequest != null ? registerRequest.getUsername() : null);

        try {
            // 参数校验
            validateNotNull(registerRequest, "registerRequest");
            validateNotBlank(registerRequest.getUsername(), "username");
            validateNotBlank(registerRequest.getPassword(), "password");
        } catch (IllegalArgumentException e) {
            logBusinessWarning("注册失败: {}", e.getMessage());
            return RegisterResult.fail(RegisterErrorEnum.PARAMETER_ERROR);
        }
        
        try {
            // 检查密码强度
            if (!PasswordUtil.isStrongPassword(registerRequest.getPassword())) {
                logger.warn("注册失败: 密码强度不足");
                return RegisterResult.fail(RegisterErrorEnum.WEAK_PASSWORD);
            }
            
            // 检查用户名唯一性
            User existingUser = userDao.findByUsername(registerRequest.getUsername());
            if (existingUser != null) {
                logger.warn("注册失败: 用户名 {} 已存在", registerRequest.getUsername());
                return RegisterResult.fail(RegisterErrorEnum.USERNAME_EXISTS);
            }
            
            // 检查邮箱唯一性
            if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty()) {
                existingUser = userDao.findByEmail(registerRequest.getEmail());
                if (existingUser != null) {
                    logger.warn("注册失败: 邮箱 {} 已被注册", registerRequest.getEmail());
                    return RegisterResult.fail(RegisterErrorEnum.EMAIL_EXISTS);
                }
            }
            
            // 检查手机号唯一性
            if (registerRequest.getPhone() != null && !registerRequest.getPhone().isEmpty()) {
                existingUser = userDao.findByPhone(registerRequest.getPhone());
                if (existingUser != null) {
                    logger.warn("注册失败: 手机号 {} 已被注册", registerRequest.getPhone());
                    return RegisterResult.fail(RegisterErrorEnum.PHONE_EXISTS);
                }
            }
            
            // 创建新用户
            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername());
            // 使用BCrypt加盐哈希处理密码
            String hashedPassword = PasswordUtil.encrypt(registerRequest.getPassword());
            if (hashedPassword == null) {
                logger.error("注册失败: 密码加密错误");
                return RegisterResult.fail(RegisterErrorEnum.SYSTEM_ERROR);
            }
            newUser.setPassword(hashedPassword);
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPhone(registerRequest.getPhone());
            newUser.setNickname(registerRequest.getNickname());
            newUser.setStatus(USER_STATUS_NORMAL);
            
            Long userId = userDao.createUser(newUser);
            if (userId == null) {
                logger.error("注册失败: 创建用户数据库记录失败");
                return RegisterResult.fail(RegisterErrorEnum.SYSTEM_ERROR);
            }
            
            // 设置用户ID
            newUser.setId(userId);
            
            // 转换为VO对象
            UserVO userVO = convertToVO(newUser);
            
            // 生成JWT令牌
            String token = JwtUtil.generateToken(userId, newUser.getUsername());
            if (token == null) {
                logger.error("用户 {} 注册成功但生成JWT令牌失败", newUser.getUsername());
                return RegisterResult.fail(RegisterErrorEnum.SYSTEM_ERROR);
            }
            
            userVO.setToken(token);
            logger.info("用户 {} 注册成功并生成JWT令牌", newUser.getUsername());
            return RegisterResult.success(userVO);
        } catch (Exception e) {
            logger.error("注册过程发生异常: {}", e.getMessage(), e);
            return RegisterResult.fail(RegisterErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public UserProfileVO getUserProfile(Long userId, Long currentUserId) {
        // 参数校验
        if (userId == null) {
            logger.warn("获取用户公开信息失败: 用户ID为空");
            return null;
        }

        try {
            // 获取用户公开信息
            User user = userDao.findPublicInfoById(userId);
            if (user == null) {
                logger.warn("获取用户公开信息失败: 用户 {} 不存在", userId);
                return null;
            }

            // 检查用户状态，被封禁的用户不显示公开信息
            if (USER_STATUS_BANNED.equals(user.getStatus())) {
                logger.warn("获取用户公开信息失败: 用户 {} 已被封禁", userId);
                return null;
            }

            // 构建UserProfileVO
            UserProfileVO profileVO = new UserProfileVO();

            // 设置基本用户信息
            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatarUrl(user.getAvatarUrl());
            profileVO.setUser(userVO);

            // 设置统计信息
            profileVO.setFollowerCount(user.getFollowerCount());
            profileVO.setAverageRating(user.getAverageRating());
            profileVO.setRegistrationDate(user.getCreateTime());

            // 获取在售商品列表
            // 根据模块解耦原则，调用ProductService获取在售商品
            List<ProductCardVO> onSaleProducts = productService.getProductsBySellerIdAndStatus(userId, 1); // 1表示在售状态
            profileVO.setOnSaleProducts(onSaleProducts);

            // 判断当前用户是否关注了该用户
            boolean isFollowing = false;
            if (currentUserId != null && !currentUserId.equals(userId)) {
                // 查询关注关系
                isFollowing = userFollowDao.isFollowing(currentUserId, userId);
            }
            profileVO.setIsFollowing(isFollowing);

            logger.info("成功获取用户 {} 的公开信息", userId);
            return profileVO;
        } catch (Exception e) {
            logger.error("获取用户公开信息过程发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public UserProfileResult getUserProfileWithErrorHandling(Long userId, Long currentUserId) {
        // 参数校验
        if (userId == null) {
            logger.warn("获取用户主页失败: 用户ID为空");
            return UserProfileResult.fail(UserProfileErrorEnum.PARAMETER_ERROR);
        }

        // 验证用户ID格式（必须为正数）
        if (userId <= 0) {
            logger.warn("获取用户主页失败: 用户ID格式错误 {}", userId);
            return UserProfileResult.fail(UserProfileErrorEnum.INVALID_USER_ID);
        }

        try {
            // 获取用户公开信息
            User user = userDao.findPublicInfoById(userId);
            if (user == null) {
                logger.warn("获取用户主页失败: 用户 {} 不存在", userId);
                return UserProfileResult.fail(UserProfileErrorEnum.USER_NOT_FOUND);
            }

            // 检查用户状态
            if (USER_STATUS_BANNED.equals(user.getStatus())) {
                logger.warn("获取用户主页失败: 用户 {} 已被封禁", userId);
                return UserProfileResult.fail(UserProfileErrorEnum.USER_BANNED);
            }

            if (USER_STATUS_MUTED.equals(user.getStatus())) {
                logger.warn("获取用户主页失败: 用户 {} 已被禁言", userId);
                return UserProfileResult.fail(UserProfileErrorEnum.USER_MUTED);
            }

            // 构建UserProfileVO
            UserProfileVO profileVO = new UserProfileVO();

            // 设置基本用户信息
            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatarUrl(user.getAvatarUrl());
            profileVO.setUser(userVO);

            // 设置统计信息
            profileVO.setFollowerCount(user.getFollowerCount());
            profileVO.setAverageRating(user.getAverageRating());
            profileVO.setRegistrationDate(user.getCreateTime());

            // 获取在售商品列表
            profileVO.setOnSaleProducts(userDao.findOnSaleProductsByUserId(userId));

            // 判断当前用户是否关注了该用户
            if (currentUserId != null) {
                profileVO.setIsFollowing(userFollowDao.isFollowing(currentUserId, userId));
            } else {
                profileVO.setIsFollowing(false);
            }

            logger.info("成功获取用户 {} 的主页信息", userId);
            return UserProfileResult.success(profileVO);

        } catch (Exception e) {
            logger.error("获取用户主页过程发生异常: {}", e.getMessage(), e);
            return UserProfileResult.fail(UserProfileErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public FollowResult followUser(Long currentUserId, Long targetUserId) {
        // 参数校验
        if (currentUserId == null) {
            logger.warn("关注用户失败: 当前用户ID为空");
            return FollowResult.fail(FollowErrorEnum.NOT_AUTHENTICATED);
        }

        if (targetUserId == null) {
            logger.warn("关注用户失败: 目标用户ID为空");
            return FollowResult.fail(FollowErrorEnum.PARAMETER_ERROR);
        }

        // 验证用户ID格式
        if (currentUserId <= 0 || targetUserId <= 0) {
            logger.warn("关注用户失败: 用户ID格式错误 currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            return FollowResult.fail(FollowErrorEnum.INVALID_USER_ID);
        }

        // 不能关注自己
        if (currentUserId.equals(targetUserId)) {
            logger.warn("关注用户失败: 不能关注自己 userId={}", currentUserId);
            return FollowResult.fail(FollowErrorEnum.CANNOT_FOLLOW_SELF);
        }

        try {
            // 检查目标用户是否存在
            User targetUser = userDao.findPublicInfoById(targetUserId);
            if (targetUser == null) {
                logger.warn("关注用户失败: 目标用户 {} 不存在", targetUserId);
                return FollowResult.fail(FollowErrorEnum.TARGET_USER_NOT_FOUND);
            }

            // 检查目标用户状态
            if (USER_STATUS_BANNED.equals(targetUser.getStatus())) {
                logger.warn("关注用户失败: 目标用户 {} 已被封禁", targetUserId);
                return FollowResult.fail(FollowErrorEnum.TARGET_USER_BANNED);
            }

            // 检查是否已经关注
            if (userFollowDao.isFollowing(currentUserId, targetUserId)) {
                logger.warn("关注用户失败: 已经关注了用户 {}", targetUserId);
                return FollowResult.fail(FollowErrorEnum.ALREADY_FOLLOWING);
            }

            // 执行关注操作
            boolean success = userFollowDao.followUser(currentUserId, targetUserId);
            if (!success) {
                logger.error("关注用户失败: 数据库操作失败 currentUserId={}, targetUserId={}", currentUserId, targetUserId);
                return FollowResult.fail(FollowErrorEnum.DATABASE_ERROR);
            }

            // 获取更新后的粉丝数量
            int followerCount = userFollowDao.getFollowerCount(targetUserId);

            logger.info("关注用户成功: currentUserId={}, targetUserId={}, followerCount={}", currentUserId, targetUserId, followerCount);
            return FollowResult.success(true, followerCount);

        } catch (Exception e) {
            logger.error("关注用户过程发生异常: currentUserId={}, targetUserId={}, error={}", currentUserId, targetUserId, e.getMessage(), e);
            return FollowResult.fail(FollowErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public FollowResult unfollowUser(Long currentUserId, Long targetUserId) {
        // 参数校验
        if (currentUserId == null) {
            logger.warn("取关用户失败: 当前用户ID为空");
            return FollowResult.fail(FollowErrorEnum.NOT_AUTHENTICATED);
        }

        if (targetUserId == null) {
            logger.warn("取关用户失败: 目标用户ID为空");
            return FollowResult.fail(FollowErrorEnum.PARAMETER_ERROR);
        }

        // 验证用户ID格式
        if (currentUserId <= 0 || targetUserId <= 0) {
            logger.warn("取关用户失败: 用户ID格式错误 currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            return FollowResult.fail(FollowErrorEnum.INVALID_USER_ID);
        }

        // 不能取关自己
        if (currentUserId.equals(targetUserId)) {
            logger.warn("取关用户失败: 不能取关自己 userId={}", currentUserId);
            return FollowResult.fail(FollowErrorEnum.CANNOT_FOLLOW_SELF);
        }

        try {
            // 检查目标用户是否存在
            User targetUser = userDao.findPublicInfoById(targetUserId);
            if (targetUser == null) {
                logger.warn("取关用户失败: 目标用户 {} 不存在", targetUserId);
                return FollowResult.fail(FollowErrorEnum.TARGET_USER_NOT_FOUND);
            }

            // 检查是否已经关注
            if (!userFollowDao.isFollowing(currentUserId, targetUserId)) {
                logger.warn("取关用户失败: 未关注用户 {}", targetUserId);
                return FollowResult.fail(FollowErrorEnum.NOT_FOLLOWING);
            }

            // 执行取关操作
            boolean success = userFollowDao.unfollowUser(currentUserId, targetUserId);
            if (!success) {
                logger.error("取关用户失败: 数据库操作失败 currentUserId={}, targetUserId={}", currentUserId, targetUserId);
                return FollowResult.fail(FollowErrorEnum.DATABASE_ERROR);
            }

            // 获取更新后的粉丝数量
            int followerCount = userFollowDao.getFollowerCount(targetUserId);

            logger.info("取关用户成功: currentUserId={}, targetUserId={}, followerCount={}", currentUserId, targetUserId, followerCount);
            return FollowResult.success(false, followerCount);

        } catch (Exception e) {
            logger.error("取关用户过程发生异常: currentUserId={}, targetUserId={}, error={}", currentUserId, targetUserId, e.getMessage(), e);
            return FollowResult.fail(FollowErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public FollowStatusVO getFollowStatus(Long currentUserId, Long targetUserId) {
        // 参数校验
        if (targetUserId == null) {
            logger.warn("获取关注状态失败: 目标用户ID为空");
            return null;
        }

        // 验证用户ID格式
        if (targetUserId <= 0) {
            logger.warn("获取关注状态失败: 目标用户ID格式错误 {}", targetUserId);
            return null;
        }

        try {
            // 检查目标用户是否存在
            User targetUser = userDao.findPublicInfoById(targetUserId);
            if (targetUser == null) {
                logger.warn("获取关注状态失败: 目标用户 {} 不存在", targetUserId);
                return null;
            }

            // 构建关注状态VO
            FollowStatusVO statusVO = new FollowStatusVO();
            statusVO.setUserId(targetUser.getId());
            statusVO.setUsername(targetUser.getUsername());
            statusVO.setNickname(targetUser.getNickname());

            // 获取粉丝数量和关注数量
            statusVO.setFollowerCount(userFollowDao.getFollowerCount(targetUserId));
            statusVO.setFollowingCount(userFollowDao.getFollowingCount(targetUserId));

            // 判断当前用户是否关注了目标用户
            if (currentUserId != null && currentUserId > 0) {
                statusVO.setIsFollowing(userFollowDao.isFollowing(currentUserId, targetUserId));
            } else {
                statusVO.setIsFollowing(false);
            }

            logger.info("成功获取关注状态: currentUserId={}, targetUserId={}, isFollowing={}",
                    currentUserId, targetUserId, statusVO.getIsFollowing());
            return statusVO;

        } catch (Exception e) {
            logger.error("获取关注状态过程发生异常: currentUserId={}, targetUserId={}, error={}",
                    currentUserId, targetUserId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public boolean updateUserAverageRating(Long userId) {
        // 参数校验
        if (userId == null) {
            logger.warn("更新用户平均评分失败: 用户ID为空");
            return false;
        }

        try {
            // 检查用户是否存在
            User user = userDao.findById(userId);
            if (user == null) {
                logger.warn("更新用户平均评分失败: 用户 {} 不存在", userId);
                return false;
            }

            // 计算用户的平均评分
            BigDecimal averageRating = userDao.calculateUserAverageRating(userId);

            // 如果没有评价，设置为0
            if (averageRating == null) {
                averageRating = BigDecimal.ZERO;
            }

            // 更新用户的平均评分
            boolean success = userDao.updateAverageRating(userId, averageRating);
            if (success) {
                logger.info("更新用户平均评分成功: userId={}, averageRating={}", userId, averageRating);
                return true;
            } else {
                logger.error("更新用户平均评分失败: 数据库操作失败 userId={}", userId);
                return false;
            }

        } catch (Exception e) {
            logger.error("更新用户平均评分过程发生异常: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    
    @Override
    public Result<FeedResponseVO> getFollowingFeed(Long userId, int page, int size, String type) {
        // 参数校验
        if (userId == null) {
            logger.warn("获取关注动态失败: 用户ID为空");
            return Result.error("用户ID不能为空");
        }

        if (page < 1) {
            logger.warn("获取关注动态失败: 页码无效 page={}", page);
            return Result.error("页码必须大于0");
        }

        if (size < 1 || size > 100) {
            logger.warn("获取关注动态失败: 每页大小无效 size={}", size);
            return Result.error("每页大小必须在1-100之间");
        }

        // 验证动态类型
        if (type != null && !"ALL".equals(type) &&
            !"PRODUCT_APPROVED".equals(type) && !"PRODUCT_PUBLISHED".equals(type)) {
            logger.warn("获取关注动态失败: 动态类型无效 type={}", type);
            return Result.error("动态类型无效");
        }

        try {
            // 计算偏移量
            int offset = (page - 1) * size;

            // 获取动态列表
            List<FeedItemVO> feeds = feedDao.getFollowingFeed(userId, type, offset, size);

            // 获取总数量
            long total = feedDao.getFollowingFeedCount(userId, type);

            // 构建分页信息
            PaginationVO pagination = new PaginationVO(page, size, total);

            // 构建响应
            FeedResponseVO response = new FeedResponseVO(feeds, pagination);

            logger.info("获取关注动态成功: userId={}, type={}, page={}, size={}, total={}",
                       userId, type, page, size, total);

            return Result.success(response);

        } catch (Exception e) {
            logger.error("获取关注动态过程发生异常: userId={}, type={}, page={}, size={}, error={}",
                        userId, type, page, size, e.getMessage(), e);
            return Result.error("获取关注动态失败");
        }
    }

    /**
     * 将User实体转换为UserVO视图对象
     * @param user 用户实体
     * @return 用户视图对象
     */
    private UserVO convertToVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setStatus(user.getStatus());
        userVO.setAvatarUrl(user.getAvatarUrl());
        userVO.setNickname(user.getNickname());
        userVO.setGender(user.getGender());
        return userVO;
    }
}