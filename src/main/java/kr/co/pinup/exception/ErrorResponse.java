package kr.co.pinup.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String details;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

}

