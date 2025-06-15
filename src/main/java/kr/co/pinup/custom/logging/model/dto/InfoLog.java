package kr.co.pinup.custom.logging.model.dto;

import lombok.Getter;

@Getter
public class InfoLog extends BaseLog {
    private String message;

    public InfoLog(String message) {
        super("INFO");
        this.message = message;
    }

}