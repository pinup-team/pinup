package kr.co.pinup.members.controller;

import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberLoginRequest;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfigTest.class, LoggerConfig.class})
@WebMvcTest(MemberController.class)
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
public class MemberControllerSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Login 페이지 이동")
    void login() throws Exception {
        mockMvc.perform(get("/members/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/login"))
                .andExpect(model().attributeExists("loginRequest"))
                .andDo(result -> {
                    Object loginRequest = result.getModelAndView().getModel().get("loginRequest");
                    assertTrue(loginRequest instanceof MemberLoginRequest);
                    MemberLoginRequest req = (MemberLoginRequest) loginRequest;
                    assertEquals(OAuthProvider.PINUP, req.providerType());
                });
    }

    @Test
    @DisplayName("Register 페이지 이동")
    void register() throws Exception {
        mockMvc.perform(get("/members/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andDo(result -> {
                    Object registerRequest = result.getModelAndView().getModel().get("registerRequest");
                    assertTrue(registerRequest instanceof MemberRequest);
                    MemberRequest req = (MemberRequest) registerRequest;
                    assertEquals(OAuthProvider.PINUP, req.providerType());
                });
    }

    @Test
    @WithMockMember(nickname = "test", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("마이 페이지 이동-OAuth 사용자")
    void memberProfileOAuth() throws Exception {
        MemberResponse testResponse = new MemberResponse(1L, "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER, false);

        when(memberService.findMember(any(MemberInfo.class))).thenReturn(testResponse);

        mockMvc.perform(get("/members/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/profile"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().attribute("profile", is(testResponse)));
    }

    @Test
    @WithMockMember(nickname = "test", provider = OAuthProvider.PINUP, role = MemberRole.ROLE_USER)
    @DisplayName("마이 페이지 이동-자체 로그인 사용자")
    void memberProfilePinup() throws Exception {
        MemberResponse testResponse = new MemberResponse(1L, "test", "test@test.com", "핀업TestMember", OAuthProvider.PINUP, MemberRole.ROLE_USER, false);

        when(memberService.findMember(any(MemberInfo.class))).thenReturn(testResponse);

        mockMvc.perform(get("/members/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/profile"))
                .andExpect(model().attributeExists("profile"))
                .andDo(result -> {
                    Object profileObj = result.getModelAndView().getModel().get("profile");

                    // PINUP인 경우 MemberRequest 타입이고 password는 null
                    assertTrue(profileObj instanceof MemberRequest, "profile 객체 타입은 MemberRequest 여야 합니다.");

                    MemberRequest profile = (MemberRequest) profileObj;
                    assertEquals(testResponse.getName(), profile.name());
                    assertEquals(testResponse.getEmail(), profile.email());
                    assertNull(profile.password());
                    assertEquals(testResponse.getNickname(), profile.nickname());
                    assertEquals(testResponse.getProviderType(), profile.providerType());
                });
    }

    @Test
    @DisplayName("본인 인증 페이지 접근")
    void verifyPage_returnsVerifyView() throws Exception {
        mockMvc.perform(get("/members/verify"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/verify"));
    }

    @Nested
    @DisplayName("비밀번호 재설정 화면")
    class RegisterMemberTests {
        @Test
        @DisplayName("비밀번호 재설정 페이지 - 세션 없으면 /members/verify로 리다이렉트")
        void password_noSession_redirectVerify() throws Exception {
            mockMvc.perform(get("/members/password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/members/verify"));
        }

        @Test
        @DisplayName("비밀번호 재설정 페이지 - 세션 있을 때 정상 페이지 반환")
        void password_withSession_returnsPage() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("verifiedEmail", "test@test.com");

            MvcResult result = mockMvc.perform(get("/members/password").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("views/members/password"))
                    .andExpect(model().attributeExists("resetRequest"))
                    .andReturn();

            Object resetRequest = result.getModelAndView().getModel().get("resetRequest");
            assertNotNull(resetRequest, "resetRequest가 null이면 안 됩니다.");
            // 세부 필드 확인
            // resetRequest는 MemberPasswordRequest 타입
            assertEquals("test@test.com", ((kr.co.pinup.members.model.dto.MemberPasswordRequest) resetRequest).email());
            assertEquals(OAuthProvider.PINUP, ((kr.co.pinup.members.model.dto.MemberPasswordRequest) resetRequest).providerType());
        }
    }
}