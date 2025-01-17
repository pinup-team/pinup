package kr.co.pinup.users.model;

import jakarta.persistence.*;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.users.model.enums.UserRole;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private UserRole role;

    @PrePersist
    public void setDefaultRole() {
        if (this.role == null) {
            this.role = UserRole.ROLE_USER;
        }
    }
}
