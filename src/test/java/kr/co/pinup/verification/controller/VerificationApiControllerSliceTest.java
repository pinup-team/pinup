package kr.co.pinup.verification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.verification.model.VerificationRequest;
import kr.co.pinup.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfigTest.class, LoggerConfig.class})
@WebMvcTest(VerificationApiController.class)
public class VerificationApiControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private MemberService memberService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("이메일 인증 코드 전송")
    class SendEmailTests {

        @Test
        @DisplayName("인증 코드 전송 성공")
        void testSendEmailSuccess() throws Exception {
            Map<String, String> request = Map.of("email", "test@pinup.com");

            when(verificationService.sendCode("test@pinup.com")).thenReturn(true);

            mockMvc.perform(post("/api/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("인증 코드가 성공적으로 전송되었습니다."));
        }

        @Test
        @DisplayName("인증 코드 전송 실패")
        void testSendEmailConflict() throws Exception {
            Map<String, String> request = Map.of("email", "fail@pinup.com");

            when(verificationService.sendCode("fail@pinup.com")).thenReturn(false);

            mockMvc.perform(post("/api/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(content().string("인증 코드 전송에 실패하였습니다."));
        }
    }

    @Nested
    @DisplayName("이메일 인증 코드 검증")
    class VerifyCodeTests {

        @Test
        @DisplayName("인증 성공 - PINUP 계정")
        void testVerifyCodeSuccess() throws Exception {
            VerificationRequest request = new VerificationRequest("test@pinup.com", "123456");
            MockHttpSession session = new MockHttpSession();

            doNothing().when(verificationService).verifyCode(any());
            when(memberService.getProviderType("test@pinup.com")).thenReturn(OAuthProvider.PINUP);

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string("사용자 본인 확인이 완료되었습니다.\n비밀번호 변경 화면으로 이동합니다."));

            assertThat(session.getAttribute("verifiedEmail")).isEqualTo("test@pinup.com");
        }

        @Test
        @DisplayName("가입되지 않은 계정")
        void testVerifyCodeNotFound() throws Exception {
            VerificationRequest request = new VerificationRequest("notfound@pinup.com", "123456");
            MockHttpSession session = new MockHttpSession();

            doNothing().when(verificationService).verifyCode(any());
            when(memberService.getProviderType("notfound@pinup.com")).thenReturn(null);

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("이 계정은 가입되지 않았습니다.\n비밀번호 변경은 지원되지 않습니다."));
        }

        @Test
        @DisplayName("다른 OAuth 계정")
        void testVerifyCodeOtherProvider() throws Exception {
            VerificationRequest request = new VerificationRequest("google@pinup.com", "123456");
            MockHttpSession session = new MockHttpSession();

            doNothing().when(verificationService).verifyCode(any());
            when(memberService.getProviderType("google@pinup.com")).thenReturn(OAuthProvider.GOOGLE);

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .session(session))
                    .andExpect(status().isConflict())
                    .andExpect(content().string("이 계정은 " + OAuthProvider.GOOGLE.getDisplayName() + " 계정으로 가입되어 있습니다.\n비밀번호 변경은 지원되지 않습니다."));
        }
    }
}