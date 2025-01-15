package kr.co.pinup.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleDto {
    private String sub;
    private String name;
    private String email;
    private String gender;
    private String birthday;
}
