package kr.co.pinup.verification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.dto.VerificationConfirm;
import kr.co.pinup.verification.model.dto.VerificationRequest;
import kr.co.pinup.verification.model.enums.VerifyPurpose;
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

import static org.mockito.ArgumentMatchers.any;
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
        @DisplayName("회원가입 인증 코드 전송 성공")
        void testSendEmailRegisterSuccess() throws Exception {
            VerificationRequest request = new VerificationRequest("test@pinup.com", VerifyPurpose.REGISTER);
            when(verificationService.sendCode(any(VerificationRequest.class)))
                    .thenReturn("인증 코드가 성공적으로 전송되었습니다.");

            mockMvc.perform(post("/api/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "인증 코드가 성공적으로 전송되었습니다.")
                    )));
        }

        @Test
        @DisplayName("회원가입 인증 코드 전송 실패 - BadRequest")
        void testSendEmailRegisterBadRequest() throws Exception {
            VerificationRequest request = new VerificationRequest("fail@pinup.com", VerifyPurpose.REGISTER);
            when(verificationService.sendCode(any(VerificationRequest.class)))
                    .thenThrow(new VerificationBadRequestException("잘못된 요청"));

            mockMvc.perform(post("/api/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "잘못된 요청")
                    )));
        }

        @Test
        @DisplayName("회원가입 인증 코드 전송 실패 - ServerError")
        void testSendEmailRegisterServerError() throws Exception {
            VerificationRequest request = new VerificationRequest("error@pinup.com", VerifyPurpose.REGISTER);
            when(verificationService.sendCode(any(VerificationRequest.class)))
                    .thenThrow(new RuntimeException("서버 오류"));

            mockMvc.perform(post("/api/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "서버 오류")
                    )));
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    class VerifyCodeTests {

        @Test
        @DisplayName("회원가입 계정 인증 성공")
        void testVerifyCodeRegisterSuccess() throws Exception {
            VerificationConfirm confirm = new VerificationConfirm("test@pinup.com", "123456", VerifyPurpose.REGISTER);
            MockHttpSession session = new MockHttpSession();

            doNothing().when(verificationService).verifyCode(any(VerificationConfirm.class), any(MockHttpSession.class));

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirm))
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "본인 확인이 완료되었습니다.")
                    )));
        }

        @Test
        @DisplayName("회원가입 계정 인증 실패 - BadRequest")
        void testVerifyCodeRegisterBadRequest() throws Exception {
            VerificationConfirm confirm = new VerificationConfirm("fail@pinup.com", "123456", VerifyPurpose.REGISTER);
            MockHttpSession session = new MockHttpSession();

            doThrow(new VerificationBadRequestException("인증 코드가 올바르지 않습니다."))
                    .when(verificationService).verifyCode(confirm, session);

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirm))
                            .session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "인증 코드가 올바르지 않습니다.")
                    )));
        }

        @Test
        @DisplayName("회원가입 계정 인증 실패 - ServerError")
        void testVerifyCodeRegisterServerError() throws Exception {
            VerificationConfirm confirm = new VerificationConfirm("error@pinup.com", "123456", VerifyPurpose.REGISTER);
            MockHttpSession session = new MockHttpSession();

            doThrow(new RuntimeException("서버 오류"))
                    .when(verificationService).verifyCode(confirm, session);

            mockMvc.perform(post("/api/verification/verifyCode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirm))
                            .session(session))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().json(objectMapper.writeValueAsString(
                            java.util.Map.of("message", "서버 오류")
                    )));
        }
    }
}
