package kr.co.pinup.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenReponse {
    private String access_token;
    private String token_type;
    private String expires_in;
    private String refresh_token;
    private String error;
    private String error_description;
}
