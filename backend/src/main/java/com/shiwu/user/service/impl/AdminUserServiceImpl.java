package com.shiwu.user.service.impl;

import com.shiwu.admin.enums.AuditActionEnum;
import com.shiwu.admin.enums.AuditTargetTypeEnum;
import com.shiwu.admin.model.AdminUserQueryDTO;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.user.dao.AdminUserDao;
import com.shiwu.user.dao.UserDao;
import com.shiwu.user.model.User;
import com.shiwu.user.service.AdminUserService;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户服务实现类 - MVC框架版本
 *
 * 使用MVC框架的依赖注入，简化依赖管理
 * 实现NFR-SEC-03要求，在所有敏感操作中嵌入审计日志记录
 * 支持用户管理、封禁、禁言等管理员功能
 *
 * @author LoopBuy Team
 * @version 2.0 (MVC Framework)
 */
@Service
public class AdminUserServiceImpl extends BaseService implements AdminUserService {

    // 用户状态常量
    private static final Integer USER_STATUS_NORMAL = 0;
    private static final Integer USER_STATUS_BANNED = 1;
    private static final Integer USER_STATUS_MUTED = 2;

    @Autowired
    private AdminUserDao adminUserDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private AuditLogService auditLogService;

    public AdminUserServiceImpl() {
        logger.info("AdminUserServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public AdminUserServiceImpl(AdminUserDao adminUserDao, UserDao userDao, AuditLogService auditLogService) {
        this.adminUserDao = adminUserDao;
        this.userDao = userDao;
        this.auditLogService = auditLogService;
        logger.info("AdminUserServiceImpl初始化完成 - 使用兼容性构造函数");
    }

    @Override
    public Map<String, Object> findUsers(AdminUserQueryDTO queryDTO) {
        logMethodStart("findUsers", queryDTO);

        try {
            validateNotNull(queryDTO, "queryDTO");
            validatePagination(queryDTO.getPageNum(), queryDTO.getPageSize());
            // 查询用户列表
            List<Map<String, Object>> users = adminUserDao.findUsers(queryDTO);

            // 查询总数
            long totalCount = adminUserDao.countUsers(queryDTO);

            // 构建返回结果 - 保持原有的key名称
            Map<String, Object> result = new HashMap<>();
            result.put("users", users);
            result.put("totalCount", totalCount);
            result.put("page", queryDTO.getPageNum());
            result.put("pageSize", queryDTO.getPageSize());
            result.put("totalPages", (totalCount + queryDTO.getPageSize() - 1) / queryDTO.getPageSize());

            logBusinessInfo("查询用户列表成功: 共{}条记录", totalCount);
            logMethodSuccess("findUsers", result);
            return result;
        } catch (IllegalArgumentException e) {
            logBusinessWarning("查询用户列表失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logMethodError("findUsers", e, queryDTO);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUserDetail(Long userId, Long adminId) {
        logMethodStart("getUserDetail", userId, adminId);

        try {
            validateId(userId, "userId");
            validateId(adminId, "adminId");
        } catch (IllegalArgumentException e) {
            logBusinessWarning("获取用户详情失败: {}", e.getMessage());
            return null;
        }

        try {
            // 管理员可以查看所有用户详情（包括被封禁的用户）
            User user = userDao.findById(userId);
            
            if (user == null) {
                logger.warn("获取用户详情失败: 用户不存在, userId={}", userId);
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("user", user);
            
            logger.info("管理员 {} 获取用户详情成功: userId={}", adminId, userId);
            return result;
        } catch (Exception e) {
            logger.error("获取用户详情失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean banUser(Long userId, Long adminId, String reason, String ipAddress, String userAgent) {
        if (userId == null || adminId == null) {
            logger.warn("封禁用户失败: 参数为空");
            return false;
        }

        try {
            // 检查用户是否存在
            User user = userDao.findById(userId);
            if (user == null) {
                logger.warn("封禁用户失败: 用户不存在, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_BAN, AuditTargetTypeEnum.USER, 
                                         userId, "封禁用户失败: 用户不存在" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 检查用户当前状态
            if (USER_STATUS_BANNED.equals(user.getStatus())) {
                logger.warn("封禁用户失败: 用户已被封禁, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_BAN, AuditTargetTypeEnum.USER, 
                                         userId, "封禁用户失败: 用户已被封禁" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 更新用户状态为封禁
            boolean success = adminUserDao.updateUserStatus(userId, USER_STATUS_BANNED, adminId);
            
            // 记录审计日志
            String details = "封禁用户: " + user.getUsername() + " (ID: " + userId + ")" + 
                           (reason != null ? ", 原因: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.USER_BAN, AuditTargetTypeEnum.USER, 
                                     userId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 封禁用户 {} 成功", adminId, userId);
                return true;
            } else {
                logger.warn("封禁用户失败: 更新状态失败, userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            logger.error("封禁用户失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_BAN, AuditTargetTypeEnum.USER, 
                                     userId, "封禁用户异常: " + e.getMessage() + (reason != null ? ", 原因: " + reason : ""), 
                                     ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean muteUser(Long userId, Long adminId, String reason, String ipAddress, String userAgent) {
        if (userId == null || adminId == null) {
            logger.warn("禁言用户失败: 参数为空");
            return false;
        }

        try {
            // 检查用户是否存在
            User user = userDao.findById(userId);
            if (user == null) {
                logger.warn("禁言用户失败: 用户不存在, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_MUTE, AuditTargetTypeEnum.USER, 
                                         userId, "禁言用户失败: 用户不存在" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 检查用户当前状态
            if (USER_STATUS_BANNED.equals(user.getStatus())) {
                logger.warn("禁言用户失败: 用户已被封禁, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_MUTE, AuditTargetTypeEnum.USER, 
                                         userId, "禁言用户失败: 用户已被封禁" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            if (USER_STATUS_MUTED.equals(user.getStatus())) {
                logger.warn("禁言用户失败: 用户已被禁言, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_MUTE, AuditTargetTypeEnum.USER, 
                                         userId, "禁言用户失败: 用户已被禁言" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 更新用户状态为禁言
            boolean success = adminUserDao.updateUserStatus(userId, USER_STATUS_MUTED, adminId);
            
            // 记录审计日志
            String details = "禁言用户: " + user.getUsername() + " (ID: " + userId + ")" + 
                           (reason != null ? ", 原因: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.USER_MUTE, AuditTargetTypeEnum.USER, 
                                     userId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 禁言用户 {} 成功", adminId, userId);
                return true;
            } else {
                logger.warn("禁言用户失败: 更新状态失败, userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            logger.error("禁言用户失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_MUTE, AuditTargetTypeEnum.USER, 
                                     userId, "禁言用户异常: " + e.getMessage() + (reason != null ? ", 原因: " + reason : ""), 
                                     ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean unbanUser(Long userId, Long adminId, String ipAddress, String userAgent) {
        if (userId == null || adminId == null) {
            logger.warn("解封用户失败: 参数为空");
            return false;
        }

        try {
            // 检查用户是否存在
            User user = userDao.findById(userId);
            if (user == null) {
                logger.warn("解封用户失败: 用户不存在, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_UNBAN, AuditTargetTypeEnum.USER, 
                                         userId, "解封用户失败: 用户不存在", ipAddress, userAgent, false);
                return false;
            }

            // 检查用户当前状态
            if (!USER_STATUS_BANNED.equals(user.getStatus())) {
                logger.warn("解封用户失败: 用户未被封禁, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_UNBAN, AuditTargetTypeEnum.USER, 
                                         userId, "解封用户失败: 用户未被封禁", ipAddress, userAgent, false);
                return false;
            }

            // 更新用户状态为正常
            boolean success = adminUserDao.updateUserStatus(userId, USER_STATUS_NORMAL, adminId);
            
            // 记录审计日志
            String details = "解封用户: " + user.getUsername() + " (ID: " + userId + ")";
            auditLogService.logAction(adminId, AuditActionEnum.USER_UNBAN, AuditTargetTypeEnum.USER, 
                                     userId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 解封用户 {} 成功", adminId, userId);
                return true;
            } else {
                logger.warn("解封用户失败: 更新状态失败, userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            logger.error("解封用户失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_UNBAN, AuditTargetTypeEnum.USER, 
                                     userId, "解封用户异常: " + e.getMessage(), ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean unmuteUser(Long userId, Long adminId, String ipAddress, String userAgent) {
        if (userId == null || adminId == null) {
            logger.warn("解除禁言失败: 参数为空");
            return false;
        }

        try {
            // 检查用户是否存在
            User user = userDao.findById(userId);
            if (user == null) {
                logger.warn("解除禁言失败: 用户不存在, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_UNMUTE, AuditTargetTypeEnum.USER, 
                                         userId, "解除禁言失败: 用户不存在", ipAddress, userAgent, false);
                return false;
            }

            // 检查用户当前状态
            if (!USER_STATUS_MUTED.equals(user.getStatus())) {
                logger.warn("解除禁言失败: 用户未被禁言, userId={}", userId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.USER_UNMUTE, AuditTargetTypeEnum.USER, 
                                         userId, "解除禁言失败: 用户未被禁言", ipAddress, userAgent, false);
                return false;
            }

            // 更新用户状态为正常
            boolean success = adminUserDao.updateUserStatus(userId, USER_STATUS_NORMAL, adminId);
            
            // 记录审计日志
            String details = "解除禁言: " + user.getUsername() + " (ID: " + userId + ")";
            auditLogService.logAction(adminId, AuditActionEnum.USER_UNMUTE, AuditTargetTypeEnum.USER, 
                                     userId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 解除用户 {} 禁言成功", adminId, userId);
                return true;
            } else {
                logger.warn("解除禁言失败: 更新状态失败, userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            logger.error("解除禁言失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_UNMUTE, AuditTargetTypeEnum.USER, 
                                     userId, "解除禁言异常: " + e.getMessage(), ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public Map<String, Object> batchBanUsers(List<Long> userIds, Long adminId, String reason, String ipAddress, String userAgent) {
        if (userIds == null || userIds.isEmpty() || adminId == null) {
            logger.warn("批量封禁用户失败: 参数为空");
            return null;
        }

        try {
            int totalCount = userIds.size();
            int successCount = 0;
            int failCount = 0;

            for (Long userId : userIds) {
                try {
                    boolean success = banUser(userId, adminId, reason, ipAddress, userAgent);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    logger.error("批量封禁用户失败: userId={}, error={}", userId, e.getMessage());
                    failCount++;
                }
            }

            // 记录批量操作的审计日志
            String details = String.format("批量封禁用户: 总数=%d, 成功=%d, 失败=%d%s",
                                          totalCount, successCount, failCount,
                                          reason != null ? ", 原因: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.USER_BATCH_BAN, AuditTargetTypeEnum.USER,
                                     null, details, ipAddress, userAgent, failCount == 0);

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("successCount", successCount);
            result.put("failCount", failCount);

            logger.info("管理员 {} 批量封禁用户完成: 总数={}, 成功={}, 失败={}", adminId, totalCount, successCount, failCount);
            return result;
        } catch (Exception e) {
            logger.error("批量封禁用户失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_BATCH_BAN, AuditTargetTypeEnum.USER,
                                     null, "批量封禁用户异常: " + e.getMessage(), ipAddress, userAgent, false);
            return null;
        }
    }

    @Override
    public Map<String, Object> batchMuteUsers(List<Long> userIds, Long adminId, String reason, String ipAddress, String userAgent) {
        if (userIds == null || userIds.isEmpty() || adminId == null) {
            logger.warn("批量禁言用户失败: 参数为空");
            return null;
        }

        try {
            int totalCount = userIds.size();
            int successCount = 0;
            int failCount = 0;

            for (Long userId : userIds) {
                try {
                    boolean success = muteUser(userId, adminId, reason, ipAddress, userAgent);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    logger.error("批量禁言用户失败: userId={}, error={}", userId, e.getMessage());
                    failCount++;
                }
            }

            // 记录批量操作的审计日志
            String details = String.format("批量禁言用户: 总数=%d, 成功=%d, 失败=%d%s",
                                          totalCount, successCount, failCount,
                                          reason != null ? ", 原因: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.USER_BATCH_MUTE, AuditTargetTypeEnum.USER,
                                     null, details, ipAddress, userAgent, failCount == 0);

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("successCount", successCount);
            result.put("failCount", failCount);

            logger.info("管理员 {} 批量禁言用户完成: 总数={}, 成功={}, 失败={}", adminId, totalCount, successCount, failCount);
            return result;
        } catch (Exception e) {
            logger.error("批量禁言用户失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.USER_BATCH_MUTE, AuditTargetTypeEnum.USER,
                                     null, "批量禁言用户异常: " + e.getMessage(), ipAddress, userAgent, false);
            return null;
        }
    }
}
