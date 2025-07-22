package com.shiwu.framework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MVC框架基础服务类
 * 
 * 提供Service层的通用功能，包括参数验证、日志记录、异常处理等
 * 所有业务Service都应该继承此类或实现相应的功能
 * 
 * 核心功能：
 * 1. 统一的参数验证方法
 * 2. 统一的日志记录格式
 * 3. 统一的异常处理机制
 * 4. 通用的数据处理方法
 * 5. 分页结果构建方法
 */
public abstract class BaseService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // ==================== 参数验证方法 ====================
    
    /**
     * 验证参数不为空
     * 
     * @param value 要验证的值
     * @param paramName 参数名称
     * @throws IllegalArgumentException 如果参数为空
     */
    protected void validateNotNull(Object value, String paramName) {
        if (value == null) {
            String message = String.format("参数 %s 不能为空", paramName);
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 验证字符串参数不为空且不为空白
     * 
     * @param value 要验证的字符串
     * @param paramName 参数名称
     * @throws IllegalArgumentException 如果参数为空或空白
     */
    protected void validateNotBlank(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            String message = String.format("参数 %s 不能为空或空白", paramName);
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 验证数值参数为正数
     * 
     * @param value 要验证的数值
     * @param paramName 参数名称
     * @throws IllegalArgumentException 如果参数不是正数
     */
    protected void validatePositive(Number value, String paramName) {
        validateNotNull(value, paramName);
        if (value.doubleValue() <= 0) {
            String message = String.format("参数 %s 必须为正数", paramName);
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 验证ID参数有效性
     * 
     * @param id 要验证的ID
     * @param paramName 参数名称
     * @throws IllegalArgumentException 如果ID无效
     */
    protected void validateId(Long id, String paramName) {
        validateNotNull(id, paramName);
        if (id <= 0) {
            String message = String.format("参数 %s 必须为有效的正整数ID", paramName);
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 验证分页参数
     * 
     * @param page 页码
     * @param size 页面大小
     * @throws IllegalArgumentException 如果分页参数无效
     */
    protected void validatePagination(Integer page, Integer size) {
        if (page != null && page < 1) {
            String message = "页码必须大于0";
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
        if (size != null && (size < 1 || size > 100)) {
            String message = "页面大小必须在1-100之间";
            logger.warn("参数验证失败: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
    
    // ==================== 日志记录方法 ====================
    
    /**
     * 记录方法开始执行的日志
     * 
     * @param methodName 方法名
     * @param params 参数
     */
    protected void logMethodStart(String methodName, Object... params) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始执行方法: {}, 参数: {}", methodName, formatParams(params));
        }
    }
    
    /**
     * 记录方法执行成功的日志
     * 
     * @param methodName 方法名
     * @param result 结果
     */
    protected void logMethodSuccess(String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            logger.debug("方法执行成功: {}, 结果类型: {}", methodName, 
                result != null ? result.getClass().getSimpleName() : "null");
        }
    }
    
    /**
     * 记录方法执行失败的日志
     * 
     * @param methodName 方法名
     * @param error 异常
     * @param params 参数
     */
    protected void logMethodError(String methodName, Throwable error, Object... params) {
        logger.error("方法执行失败: {}, 参数: {}, 错误: {}", 
            methodName, formatParams(params), error.getMessage(), error);
    }
    
    /**
     * 记录业务警告日志
     * 
     * @param message 警告信息
     * @param params 参数
     */
    protected void logBusinessWarning(String message, Object... params) {
        logger.warn("业务警告: " + message, params);
    }
    
    /**
     * 记录业务信息日志
     * 
     * @param message 信息
     * @param params 参数
     */
    protected void logBusinessInfo(String message, Object... params) {
        logger.info("业务信息: " + message, params);
    }
    
    // ==================== 异常处理方法 ====================
    
    /**
     * 处理并包装异常
     * 
     * @param e 原始异常
     * @param methodName 方法名
     * @param message 自定义错误信息
     * @return 包装后的运行时异常
     */
    protected RuntimeException handleException(Exception e, String methodName, String message) {
        logMethodError(methodName, e);
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(message, e);
    }
    
    /**
     * 安全执行方法，捕获异常并返回默认值
     * 
     * @param supplier 要执行的方法
     * @param defaultValue 默认值
     * @param methodName 方法名
     * @param <T> 返回值类型
     * @return 执行结果或默认值
     */
    protected <T> T safeExecute(java.util.function.Supplier<T> supplier, T defaultValue, String methodName) {
        try {
            return supplier.get();
        } catch (Exception e) {
            logMethodError(methodName, e);
            return defaultValue;
        }
    }
    
    // ==================== 数据处理方法 ====================
    
    /**
     * 创建空的分页结果
     * 
     * @return 空的分页结果Map
     */
    protected Map<String, Object> createEmptyPageResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", new ArrayList<>());
        result.put("total", 0L);
        result.put("page", 1);
        result.put("size", 10);
        result.put("totalPages", 0);
        return result;
    }
    
    /**
     * 创建分页结果
     * 
     * @param data 数据列表
     * @param total 总数
     * @param page 当前页
     * @param size 页面大小
     * @return 分页结果Map
     */
    protected Map<String, Object> createPageResult(List<?> data, long total, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", data != null ? data : new ArrayList<>());
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }
    
    /**
     * 检查列表是否为空
     * 
     * @param list 要检查的列表
     * @return 如果列表为空或null返回true
     */
    protected boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
    
    /**
     * 获取安全的列表大小
     * 
     * @param list 列表
     * @return 列表大小，如果列表为null返回0
     */
    protected int safeSize(List<?> list) {
        return list != null ? list.size() : 0;
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 格式化参数用于日志输出
     * 
     * @param params 参数数组
     * @return 格式化后的字符串
     */
    private String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "无参数";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object param = params[i];
            if (param == null) {
                sb.append("null");
            } else if (param instanceof String) {
                sb.append("\"").append(param).append("\"");
            } else {
                sb.append(param.toString());
            }
        }
        return sb.toString();
    }
}
