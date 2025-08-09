package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.*;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.google.GoogleResponse;
import kr.co.pinup.oauth.google.GoogleToken;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverResponse;
import kr.co.pinup.oauth.naver.NaverToken;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfigTest.class, LoggerConfig.class})
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    private MemberService memberService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("회원가입")
    class RegisterMemberTests {

        @Test
        @DisplayName("회원가입 - 성공")
        void testRegisterSuccess() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@test.com")
                    .nickname("testNickname")
                    .providerType(OAuthProvider.PINUP)
                    .password("test1234!")
                    .build();

            Member member = Member.builder()
                    .name("test")
                    .email("test@test.com")
                    .nickname("testNickname")
                    .password("test1234!")
                    .providerType(OAuthProvider.PINUP)
                    .role(MemberRole.ROLE_USER)
                    .isDeleted(false)
                    .build();

            when(memberService.register(any(MemberRequest.class))).thenReturn(Pair.of(member, "가입완료"));

            mockMvc.perform(post("/api/members/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("회원가입이 완료되었습니다."));
        }

        @Test
        @DisplayName("회원가입 - 중복(이메일 등)")
        void testRegisterConflict() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("duplicate@test.com")
                    .nickname("dupNickname")
                    .providerType(OAuthProvider.PINUP)
                    .password("test1234!")
                    .build();

            when(memberService.register(any(MemberRequest.class))).thenReturn(Pair.of(null, null));

            mockMvc.perform(post("/api/members/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(content().string("회원가입에 실패했습니다.\n이미 존재하는 이메일일 수 있습니다."));
        }

        @Test
        @DisplayName("회원가입 - 비즈니스 예외 발생")
        void testRegisterBadRequestException() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("bad@test.com")
                    .nickname("badRequest")
                    .providerType(OAuthProvider.PINUP)
                    .password("test1234!")
                    .build();

            when(memberService.register(any(MemberRequest.class)))
                    .thenThrow(new MemberBadRequestException("중복된 닉네임입니다."));

            mockMvc.perform(post("/api/members/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("회원가입에 실패했습니다.\n중복된 닉네임입니다."));
        }

        @Test
        @DisplayName("회원가입 - 시스템 예외 발생")
        void testRegisterServerError() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("fail@test.com")
                    .nickname("failRequest")
                    .providerType(OAuthProvider.PINUP)
                    .password("test1234!")
                    .build();

            when(memberService.register(any(MemberRequest.class)))
                    .thenThrow(new RuntimeException("DB 연결 오류"));

            mockMvc.perform(post("/api/members/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("서버 오류로 인해 회원가입에 실패했습니다."));
        }
    }

    @Nested
    @DisplayName("회원 이메일 중복 확인")
    class ValidateEmailTests {

        @Test
        @DisplayName("이메일 중복 확인 - 사용 가능")
        void testValidateEmailAvailable() throws Exception {
            String email = "available@test.com";

            when(memberService.validateEmail(email)).thenReturn(true);

            mockMvc.perform(get("/api/members/validate")
                            .param("email", email))
                    .andExpect(status().isOk())
                    .andExpect(content().string("가입 가능한 이메일입니다."));
        }

        @Test
        @DisplayName("이메일 중복 확인 - 이미 사용 중")
        void testValidateEmailConflict() throws Exception {
            String email = "used@test.com";

            when(memberService.validateEmail(email)).thenReturn(false);

            mockMvc.perform(get("/api/members/validate")
                            .param("email", email))
                    .andExpect(status().isConflict())
                    .andExpect(content().string("이미 가입된 이메일입니다."));
        }
    }

    @Nested
    @DisplayName("회원 로그인")
    class MemberLoginTests {
        @Test
        @DisplayName("네이버 OAuth 로그인 성공")
        void testLoginNaverSuccess() throws Exception {
            NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
            NaverResponse oAuthResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("123456789").name("test").email("test@naver.com").build()).build(); // Mock or populate with necessary data
            NaverToken oAuthToken = NaverToken.builder().accessToken("access_token").refreshToken("refresh_token").build(); // Missing refresh token

            when(memberService.oauthLogin(any(NaverLoginParams.class), any(HttpSession.class)))
                    .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

            mockMvc.perform(get("/api/members/oauth/naver")
                            .param("code", "auth_code")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "/"));
        }

        @Test
        @DisplayName("구글 OAuth 로그인 성공")
        void testLoginGoogleSuccess() throws Exception {
            GoogleLoginParams params = GoogleLoginParams.builder().code("auth_code").state("1234567890").build();
            GoogleResponse oAuthResponse = GoogleResponse.builder().sub("987654321").name("test").email("test@gmail.com").build();
            GoogleToken oAuthToken = GoogleToken.builder().accessToken("access_token").refreshToken("refresh_token").build();

            when(memberService.oauthLogin(any(GoogleLoginParams.class), any(HttpSession.class)))
                    .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

            mockMvc.perform(get("/api/members/oauth/google")
                            .param("code", "auth_code")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "/"));
        }

        @Test
        @DisplayName("핀업 자체 로그인 성공")
        void testPinupLoginSuccess() throws Exception {
            Member member = Member.builder().name("test").email("test@test.com").password(passwordEncoder.encode("test1234!")).nickname("pinup 자체유저").providerType(OAuthProvider.PINUP).build();

            when(memberService.login(any(MemberLoginRequest.class), any(HttpSession.class)))
                    .thenReturn(Pair.of(member, "다시 돌아오신 걸 환영합니다 \"" + member.getName() + "\"님"));

            String loginRequestJson = """
                        {
                            "email": "test@test.com",
                            "password": "test1234!",
                            "providerType": "PINUP"
                        }
                    """;

            mockMvc.perform(post("/api/members/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequestJson))
                    .andDo(print())
                    .andExpect(status().isOk()) // 200 OK 예상
                    .andExpect(content().string("다시 돌아오신 걸 환영합니다 \"test\"님"));
        }
    }

    @Test
    @WithMockMember
    @DisplayName("회원 닉네임 추천")
    void testMakeNicknameByRefererConditions() throws Exception {
        String generatedNickname = "generate-nickname";
        when(memberService.makeNickname()).thenReturn(generatedNickname);

        // 로그인 필요
        mockMvc.perform(get("/api/members/nickname")
                        .header("Referer", "/members/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(generatedNickname));

        // 비로그인 가능
        mockMvc.perform(get("/api/members/nickname")
                        .header("Referer", "/members/register"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(generatedNickname));

        // unknown → 403
        mockMvc.perform(get("/api/members/nickname")
                        .header("Referer", "/unknown"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("허용되지 않은 요청입니다."));

        // no referer → 403
        mockMvc.perform(get("/api/members/nickname"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("허용되지 않은 요청입니다."));
    }

    @Nested
    @DisplayName("회원 정보 수정")
    class UpdateMemberTests {

        @Test
        @WithMockMember
        @DisplayName("성공 - 닉네임 변경 성공")
        void testUpdateMemberSuccess() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@naver.com")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.NAVER)
                    .build();

            MemberResponse updatedMemberResponse = MemberResponse.builder()
                    .id(1L)
                    .name("test")
                    .email("test@naver.com")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(memberService.update(any(MemberInfo.class), any(MemberRequest.class)))
                    .thenReturn(updatedMemberResponse);

            mockMvc.perform(patch("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("사용자 정보 수정되었습니다."));
        }

        @Test
        @WithMockMember
        @DisplayName("실패 - 닉네임 변경 불일치")
        void testUpdateMemberFailure() throws Exception {
            // given
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@naver.com")
                    .nickname("변경실패닉네임")
                    .providerType(OAuthProvider.NAVER)
                    .build();

            MemberResponse failedResponse = MemberResponse.builder()
                    .id(1L)
                    .name("test")
                    .email("test@naver.com")
                    .nickname("기존닉네임") // nickname 다르게 설정
                    .providerType(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(memberService.update(any(MemberInfo.class), any(MemberRequest.class)))
                    .thenReturn(failedResponse);

            // when & then
            mockMvc.perform(patch("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("사용자 정보 수정에 실패하였습니다.\n관리자에게 문의해주세요."));
        }
    }

    @Nested
    @DisplayName("회원 로그아웃")
    class MemberLogOutTests {
        @Test
        @WithMockMember
        @DisplayName("로그아웃 - 성공")
        void testLogoutSuccess() throws Exception {
            when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(true);

            mockMvc.perform(post("/api/members/logout")
                            .header("Authorization", "Bearer testToken")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
        }

        @Test
        @WithMockMember
        @DisplayName("로그아웃 - 실패")
        void testLogoutFail() throws Exception {
            when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(false);

            mockMvc.perform(post("/api/members/logout")
                            .header("Authorization", "Bearer testToken")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("로그아웃에 실패하였습니다.\n관리자에게 문의해주세요."));
        }

        @Test
        @WithMockMember
        @DisplayName("토큰 정보 없는 로그아웃")
        void testLogoutWithoutLoginInfo() throws Exception {
            when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(false);

            MemberApiResponse expectedResponse =
                    MemberApiResponse.builder().code(400).message("로그아웃에 실패하였습니다.\n관리자에게 문의해주세요.").build();

            mockMvc.perform(post("/api/members/logout")
                            .header("Authorization", "Bearer testToken")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                    .andExpect(jsonPath("$.message").value(expectedResponse.message()));
        }
    }

    @Nested
    @DisplayName("회원 토큰")
    class MemberTokenTests {
        @Test
        @DisplayName("OAuth 토큰 요청 실패 시 예외 처리")
        @Disabled
        void testOAuthTokenRequestFailure() throws Exception {
            NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
            when(memberService.oauthLogin(any(NaverLoginParams.class), any(HttpSession.class)))
                    .thenThrow(new OAuthTokenRequestException("OAuth token is empty"));

            mockMvc.perform(get("/api/members/oauth/naver")
                            .param("code", "auth_code")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("OAuth token is empty"));
        }

        @Test
        @DisplayName("OAuth 토큰에 리프레시 토큰이 없을 때 예외 처리")
        @Disabled
        void testOAuthTokenWithoutRefreshToken() throws Exception {
            NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
            NaverResponse oAuthResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("123456789").name("test").email("test@naver.com").build()).build(); // Mock or populate with necessary data
            NaverToken oAuthToken = NaverToken.builder().accessToken("access_token").build(); // Missing refresh token

            when(memberService.oauthLogin(any(NaverLoginParams.class), any(HttpSession.class)))
                    .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

            mockMvc.perform(get("/api/members/oauth/naver")
                            .param("code", "auth_code")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Refresh token is empty"));
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class MemberDisableTests {
        @Test
        @WithMockMember
        @DisplayName("회원 탈퇴 - 성공")
        void testDeleteMemberSuccess() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@naver.com")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.NAVER)
                    .build();

            when(memberService.disable(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

            mockMvc.perform(delete("/api/members")
                            .header("Authorization", "Bearer testToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("탈퇴되었습니다. 이용해주셔서 감사합니다."));
        }

        @Test
        @WithMockMember
        @DisplayName("회원 탈퇴 - 실패")
        void testDeleteMemberFail() throws Exception {
            MemberRequest memberRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@naver.com")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.NAVER)
                    .build();

            when(memberService.disable(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(false);

            mockMvc.perform(delete("/api/members")
                            .header("Authorization", "Bearer testToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("탈퇴에 실패하였습니다.\n관리자에게 문의해주세요."));
        }
    }
}