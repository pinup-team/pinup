package kr.co.pinup.custom.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.custom.logging.model.dto.BaseLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class StructuredLogger {

    private final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private Map<String, Object> getBaseFields(String level) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("logLevel", level);
        fields.put("timestamp", Instant.now().toString());
        fields.put("userNickName", MDC.get("userNickName"));
        fields.put("requestId", MDC.get("requestId"));
        fields.put("className", MDC.get("className"));
        fields.put("methodName", MDC.get("methodName"));
        fields.put("targetId", MDC.get("targetId"));

        String status = MDC.get("status");
        if (status != null) {
            fields.put("status", status);
        }
        return fields;
    }

    public void info(String message, Map<String, Object> additionalFields) {
        Map<String, Object> log = getBaseFields("INFO");
        log.put("message", message);
        if (additionalFields != null) log.putAll(additionalFields);
        logger.info(toJson(log));
    }

    public void warn(String message, Map<String, Object> additional) {
        Map<String, Object> log = getBaseFields("WARN");
        log.put("message", message);
        if (additional != null) log.putAll(additional);
        logger.warn(toJson(log));
    }

    public void error(String message, Throwable e, Map<String, Object> additional) {
        Map<String, Object> log = getBaseFields("ERROR");
        log.put("message", message);
        log.put("exceptionType", e.getClass().getSimpleName());
        log.put("stackTrace", getStackTrace(e));
        if (additional != null) log.putAll(additional);
        logger.error(toJson(log));
    }

    private String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append(el.toString()).append("\n");
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public void log(BaseLog payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            switch (payload.getLogLevel()) {
                case "INFO" -> logger.info(json);
                case "WARN" -> logger.warn(json);
                case "ERROR" -> logger.error(json);
                default -> logger.info(json);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log payload", e);
        }
    }
}