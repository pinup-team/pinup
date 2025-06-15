package kr.co.pinup.custom.logging.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarnLog extends BaseLog {
    private String message;
    private String warningType;
    private String reason;
    private String invalidField;
    private String externalApi;

    public WarnLog(String message) {
        super("WARN");
        this.message = message;
    }

}
