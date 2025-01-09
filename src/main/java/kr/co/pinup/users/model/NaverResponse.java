package kr.co.pinup.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NaverResponse {
    private String code;
    private String state;
    private String error;
    private String error_description;
}
