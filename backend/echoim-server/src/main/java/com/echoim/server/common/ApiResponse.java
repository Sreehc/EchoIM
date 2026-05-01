package com.echoim.server.common;

import com.echoim.server.common.trace.TraceContext;

public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final String requestId;
    private final String traceId;

    private ApiResponse(int code, String message, T data, String requestId, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, null, TraceContext.get());
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null, null, TraceContext.get());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null, TraceContext.get());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }
}
