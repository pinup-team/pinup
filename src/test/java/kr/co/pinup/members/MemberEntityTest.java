package kr.co.pinup.members;

import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MemberEntityTest {

    @Autowired
    MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("네이버TestMember")
                .providerType(OAuthProvider.NAVER)
                .providerId("123456789")
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Member 빌더를 통해 정상적으로 객체를 생성할 수 있다.")
    void memberBuilder_정상생성() {
        assertThat(member.getName()).isEqualTo("test");
        assertThat(member.getEmail()).isEqualTo("test@naver.com");
        assertThat(member.getNickname()).isEqualTo("네이버TestMember");
        assertThat(member.getProviderType()).isEqualTo(OAuthProvider.NAVER);
        assertThat(member.getProviderId()).isEqualTo("123456789");
        assertThat(member.getRole()).isEqualTo(MemberRole.ROLE_USER);
        assertThat(member.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("disableMember()를 호출하면 isDeleted가 true가 된다.")
    void disableMember_삭제처리() {
        member.disableMember();

        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("@PrePersist 없이 role이 null인 경우 수동 설정 테스트")
    void setDefaultRole_수동호출() {
        member.setDefaultRole();

        assertThat(member.getRole()).isEqualTo(MemberRole.ROLE_USER);
    }
}
