package kr.co.pinup.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import kr.co.pinup.BaseEntity;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Builder
@Table(name = "verification")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", updatable = false)
    private LocalDateTime expiresAt;
}
