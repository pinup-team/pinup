package kr.co.pinup.members;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, "123456789", MemberRole.ROLE_USER, false);
        memberRepository.save(member);
        entityManager.flush();
    }

    @Test
    @DisplayName("회원 조회_이메일로 회원을 조회하고 삭제되지 않았는지 확인")
    void testFindByEmailAndIsDeletedFalse_ShouldReturnMember() {
        Optional<Member> foundMember = memberRepository.findByEmailAndIsDeletedFalse("test@naver.com");

        assertTrue(foundMember.isPresent());
        assertEquals("test@naver.com", foundMember.get().getEmail());
        assertFalse(foundMember.get().isDeleted());
    }

    @Test
    @DisplayName("회원 닉네임 존재 여부 확인")
    void testExistsByNickname_ShouldReturnTrue() {
        boolean exists = memberRepository.existsByNickname("네이버TestMember");

        assertTrue(exists);
    }

    @Test
    @DisplayName("회원 닉네임으로 회원 조회")
    void testFindByNickname_ShouldReturnMember() {
        Optional<Member> foundMember = memberRepository.findByNickname("네이버TestMember");

        assertTrue(foundMember.isPresent());
        assertEquals("네이버TestMember", foundMember.get().getNickname());
    }

    @Test
    @DisplayName("회원 삭제 상태 업데이트")
    @Transactional
    void testUpdateIsDeletedTrue_ShouldSetIsDeletedTrue() {
        memberRepository.updateIsDeletedTrue(member.getId());

        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 비우고 엔티티 새로 로드

        Optional<Member> updatedMember = memberRepository.findById(member.getId());

        assertTrue(updatedMember.isPresent(), "Member should exist after update.");
        assertTrue(updatedMember.get().isDeleted(), "Member's 'isDeleted' field should be true.");
    }
}