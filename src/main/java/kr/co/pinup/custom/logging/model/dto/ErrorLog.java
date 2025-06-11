package kr.co.pinup.custom.logging.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@Setter
public class ErrorLog extends BaseLog {
    private String message;
    private String exceptionType;
    private String stackTrace;
    private String query;
    private Long responseTime;


    public ErrorLog(String message, Exception ex) {
        super("ERROR");
        this.message = message;
        if (ex != null) {
            this.exceptionType = ex.getClass().getSimpleName();
            this.stackTrace = summarizeStackTrace(ex);
        }
    }

    private String summarizeStackTrace(Throwable ex) {
        return ex.toString() + "\n" +
                Arrays.stream(ex.getStackTrace())
                        .limit(3)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
    }

}
