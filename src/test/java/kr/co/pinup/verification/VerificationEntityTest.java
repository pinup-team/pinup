package kr.co.pinup.verification;

import kr.co.pinup.verification.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class VerificationEntityTest {

    @Autowired
    VerificationRepository verificationRepository;

    private Verification verification;

    @BeforeEach
    void setUp() {
        LocalDateTime expires = LocalDateTime.now().plusMinutes(5);
        verification = Verification.builder()
                .email("test@pinup.com")
                .code("핀업TestMember")
                .expiresAt(expires)
                .build();
    }

    @Test
    @DisplayName("Verification 빌더를 통해 정상적으로 객체를 생성할 수 있다.")
    void verificationBuilder_정상생성() {
        assertThat(verification.getEmail()).isEqualTo("test@pinup.com");
        assertThat(verification.getCode()).isEqualTo("핀업TestMember");
        assertThat(verification.getExpiresAt()).isNotNull();
        assertThat(verification.getExpiresAt()).isAfter(LocalDateTime.now());
    }
}
