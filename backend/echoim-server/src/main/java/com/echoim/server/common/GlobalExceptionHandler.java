package com.echoim.server.common;

import com.echoim.server.common.exception.BizException;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.trace.TraceContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Long currentUserId() {
        return LoginUserContext.get() == null ? null : LoginUserContext.get().getUserId();
    }

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex, HttpServletRequest request) {
        log.warn("traceId={} path={} userId={} code={} message={}",
                TraceContext.get(), request.getRequestURI(), currentUserId(), ex.getCode(), ex.getMessage(), ex);
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("traceId={} path={} userId={} code={} message={}",
                TraceContext.get(), request.getRequestURI(), currentUserId(), 40000, message, ex);
        return ApiResponse.fail(40000, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("traceId={} path={} userId={} code={} message={}",
                TraceContext.get(), request.getRequestURI(), currentUserId(), 40000, ex.getMessage(), ex);
        return ApiResponse.fail(40000, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ApiResponse<Void> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.error("traceId={} path={} userId={} code={} message={}",
                TraceContext.get(), request.getRequestURI(), currentUserId(), 50000, ex.getMessage(), ex);
        return ApiResponse.fail(50000, "服务暂时不可用，请稍后再试");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("traceId={} path={} userId={} code={} message={}",
                TraceContext.get(), request.getRequestURI(), currentUserId(), 50000, ex.getMessage(), ex);
        return ApiResponse.fail(50000, "服务暂时不可用，请稍后再试");
    }
}
