package com.echoim.server.service.admin;

import com.echoim.server.entity.SysOperationLogEntity;
import com.echoim.server.mapper.SysOperationLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AdminOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminOperationLogService(SysOperationLogMapper sysOperationLogMapper, ObjectMapper objectMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    public void log(Long adminUserId,
                    String moduleName,
                    String actionName,
                    String targetType,
                    Long targetId,
                    Map<String, Object> content) {
        SysOperationLogEntity entity = new SysOperationLogEntity();
        entity.setAdminUserId(adminUserId == null ? 0L : adminUserId);
        entity.setModuleName(moduleName);
        entity.setActionName(actionName);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setRequestIp(resolveRequestIp());
        entity.setContentJson(toJson(content));
        entity.setCreatedAt(LocalDateTime.now());
        sysOperationLogMapper.insert(entity);
    }

    private String resolveRequestIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            return "{\"serialize\":\"failed\"}";
        }
    }
}
