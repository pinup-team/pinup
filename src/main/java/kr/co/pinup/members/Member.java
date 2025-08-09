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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;
    @Column(nullable = false, length = 100)
    private String email;
    @Column(nullable = false, length = 50, unique = true)
    private String nickname;
    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private OAuthProvider providerType;
    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean isDeleted;

    @PrePersist
    public void setDefaultRole() {
        if (this.role == null) {
            this.role = MemberRole.ROLE_USER;
        }
    }

    public void disableMember() {
        this.isDeleted = true;
    }
}
