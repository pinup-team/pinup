package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.*;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.security.SecurityUtil;
import kr.co.pinup.support.RestDocsSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberApiController.class)
@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({MemberApiControllerDocsTest.MockConfig.class, MemberApiControllerDocsTest.SecurityConfig.class, RestDocsSupport.class, LoggerConfig.class})
class MemberApiControllerDocsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MemberService memberService;

    @Autowired
    SecurityUtil securityUtil;

    @Autowired
    RestDocumentationResultHandler restDocs;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    MemberRequest request = MemberRequest.builder()
            .name("test")
            .email("test@naver.com")
            .nickname("updatedTestNickname")
            .providerType(OAuthProvider.NAVER)
            .build();

    MemberResponse response = MemberResponse.builder()
            .id(1L)
            .name("test")
            .email("test@naver.com")
            .nickname("updatedTestNickname")
            .providerType(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public MemberService memberService() {
            return mock(MemberService.class);
        }

        @Bean
        public SecurityUtil securityUtil() {
            return mock(SecurityUtil.class);
        }

        @Bean
        public OAuthService oAuthService() {
            return mock(OAuthService.class);
        }
    }

    @TestConfiguration
    static class SecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Test
    @DisplayName("GET /api/members/validate - 회원 가입 성공 문서화")
    void testRegisterSuccess_document() throws Exception {
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
                .password(passwordEncoder.encode("test1234!"))
                .providerType(OAuthProvider.PINUP)
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();

        when(memberService.register(any(MemberRequest.class))).thenReturn(Pair.of(member, "가입완료"));

        mockMvc.perform(post("/api/members/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 완료되었습니다."))
                .andDo(document("members-register-success",
                        requestFields(
                                fieldWithPath("name").description("회원 이름"),
                                fieldWithPath("email").description("회원 이메일"),
                                fieldWithPath("nickname").description("회원 닉네임"),
                                fieldWithPath("providerType").description("OAuth 제공자 타입 (예: PINUP)"),
                                fieldWithPath("password").description("회원 비밀번호")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("GET /api/members/validate - 회원 이메일 중복 성공 문서화")
    void testValidateEmailAvailable_document() throws Exception {
        when(memberService.validateEmail(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/members/validate")
                        .param("email", "available@test.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("가입 가능한 이메일입니다."))
                .andDo(document("members-validate-email-available",
                        queryParameters(
                                parameterWithName("email").description("중복 확인할 이메일 주소")
                        )
                ));
    }

    @Test
    @DisplayName("POST /api/members/login - 회원 자체 로그인 성공 문서화")
    void testPinupLoginSuccess_document() throws Exception {
        Member member = Member.builder()
                .name("핀업")
                .email("pinup@test.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("pinupUser")
                .providerType(OAuthProvider.PINUP)
                .build();

        when(memberService.login(any(MemberLoginRequest.class), any(HttpSession.class)))
                .thenReturn(Pair.of(member, "다시 돌아오신 걸 환영합니다 \"" + member.getName() + "\"님"));

        String loginRequestJson = """
        {
            "email": "pinup@test.com",
            "password": "test1234!",
            "providerType": "PINUP"
        }
        """;

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("다시 돌아오신 걸 환영합니다 \"핀업\"님"))
                .andDo(document("members-login-pinup-success",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일"),
                                fieldWithPath("password").description("비밀번호"),
                                fieldWithPath("providerType").description("OAuth 제공자 (PINUP)")
                        )
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("PATCH /api/members - 회원정보 수정 성공 문서화")
    void updateMemberSuccess_document() throws Exception {
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

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("사용자 정보 수정되었습니다.").build();

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andDo(document("members-update-success",
                        requestFields(
                                fieldWithPath("name").description("사용자 이름"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("nickname").description("변경할 닉네임"),
                                fieldWithPath("providerType").description("OAuth 제공자 타입"),
                                fieldWithPath("password").optional().description("비밀번호 (수정 시에는 null일 수 있음)")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("수정 결과 메시지")
                        )
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("POST /api/members/logout - 로그아웃 성공 문서화")
    void logoutSuccess_document() throws Exception {
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("testToken");
        when(memberService.logout(any(OAuthProvider.class), eq("testToken"))).thenReturn(true);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("로그아웃되었습니다.").build();

        mockMvc.perform(post("/api/members/logout")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andDo(document("members-logout-success",
                        requestHeaders(
                                headerWithName("Authorization").description("사용자 인증 토큰 (Bearer 토큰)")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("로그아웃 결과 메시지")
                        )
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("DELETE /api/members - 회원 탈퇴 성공 문서화")
    void deleteMemberSuccess_document() throws Exception {
        when(memberService.disable(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

        mockMvc.perform(delete("/api/members")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("탈퇴되었습니다. 이용해주셔서 감사합니다."))
                .andDo(document("members-disable-success",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        requestFields(
                                fieldWithPath("name").description("회원 이름"),
                                fieldWithPath("email").description("회원 이메일"),
                                fieldWithPath("password").description("사용자 비밀번호"),
                                fieldWithPath("nickname").description("회원 닉네임"),
                                fieldWithPath("providerType").description("OAuth 제공자")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("탈퇴 결과 메시지")
                        )
                ));
    }
}
