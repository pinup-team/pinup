package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberApiResponse;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.google.GoogleResponse;
import kr.co.pinup.oauth.google.GoogleToken;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverResponse;
import kr.co.pinup.oauth.naver.NaverToken;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfigTest.class, LoggerConfig.class})
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("네이버 OAuth 로그인 성공")
    void testLoginNaverSuccess() throws Exception {
        NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
        NaverResponse oAuthResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("123456789").name("test").email("test@naver.com").build()).build(); // Mock or populate with necessary data
        NaverToken oAuthToken = NaverToken.builder().accessToken("access_token").refreshToken("refresh_token").build(); // Missing refresh token

        when(memberService.login(any(NaverLoginParams.class), any(HttpSession.class)))
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

        when(memberService.login(any(GoogleLoginParams.class), any(HttpSession.class)))
                .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

        mockMvc.perform(get("/api/members/oauth/google")
                        .param("code", "auth_code")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 닉네임 추천")
    void testMakeNickname() throws Exception {
        String generateNickname = "generate-nickname";
        when(memberService.makeNickname()).thenReturn(generateNickname);

        mockMvc.perform(get("/api/members/nickname")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(generateNickname));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 정보 업데이트")
    void testUpdateMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).build();
        MemberResponse updatedMemberResponse = MemberResponse.builder().id(1L).name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();

        when(memberService.update(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(updatedMemberResponse);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("닉네임이 변경되었습니다.").build();

        mockMvc.perform(patch("/api/members")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 탈퇴_성공")
    void testDeleteMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).build();

        when(memberService.disable(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("탈퇴되었습니다. 이용해주셔서 감사합니다.").build();

        mockMvc.perform(delete("/api/members")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()));
    }

    @Test
    @WithMockMember
    @DisplayName("로그아웃")
    void testLogout() throws Exception {
        when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(true);

        MemberApiResponse expectedResponse =
                MemberApiResponse.builder().code(200).message("로그아웃에 성공하였습니다.").build();

        mockMvc.perform(post("/api/members/logout")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedResponse.code()))
                .andExpect(jsonPath("$.message").value(expectedResponse.message()));
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

    @Test
    @DisplayName("OAuth 토큰 요청 실패 시 예외 처리")
    @Disabled
    void testOAuthTokenRequestFailure() throws Exception {
        NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
        when(memberService.login(any(NaverLoginParams.class), any(HttpSession.class)))
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

        when(memberService.login(any(NaverLoginParams.class), any(HttpSession.class)))
                .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

        mockMvc.perform(get("/api/members/oauth/naver")
                        .param("code", "auth_code")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Refresh token is empty"));
    }
}