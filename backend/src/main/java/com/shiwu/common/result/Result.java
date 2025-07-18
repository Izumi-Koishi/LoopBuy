package com.shiwu.common.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 统一API响应结果封装
 * @param <T> 数据类型
 */
public class Result<T> {
    private Boolean success;
    private T data;
    private ErrorInfo error;
    private String message;

    private Result() {
    }

    /**
     * 成功返回结果
     * @param data 返回的数据
     * @param <T> 数据类型
     * @return 成功的结果对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    /**
     * 成功返回结果（带消息）
     * @param data 返回的数据
     * @param message 成功消息
     * @param <T> 数据类型
     * @return 成功的结果对象
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        result.setMessage(message);
        return result;
    }

    /**
     * 错误返回结果（简化版）
     * @param errorMessage 错误信息
     * @param <T> 数据类型
     * @return 错误的结果对象
     */
    public static <T> Result<T> error(String errorMessage) {
        return fail("ERROR", errorMessage);
    }

    /**
     * 失败返回结果
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param <T> 数据类型
     * @return 失败的结果对象
     */
    public static <T> Result<T> fail(String errorCode, String errorMessage) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(errorMessage); // 设置message字段
        ErrorInfo errorInfo = new ErrorInfo(errorCode, errorMessage);
        result.setError(errorInfo);
        return result;
    }

    /**
     * 失败返回结果（带用户提示）
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param userTip 用户提示信息
     * @param <T> 数据类型
     * @return 失败的结果对象
     */
    public static <T> Result<T> fail(String errorCode, String errorMessage, String userTip) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(errorMessage); // 设置message字段
        ErrorInfo errorInfo = new ErrorInfo(errorCode, errorMessage, userTip);
        result.setError(errorInfo);
        return result;
    }

    public Boolean getSuccess() {
        return success;
    }

    // 兼容性方法
    public boolean isSuccess() {
        return success != null && success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 将结果写入HTTP响应
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    public void writeToResponse(HttpServletResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(this);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * 错误信息类
     */
    public static class ErrorInfo {
        private String code;
        private String message;
        private String userTip;

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(String code, String message, String userTip) {
            this.code = code;
            this.message = message;
            this.userTip = userTip;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUserTip() {
            return userTip;
        }

        public void setUserTip(String userTip) {
            this.userTip = userTip;
        }
    }
}