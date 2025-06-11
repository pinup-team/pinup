package kr.co.pinup.custom.logging.model.dto;

import lombok.Getter;
import org.jboss.logging.MDC;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class BaseLog {
    protected String timestamp;
    protected String logLevel;
    protected String className;
    protected String methodName;
    protected String requestId;
    protected String targetId;
    protected String status;
    protected String userNickName;

    private final Map<String, Object> details = new HashMap<>();

    public BaseLog(String logLevel) {
        this.logLevel = logLevel;
        this.timestamp = String.valueOf(MDC.get("timestamp"));
        this.requestId = String.valueOf(MDC.get("requestId"));
        this.className = String.valueOf(MDC.get("className"));
        this.methodName = String.valueOf(MDC.get("methodName"));
        this.status = String.valueOf(MDC.get("status"));
        this.targetId = String.valueOf(MDC.get("targetId"));
        this.userNickName = String.valueOf(MDC.get("userNickName"));
    }

    public BaseLog setStatus(String status) {
        String normalized = normalizeNull(status);
        this.status = normalized;
        MDC.put("status", normalized);
        return this;
    }

    public BaseLog setTargetId(String targetId) {
        String normalized = normalizeNull(targetId);
        this.targetId = normalized;
        MDC.put("targetId", normalized);
        return this;
    }

    private String normalizeNull(String value) {
        return (value == null || "null".equals(value)) ? null : value;
    }

    public BaseLog addDetails(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("addDetails requires even number of arguments (key-value pairs).");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            this.details.put(keyValues[i], keyValues[i + 1]);
        }
        return this;
    }

}
