package kr.co.pinup.users.model;

import kr.co.pinup.users.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String name;
    private String email;
    private UserRole role;
}
