package com.shiwu.admin.service.impl;

import com.shiwu.admin.dao.AdminDao;
import com.shiwu.admin.enums.AuditActionEnum;
import com.shiwu.admin.enums.AuditTargetTypeEnum;
import com.shiwu.admin.model.*;
import com.shiwu.admin.service.AdminService;
import com.shiwu.admin.service.AuditLogService;
//import com.shiwu.admin.service.impl.AuditLogServiceImpl;
import com.shiwu.common.util.JwtUtil;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员服务实现类 - MVC框架版本
 */
@Service
public class AdminServiceImpl extends BaseService implements AdminService {
    private static final Integer ADMIN_STATUS_NORMAL = 1;
    private static final Integer ADMIN_STATUS_DISABLED = 0;

    @Autowired
    private AdminDao adminDao;

    @Autowired
    private AuditLogService auditLogService;

    // 存储待确认的操作上下文（生产环境应使用Redis等缓存）
    private final Map<String, OperationContext> operationContexts = new ConcurrentHashMap<>();

    // 无参构造函数，用于MVC框架依赖注入
    public AdminServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.adminDao = new AdminDao();
        this.auditLogService = new AuditLogServiceImpl();
        logger.info("AdminServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public AdminServiceImpl(AdminDao adminDao, AuditLogService auditLogService) {
        this.adminDao = adminDao;
        this.auditLogService = auditLogService;
        logger.info("AdminServiceImpl初始化完成 - 使用兼容性构造函数");
    }

    @Override
    public AdminLoginResult login(String username, String password, String ipAddress, String userAgent) {
        // 参数校验
        if (username == null || password == null) {
            logger.warn("管理员登录失败: 用户名或密码为空");
            return AdminLoginResult.fail(AdminLoginErrorEnum.PARAMETER_ERROR);
        }

        try {
            // 查找管理员
            Administrator admin = adminDao.findByUsername(username);
            if (admin == null) {
                logger.warn("管理员登录失败: 管理员不存在, username={}", username);
                // 记录失败的登录尝试
                try {
                    auditLogService.logAction(null, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                            null, "管理员登录失败: 管理员不存在 - " + username,
                                            ipAddress, userAgent, false);
                } catch (Exception e) {
                    logger.warn("记录审计日志失败: {}", e.getMessage());
                }
                return AdminLoginResult.fail(AdminLoginErrorEnum.ADMIN_NOT_FOUND);
            }

            // 检查管理员状态
            if (admin.getDeleted() != null && admin.getDeleted()) {
                logger.warn("管理员登录失败: 管理员已删除, username={}", username);
                auditLogService.logAction(admin.getId(), AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         admin.getId(), "管理员登录失败: 管理员已删除",
                                         ipAddress, userAgent, false);
                return AdminLoginResult.fail(AdminLoginErrorEnum.ADMIN_DELETED);
            }

            if (!ADMIN_STATUS_NORMAL.equals(admin.getStatus())) {
                logger.warn("管理员登录失败: 管理员账户被禁用, username={}", username);
                auditLogService.logAction(admin.getId(), AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         admin.getId(), "管理员登录失败: 管理员账户被禁用",
                                         ipAddress, userAgent, false);
                return AdminLoginResult.fail(AdminLoginErrorEnum.ADMIN_DISABLED);
            }

            // 验证密码
            boolean passwordMatches = BCrypt.checkpw(password, admin.getPassword());
            if (!passwordMatches) {
                logger.warn("管理员登录失败: 密码错误, username={}", username);
                auditLogService.logAction(admin.getId(), AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         admin.getId(), "管理员登录失败: 密码错误",
                                         ipAddress, userAgent, false);
                return AdminLoginResult.fail(AdminLoginErrorEnum.WRONG_PASSWORD);
            }

            // 更新管理员最后登录时间和登录次数
            adminDao.updateLastLoginInfo(admin.getId());

            // 登录成功，转换为VO对象
            AdminVO adminVO = convertToVO(admin);

            // 生成JWT令牌（包含管理员角色信息）
            String token = generateAdminToken(admin.getId(), admin.getUsername(), admin.getRole());
            if (token == null) {
                logger.error("管理员 {} 登录成功但生成JWT令牌失败", username);
                auditLogService.logAction(admin.getId(), AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         admin.getId(), "管理员登录失败: 生成JWT令牌失败",
                                         ipAddress, userAgent, false);
                return AdminLoginResult.fail(AdminLoginErrorEnum.SYSTEM_ERROR);
            }

            adminVO.setToken(token);
            
            // 记录成功的登录
            auditLogService.logAction(admin.getId(), AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                     admin.getId(), "管理员登录成功",
                                     ipAddress, userAgent, true);
            
            logger.info("管理员 {} 登录成功并生成JWT令牌", username);
            return AdminLoginResult.success(adminVO);
        } catch (Exception e) {
            logger.error("管理员登录过程发生异常: {}", e.getMessage(), e);
            return AdminLoginResult.fail(AdminLoginErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public boolean hasPermission(Long adminId, String requiredRole) {
        if (adminId == null) {
            logger.warn("权限检查失败: 管理员ID为空");
            return false;
        }

        try {
            Administrator admin = adminDao.findById(adminId);
            if (admin == null) {
                logger.warn("权限检查失败: 管理员不存在, adminId={}", adminId);
                return false;
            }

            // 检查管理员状态
            if (admin.getDeleted() != null && admin.getDeleted()) {
                logger.warn("权限检查失败: 管理员已删除, adminId={}", adminId);
                return false;
            }

            if (!ADMIN_STATUS_NORMAL.equals(admin.getStatus())) {
                logger.warn("权限检查失败: 管理员账户被禁用, adminId={}", adminId);
                return false;
            }

            // 如果没有指定需要的角色，任何有效的管理员都有权限
            if (requiredRole == null || requiredRole.trim().isEmpty()) {
                return true;
            }

            // 超级管理员拥有所有权限
            if (AdminRole.isSuperAdmin(admin.getRole())) {
                return true;
            }

            // 检查角色匹配
            return requiredRole.equals(admin.getRole());
        } catch (Exception e) {
            logger.error("权限检查过程发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isSuperAdmin(Long adminId) {
        if (adminId == null) {
            return false;
        }

        try {
            Administrator admin = adminDao.findById(adminId);
            if (admin == null) {
                return false;
            }

            return AdminRole.isSuperAdmin(admin.getRole()) && 
                   ADMIN_STATUS_NORMAL.equals(admin.getStatus()) &&
                   (admin.getDeleted() == null || !admin.getDeleted());
        } catch (Exception e) {
            logger.error("检查超级管理员权限时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将Administrator转换为AdminVO
     */
    private AdminVO convertToVO(Administrator admin) {
        AdminVO adminVO = new AdminVO();
        adminVO.setId(admin.getId());
        adminVO.setUsername(admin.getUsername());
        adminVO.setEmail(admin.getEmail());
        adminVO.setRealName(admin.getRealName());
        adminVO.setRole(admin.getRole());
        
        // 设置角色描述
        AdminRole role = AdminRole.fromCode(admin.getRole());
        if (role != null) {
            adminVO.setRoleDescription(role.getDescription());
        }
        
        adminVO.setStatus(admin.getStatus());
        adminVO.setLastLoginTime(admin.getLastLoginTime());
        adminVO.setLoginCount(admin.getLoginCount());
        adminVO.setCreateTime(admin.getCreateTime());
        
        return adminVO;
    }

    @Override
    public SecondaryConfirmationResult verifySecondaryConfirmation(Long adminId, String password,
                                                                  String operationCode, String ipAddress, String userAgent) {
        // 参数校验
        if (adminId == null) {
            logger.warn("二次确认失败: 管理员ID为空");
            return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.PARAMETER_ERROR);
        }

        if (password == null || password.trim().isEmpty()) {
            logger.warn("二次确认失败: 密码为空");
            return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.PASSWORD_EMPTY);
        }

        if (operationCode == null || operationCode.trim().isEmpty()) {
            logger.warn("二次确认失败: 操作代码为空");
            return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.OPERATION_CODE_EMPTY);
        }

        try {
            // 查找管理员
            Administrator admin = adminDao.findById(adminId);
            if (admin == null) {
                logger.warn("二次确认失败: 管理员不存在, adminId={}", adminId);
                auditLogService.logAction(adminId, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         adminId, "二次确认失败: 管理员不存在",
                                         ipAddress, userAgent, false);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.ADMIN_NOT_FOUND);
            }

            // 检查管理员状态
            if (admin.getDeleted() != null && admin.getDeleted()) {
                logger.warn("二次确认失败: 管理员已删除, adminId={}", adminId);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.ADMIN_NOT_FOUND);
            }

            if (!ADMIN_STATUS_NORMAL.equals(admin.getStatus())) {
                logger.warn("二次确认失败: 管理员账户被禁用, adminId={}", adminId);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.ADMIN_DISABLED);
            }

            // 验证密码
            boolean passwordMatches = BCrypt.checkpw(password, admin.getPassword());
            if (!passwordMatches) {
                logger.warn("二次确认失败: 密码错误, adminId={}", adminId);
                auditLogService.logAction(adminId, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         adminId, "二次确认密码错误: " + operationCode,
                                         ipAddress, userAgent, false);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.WRONG_PASSWORD);
            }

            // 检查操作权限
            HighRiskOperation operation = HighRiskOperation.fromCode(operationCode);
            if (operation == null) {
                logger.warn("二次确认失败: 操作不存在, operationCode={}", operationCode);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.OPERATION_NOT_FOUND);
            }

            if (!HighRiskOperation.hasPermissionForOperation(operation, admin.getRole())) {
                logger.warn("二次确认失败: 权限不足, adminId={}, operationCode={}, role={}",
                          adminId, operationCode, admin.getRole());
                auditLogService.logAction(adminId, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                         adminId, "二次确认权限不足: " + operationCode,
                                         ipAddress, userAgent, false);
                return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.INSUFFICIENT_PERMISSION);
            }

            // 记录成功的二次确认
            auditLogService.logAction(adminId, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN,
                                     adminId, "二次确认成功: " + operationCode,
                                     ipAddress, userAgent, true);

            logger.info("管理员 {} 二次确认成功, 操作: {}", admin.getUsername(), operationCode);
            return SecondaryConfirmationResult.success("二次确认成功", operation.getDescription());

        } catch (Exception e) {
            logger.error("二次确认过程发生异常: {}", e.getMessage(), e);
            return SecondaryConfirmationResult.fail(SecondaryConfirmationErrorEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public String createOperationContext(Long adminId, String operationCode, Object operationData,
                                        String ipAddress, String userAgent) {
        if (adminId == null || operationCode == null) {
            logger.warn("创建操作上下文失败: 参数为空");
            return null;
        }

        try {
            HighRiskOperation operation = HighRiskOperation.fromCode(operationCode);
            if (operation == null) {
                logger.warn("创建操作上下文失败: 操作不存在, operationCode={}", operationCode);
                return null;
            }

            String operationId = UUID.randomUUID().toString();
            OperationContext context = new OperationContext(
                operationId, adminId, operationCode, operation.getDescription(),
                operationData, ipAddress, userAgent
            );

            operationContexts.put(operationId, context);

            // 清理过期的操作上下文
            cleanExpiredContexts();

            logger.info("创建操作上下文成功: operationId={}, adminId={}, operationCode={}",
                      operationId, adminId, operationCode);
            return operationId;

        } catch (Exception e) {
            logger.error("创建操作上下文时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public OperationContext getOperationContext(String operationId) {
        if (operationId == null) {
            return null;
        }

        OperationContext context = operationContexts.get(operationId);
        if (context != null && context.isExpired()) {
            operationContexts.remove(operationId);
            logger.info("移除过期的操作上下文: {}", operationId);
            return null;
        }

        return context;
    }

    @Override
    public boolean requiresSecondaryConfirmation(String operationCode, String adminRole) {
        if (operationCode == null || adminRole == null) {
            return false;
        }

        HighRiskOperation operation = HighRiskOperation.fromCode(operationCode);
        return operation != null && HighRiskOperation.hasPermissionForOperation(operation, adminRole);
    }

    /**
     * 清理过期的操作上下文
     */
    private void cleanExpiredContexts() {
        operationContexts.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                logger.debug("清理过期操作上下文: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 生成管理员JWT令牌
     */
    private String generateAdminToken(Long adminId, String username, String role) {
        // 使用扩展的JwtUtil生成包含角色信息的令牌
        return JwtUtil.generateToken(adminId, username, role);
    }
}
