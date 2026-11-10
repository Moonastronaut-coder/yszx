package com.yunshu.zhixue.platform.web;

import java.util.List;
import java.util.Map;
import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message, T data, String request_id, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data, MDC.get("request_id"), null);
    }

    public static ApiResponse<Void> failure(String code, String message, List<FieldError> fields) {
        return new ApiResponse<>(code, message, null, MDC.get("request_id"), new ApiError(fields, Map.of()));
    }

    public record ApiError(List<FieldError> field_errors, Map<String, Object> details) {}
    public record FieldError(String field, String code, String message) {}
}
