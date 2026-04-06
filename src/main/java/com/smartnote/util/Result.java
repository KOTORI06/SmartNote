package com.smartnote.util;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

//统一API响应结果封装
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;//响应状态码：200-成功，其他-失败
    private String message;//响应消息
    private T data;//响应数据

    //成功响应（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    //成功响应（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    //成功响应（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    //失败响应
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    //失败响应（自定义状态码）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    //参数校验失败
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    //未授权
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    //禁止访问
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    //资源不存在
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
}
