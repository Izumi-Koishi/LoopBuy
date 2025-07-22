package com.shiwu.admin.service.impl;

import com.shiwu.admin.dao.AdminDao;
import com.shiwu.admin.dao.AuditLogDao;
import com.shiwu.admin.dto.AuditLogQueryDTO;
import com.shiwu.admin.enums.AuditActionEnum;
import com.shiwu.admin.enums.AuditTargetTypeEnum;
import com.shiwu.admin.model.Administrator;
import com.shiwu.admin.model.AuditLog;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.admin.vo.AuditLogVO;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;

import java.util.*;

/**
 * 审计日志服务实现 - MVC框架版本
 * 实现NFR-SEC-03要求的审计日志功能
 */
@Service
public class AuditLogServiceImpl extends BaseService implements AuditLogService {

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private AdminDao adminDao;

    // 无参构造函数，用于MVC框架依赖注入
    public AuditLogServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.auditLogDao = new AuditLogDao();
        this.adminDao = new AdminDao();
        logger.info("AuditLogServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public AuditLogServiceImpl(AuditLogDao auditLogDao, AdminDao adminDao) {
        this.auditLogDao = auditLogDao;
        this.adminDao = adminDao;
        logger.info("AuditLogServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public Long logAction(Long adminId, AuditActionEnum action, AuditTargetTypeEnum targetType, 
                         Long targetId, String details, String ipAddress, String userAgent, boolean success) {
        if (adminId == null || action == null) {
            logger.warn("记录审计日志失败: 必要参数为空");
            return null;
        }
        
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAdminId(adminId);
            auditLog.setAction(action.getCode());
            auditLog.setTargetType(targetType != null ? targetType.getCode() : null);
            auditLog.setTargetId(targetId);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setResult(success ? 1 : 0);
            
            Long logId = auditLogDao.createAuditLog(auditLog);
            if (logId != null) {
                logger.info("审计日志记录成功: adminId={}, action={}, success={}", 
                          adminId, action.getCode(), success);
            }
            return logId;
        } catch (Exception e) {
            logger.error("记录审计日志失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Long logAction(Long adminId, AuditActionEnum action, String details, boolean success) {
        return logAction(adminId, action, null, null, details, null, null, success);
    }
    
    @Override
    public Long logAdminLogin(Long adminId, String ipAddress, String userAgent, boolean success, String details) {
        return logAction(adminId, AuditActionEnum.ADMIN_LOGIN, AuditTargetTypeEnum.ADMIN, 
                        adminId, details, ipAddress, userAgent, success);
    }
    
    @Override
    public Map<String, Object> getAuditLogs(AuditLogQueryDTO queryDTO) {
        if (queryDTO == null) {
            logger.warn("查询审计日志失败: 查询条件为空");
            return createEmptyResult();
        }
        
        try {
            // 查询日志列表
            List<AuditLog> auditLogs = auditLogDao.findAuditLogs(queryDTO);
            
            // 查询总数
            long totalCount = auditLogDao.countAuditLogs(queryDTO);
            
            // 转换为VO并填充管理员信息
            List<AuditLogVO> auditLogVOs = new ArrayList<>();
            for (AuditLog auditLog : auditLogs) {
                AuditLogVO vo = AuditLogVO.fromEntity(auditLog);
                
                // 填充管理员用户名
                if (auditLog.getAdminId() != null) {
                    Administrator admin = adminDao.findById(auditLog.getAdminId());
                    if (admin != null) {
                        vo.setAdminUsername(admin.getUsername());
                    }
                }
                
                // 填充操作描述
                AuditActionEnum actionEnum = AuditActionEnum.fromCode(auditLog.getAction());
                if (actionEnum != null) {
                    vo.setActionDescription(actionEnum.getDescription());
                }
                
                // 填充目标类型描述
                AuditTargetTypeEnum targetTypeEnum = AuditTargetTypeEnum.fromCode(auditLog.getTargetType());
                if (targetTypeEnum != null) {
                    vo.setTargetTypeDescription(targetTypeEnum.getDescription());
                }
                
                auditLogVOs.add(vo);
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", auditLogVOs);
            result.put("totalCount", totalCount);
            result.put("page", queryDTO.getPage());
            result.put("pageSize", queryDTO.getPageSize());
            result.put("totalPages", (totalCount + queryDTO.getPageSize() - 1) / queryDTO.getPageSize());
            
            logger.info("查询审计日志成功: 共{}条记录", totalCount);
            return result;
        } catch (Exception e) {
            logger.error("查询审计日志失败: {}", e.getMessage(), e);
            return createEmptyResult();
        }
    }
    
    @Override
    public AuditLogVO getAuditLogDetail(Long id) {
        if (id == null) {
            logger.warn("获取审计日志详情失败: ID为空");
            return null;
        }
        
        try {
            AuditLog auditLog = auditLogDao.findById(id);
            if (auditLog == null) {
                logger.warn("审计日志不存在: ID={}", id);
                return null;
            }
            
            AuditLogVO vo = AuditLogVO.fromEntity(auditLog);
            
            // 填充管理员信息
            if (auditLog.getAdminId() != null) {
                Administrator admin = adminDao.findById(auditLog.getAdminId());
                if (admin != null) {
                    vo.setAdminUsername(admin.getUsername());
                }
            }
            
            // 填充操作描述
            AuditActionEnum actionEnum = AuditActionEnum.fromCode(auditLog.getAction());
            if (actionEnum != null) {
                vo.setActionDescription(actionEnum.getDescription());
            }
            
            // 填充目标类型描述
            AuditTargetTypeEnum targetTypeEnum = AuditTargetTypeEnum.fromCode(auditLog.getTargetType());
            if (targetTypeEnum != null) {
                vo.setTargetTypeDescription(targetTypeEnum.getDescription());
            }
            
            logger.info("获取审计日志详情成功: ID={}", id);
            return vo;
        } catch (Exception e) {
            logger.error("获取审计日志详情失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Map<String, Object> getOperationStats(int days) {
        try {
            return auditLogDao.getOperationStats(days);
        } catch (Exception e) {
            logger.error("获取操作统计数据失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> getActivityTrend(int days) {
        try {
            return auditLogDao.getActivityTrend(days);
        } catch (Exception e) {
            logger.error("获取活动趋势数据失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<AuditLogVO> exportAuditLogs(AuditLogQueryDTO queryDTO) {
        if (queryDTO == null) {
            logger.warn("导出审计日志失败: 查询条件为空");
            return new ArrayList<>();
        }
        
        try {
            // 设置较大的页面大小用于导出
            queryDTO.setPageSize(10000);
            queryDTO.setPage(1);
            
            List<AuditLog> auditLogs = auditLogDao.findAuditLogs(queryDTO);
            List<AuditLogVO> auditLogVOs = new ArrayList<>();
            
            for (AuditLog auditLog : auditLogs) {
                AuditLogVO vo = AuditLogVO.fromEntity(auditLog);
                
                // 填充管理员用户名
                if (auditLog.getAdminId() != null) {
                    Administrator admin = adminDao.findById(auditLog.getAdminId());
                    if (admin != null) {
                        vo.setAdminUsername(admin.getUsername());
                    }
                }
                
                // 填充操作描述
                AuditActionEnum actionEnum = AuditActionEnum.fromCode(auditLog.getAction());
                if (actionEnum != null) {
                    vo.setActionDescription(actionEnum.getDescription());
                }
                
                // 填充目标类型描述
                AuditTargetTypeEnum targetTypeEnum = AuditTargetTypeEnum.fromCode(auditLog.getTargetType());
                if (targetTypeEnum != null) {
                    vo.setTargetTypeDescription(targetTypeEnum.getDescription());
                }
                
                auditLogVOs.add(vo);
            }
            
            logger.info("导出审计日志成功: 共{}条记录", auditLogVOs.size());
            return auditLogVOs;
        } catch (Exception e) {
            logger.error("导出审计日志失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean shouldLogAction(AuditActionEnum action) {
        // 根据NFR-SEC-03，所有敏感操作都需要记录审计日志
        return action != null && action.isSensitiveOperation();
    }
    
    @Override
    public List<Map<String, String>> getAvailableActions() {
        List<Map<String, String>> actions = new ArrayList<>();
        for (AuditActionEnum action : AuditActionEnum.values()) {
            Map<String, String> actionMap = new HashMap<>();
            actionMap.put("code", action.getCode());
            actionMap.put("description", action.getDescription());
            actions.add(actionMap);
        }
        return actions;
    }
    
    @Override
    public List<Map<String, String>> getAvailableTargetTypes() {
        List<Map<String, String>> targetTypes = new ArrayList<>();
        for (AuditTargetTypeEnum targetType : AuditTargetTypeEnum.values()) {
            Map<String, String> targetTypeMap = new HashMap<>();
            targetTypeMap.put("code", targetType.getCode());
            targetTypeMap.put("description", targetType.getDescription());
            targetTypes.add(targetTypeMap);
        }
        return targetTypes;
    }
    
    /**
     * 创建空的查询结果
     * @return 空结果
     */
    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("totalCount", 0L);
        result.put("page", 1);
        result.put("pageSize", 20);
        result.put("totalPages", 0L);
        return result;
    }
}
