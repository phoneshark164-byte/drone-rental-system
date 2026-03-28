package com.drone.rental.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 */
@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResult(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "操作成功");
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data);
    }

    public static <T> ApiResult<T> fail() {
        return new ApiResult<>(500, "操作失败");
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message);
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message);
    }

    public static <T> ApiResult<T> unauthorized() {
        return new ApiResult<>(401, "未授权，请先登录");
    }

    public static <T> ApiResult<T> forbidden() {
        return new ApiResult<>(403, "无权限访问");
    }

    public static <T> ApiResult<T> notFound() {
        return new ApiResult<>(404, "资源不存在");
    }
}
