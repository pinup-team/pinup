package kr.co.pinup.members.repository;

import kr.co.pinup.members.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndIsDeletedFalse(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);

    @Modifying
    @Query("UPDATE Member m SET m.isDeleted = true WHERE m.id = :memberId")
    void updateIsDeletedTrue(@Param("memberId") Long memberId);
}
