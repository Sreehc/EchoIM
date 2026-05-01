package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
public class ErrorReportController {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportController.class);

    @PostMapping("/report")
    public ApiResponse<Void> report(@RequestBody Map<String, Object> body) {
        Object errors = body.get("errors");
        if (errors instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> error) {
                    log.warn("FrontendError type={} message={} traceId={} component={} url={} userAgent={}",
                            error.get("type"),
                            error.get("message"),
                            error.get("traceId"),
                            error.get("component"),
                            error.get("url"),
                            error.get("userAgent"));
                }
            }
        }
        return ApiResponse.success();
    }
}
