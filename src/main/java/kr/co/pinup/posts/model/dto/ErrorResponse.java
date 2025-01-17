package kr.co.pinup.posts.model.dto;

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
    private String details;  // 추가 정보 (선택적)

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    // Getters and Setters
}

