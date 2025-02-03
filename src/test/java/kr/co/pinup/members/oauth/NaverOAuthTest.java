package kr.co.pinup.members.oauth;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.MemberTestAnnotation;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthProviderNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MemberTestAnnotation
public class NaverOAuthTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuthService oAuthService;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private MemberInfo memberInfo;
    private NaverResponse naverResponse;

    private NaverLoginParams params;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("네이버TestMember")
                .providerType(OAuthProvider.NAVER)
                .providerId("123456789")
                .role(MemberRole.ROLE_USER)
                .build();
        memberInfo = MemberInfo.builder()
                .nickname("네이버TestMember")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();

        params = NaverLoginParams.builder().code("test-code").state("test-state").build();

        naverResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("testId").name("testUser").email("test@naver.com").build()).build();
    }

    @Nested
    @DisplayName("NAVER 로그인/회원가입 관련 테스트")
    class LoginMemberTests {
        private MockHttpSession session = new MockHttpSession();

        @Test
        @DisplayName("OAuth 로그인 성공")
        void login_Success() {
            when(oAuthService.request(any(), any())).thenReturn(naverResponse);
            when(memberRepository.findByEmail(any())).thenReturn(Optional.ofNullable(member));

            MemberInfo result = memberService.login(params, session);

            assertNotNull(result);
            assertEquals(member.getNickname(), result.nickname());
            assertEquals(member.getProviderType(), result.provider());
            assertEquals(member.getRole(), result.role());
            verify(oAuthService).request(any(), any());
            verify(memberRepository).findByEmail(anyString());

            SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(securityContext);
            assertEquals(memberInfo, securityContext.getAuthentication().getPrincipal());
        }

        @Test
        @DisplayName("로그인 실패_회원 정보 없음_회원가입 발생")
        void testLogin_WhenMemberNotFound_ShouldCreateNewMember() {
            when(oAuthService.request(any(), any())).thenReturn(naverResponse); // OAuth 응답 설정
            when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty()); // 회원 정보가 없을 때
            when(memberRepository.save(any(Member.class))).thenReturn(member); // 새로운 회원 저장

            MemberInfo result = memberService.login(NaverLoginParams.builder()
                    .code("test-code")
                    .state("test-state")
                    .build(), session);

            assertNotNull(result);
            assertEquals(member.getNickname(), result.nickname());
            assertEquals(member.getProviderType(), result.provider());
            assertEquals(member.getRole(), result.role());
            verify(memberRepository).findByEmail(anyString()); // 이메일로 회원 조회
            verify(memberRepository).save(any(Member.class)); // 새 회원 저장이 호출되었는지 확인
        }

        @Test
        @DisplayName("로그인 실패_OAuth 요청 실패")
        void testLogin_WhenOAuthRequestFails_ShouldThrowUnauthorizedException() {
            when(oAuthService.request(any(), any())).thenThrow(new UnauthorizedException("Invalid OAuth request"));

            assertThrows(UnauthorizedException.class, () -> {
                memberService.login(NaverLoginParams.builder()
                        .code("test-code")
                        .state("test-state")
                        .build(), session);
            });
        }
    }

    @Nested
    @DisplayName("Naver 로그아웃 관련 테스트")
    class LogOutMemberTests {
        private MockHttpServletRequest request = new MockHttpServletRequest();
        private OAuthProvider oAuthProvider = OAuthProvider.NAVER;

        @Test
        @DisplayName("로그아웃 성공")
        void testLogout_Success() {
            HttpSession session = request.getSession(true);  // 세션을 명시적으로 생성
            session.setAttribute("accessToken", "valid-access-token");
            when(oAuthService.revoke(any(), any())).thenReturn(true);

            boolean result = memberService.logout(oAuthProvider, request);

            assertTrue(result);
            verify(oAuthService).revoke(oAuthProvider, "valid-access-token");
            assertNull(request.getSession(false));  // 세션에서 accessToken 제거 확인
            assertNull(SecurityContextHolder.getContext().getAuthentication());  // 인증 정보 삭제 확인
        }

        @Test
        @DisplayName("OAuth 제공자 없음")
        void testLogout_WhenOAuthProviderNotFound_ShouldThrowOAuthProviderNotFoundException() {
            OAuthProvider invalidOAuthProvider = null;

            assertThrows(OAuthProviderNotFoundException.class, () -> {
                memberService.logout(invalidOAuthProvider, request);
            });
        }

        @Test
        @DisplayName("세션이 존재하지 않음")
        void testLogout_WhenSessionNotFound_ShouldThrowUnauthorizedException() {
            MockHttpServletRequest invalidRequest = new MockHttpServletRequest();  // 세션 없는 새 요청 객체 생성

            assertThrows(UnauthorizedException.class, () -> {
                memberService.logout(oAuthProvider, invalidRequest);
            });
        }

        @Test
        @DisplayName("accessToken 없음")
        void testLogout_WhenAccessTokenNotFound_ShouldThrowOAuthTokenNotFoundException() {
            HttpSession session = request.getSession(true);  // 세션을 명시적으로 생성
            session.setAttribute("accessToken", null);

            assertThrows(OAuthTokenNotFoundException.class, () -> {
                memberService.logout(oAuthProvider, request);
            });
        }

        @Test
        @DisplayName("OAuth 로그아웃 실패")
        void testLogout_WhenOAuthRevokeFails_ShouldThrowOAuth2AuthenticationException() {
            HttpSession session = request.getSession(true);  // 세션을 명시적으로 생성
            session.setAttribute("accessToken", "valid-access-token");
            when(oAuthService.revoke(any(), any())).thenReturn(false);

            assertThrows(OAuth2AuthenticationException.class, () -> {
                memberService.logout(oAuthProvider, request);
            });
        }
    }
}
