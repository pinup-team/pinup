package kr.co.pinup.users.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByProviderId(String providerId);
    UserEntity findByProviderId(String providerId);

    Optional<UserEntity> findByEmail(String email);
}
