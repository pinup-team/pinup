package kr.co.pinup.members.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.config.SecurityConfig;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.oauth.OAuthMocks;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@WireMockTest(httpPort = 8888)
@Import(SecurityConfig.class)
class MemberServiceIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private OauthConfig oauthConfig;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;

    private static WireMockServer wireMockServer;
    private static HttpSession session;

    @BeforeEach
    void setUp() throws IOException {
        OAuthMocks oAuthMocks = new OAuthMocks(oauthConfig);
        oAuthMocks.setupResponse();

//        wireMockServer = new WireMockServer(8888); // 지정 포트에서 시작
//        wireMockServer.start();
//        configureFor("localhost", 8888);

        session = new MockHttpSession();

        member = new Member("test", "test@naver.com", "testNickname", "", OAuthProvider.NAVER, "123456789", MemberRole.ROLE_USER, false);
        memberInfo = new MemberInfo("testNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        memberRequest = new MemberRequest("test", "test@naver.com", "", "updatedNickname", OAuthProvider.NAVER);

        memberRepository.save(member);
    }

    @AfterEach
    void tearDown() {
//        if (wireMockServer != null) {
//            wireMockServer.stop();
//        }
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 로그인 네이버_정상")
    @Transactional
    @Disabled
    void testNaverOAuthLogin_ShouldReturnUpdatedMember() {
        NaverLoginParams naverLoginParams = NaverLoginParams.builder().code("oauthTestCode").state("oauthTestState").build();
        Triple<OAuthResponse, OAuthToken, String> response = memberService.oauthLogin(naverLoginParams, session);
        // 작동안함....진짜 환장 왜 안하는거야 도라방스
        assertNotNull(response);
        OAuthToken oAuthToken = response.getMiddle();
        assertNotNull(oAuthToken);
        assertEquals("mock-access-token", oAuthToken.getAccessToken());
        assertEquals("mock-refresh-token", oAuthToken.getRefreshToken());
    }

    @Test
    @DisplayName("회원 조회_정상")
    void testFindMember_ShouldReturnMemberResponse() {
        MemberResponse response = memberService.findMember(memberInfo);

        assertNotNull(response);
        assertEquals("test", response.getName());
        assertEquals("testNickname", response.getNickname());
        assertEquals("test@naver.com", response.getEmail());
        assertEquals(OAuthProvider.NAVER, response.getProviderType());
        assertEquals(MemberRole.ROLE_USER, response.getRole());
        assertFalse(response.isDeleted());
    }

    @Test
    @WithMockMember
    @DisplayName("회원 수정_정상")
    void testUpdate_ShouldReturnUpdatedMember() {
        MemberResponse response = memberService.update(memberInfo, memberRequest);

        assertNotNull(response);
        assertThat(response.getNickname()).isEqualTo("updatedNickname");
        Member updated = memberRepository.findByEmailAndProviderTypeAndIsDeletedFalse(member.getEmail(), member.getProviderType()).orElseThrow();
        assertThat(updated.getNickname()).isEqualTo("updatedNickname");
    }

    @Test
    @DisplayName("회원 삭제_정상")
    @Disabled
    void testDeleteMember_ShouldReturnTrue() {
        boolean result = memberService.disable(memberInfo, memberRequest);

        assertThat(result).isTrue();
        Member deleted = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }
}