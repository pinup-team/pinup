package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberApiResponse;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.security.SecurityUtil;
import kr.co.pinup.support.RestDocsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberApiController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({MemberApiControllerDocsTest.MockConfig.class, MemberApiControllerDocsTest.SecurityConfig.class, RestDocsSupport.class})
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
    @WithMockMember
    @DisplayName("Get /api/members/nickname - 닉네임 생성 문서화")
    void makeNickname_document() throws Exception {
        when(memberService.makeNickname()).thenReturn("generatedNickname");

        mockMvc.perform(get("/api/members/nickname")
                        .contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedNickname"))
                .andDo(document("generatedNickname"
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("PATCH /api/members - 닉네임 수정 문서화")
    void update_document() throws Exception {
        when(memberService.update(any(MemberInfo.class), any(MemberRequest.class)))
                .thenReturn(response);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("닉네임이 변경되었습니다.").build();

        mockMvc.perform(patch("/api/members")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andDo(document("members-update-nickname",
                        requestFields(
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("nickname").description("변경할 닉네임"),
                                fieldWithPath("providerType").description("OAuth 제공자")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("닉네임 변경 결과 메시지")
                        )
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("DELETE /api/members - 회원 탈퇴 문서화")
    void disableMember_document() throws Exception {
        when(memberService.disable(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("탈퇴되었습니다. 이용해주셔서 감사합니다.").build();

        mockMvc.perform(delete("/api/members")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andDo(document("members-disable",
                        requestFields(
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("nickname").description("닉네임"),
                                fieldWithPath("providerType").description("OAuth 제공자")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("탈퇴 결과 메시지")
                        )
                ));
    }

    @Test
    @WithMockMember
    @DisplayName("POST /api/members/logout - 로그아웃 문서화")
    void logoutMember_document() throws Exception {
        when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(true);
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("testToken");

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("로그아웃에 성공하였습니다.").build();

        mockMvc.perform(post("/api/members/logout")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()))
                .andDo(document("members-logout",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("로그아웃 결과 메시지")
                        )
                ));
    }
}
