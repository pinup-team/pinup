package kr.co.pinup.members.repository;

import kr.co.pinup.members.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);
}
