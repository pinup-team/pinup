package kr.co.pinup.verification.repository;

import kr.co.pinup.verification.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByEmail(String email);

    void deleteByEmail(String email);
}
