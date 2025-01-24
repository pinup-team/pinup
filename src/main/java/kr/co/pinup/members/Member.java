package kr.co.pinup.members;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;
    @Column(nullable = false, length = 100, unique = true)
    private String email;
    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private OAuthProvider providerType;
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @PrePersist
    public void setDefaultRole() {
        if (this.role == null) {
            this.role = MemberRole.ROLE_USER;
        }
    }
}
