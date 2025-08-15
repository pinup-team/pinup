package kr.co.pinup.verification;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.verification.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class VerificationRepositoryTest {

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private EntityManager entityManager;

    private Verification verification;

    @BeforeEach
    void setUp() {
        verification = new Verification("test@pinup.com", "12345", LocalDateTime.now().plusMinutes(5));
        verificationRepository.save(verification);
        entityManager.flush();
    }

    @Test
    @DisplayName("이메일로 Verification 조회 테스트")
    void findByEmailTest() {
        Optional<Verification> found = verificationRepository.findByEmail("test@pinup.com");
        assertTrue(found.isPresent());
        assertEquals("12345", found.get().getCode());
    }

    @Test
    @DisplayName("이메일로 Verification 삭제 테스트")
    void deleteByEmailTest() {
        verificationRepository.deleteByEmail("test@pinup.com");
        entityManager.flush();

        Optional<Verification> found = verificationRepository.findByEmail("test@pinup.com");
        assertFalse(found.isPresent());
    }
}