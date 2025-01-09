package kr.co.pinup.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequest {
    private String grant_type;
    private String client_id;
    private String client_secret;
    private String code;
    private String state;
}
