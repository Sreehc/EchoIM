package com.echoim.server.common.audit;

import com.echoim.server.common.trace.TraceContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public AuditLogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void log(String action, Map<String, Object> detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", TraceContext.get());
        payload.put("action", action);
        if (detail != null && !detail.isEmpty()) {
            payload.putAll(detail);
        }
        try {
            AUDIT_LOGGER.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            AUDIT_LOGGER.info("{{\"traceId\":\"{}\",\"action\":\"{}\",\"detail\":\"serialize_failed\"}}",
                    TraceContext.get(), action);
        }
    }
}
