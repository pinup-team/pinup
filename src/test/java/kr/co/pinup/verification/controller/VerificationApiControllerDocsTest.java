package kr.co.pinup.verification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.support.RestDocsSupport;
import kr.co.pinup.verification.model.dto.VerificationConfirm;
import kr.co.pinup.verification.model.dto.VerificationRequest;
import kr.co.pinup.verification.model.enums.VerifyPurpose;
import kr.co.pinup.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationApiController.class)
@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({RestDocsSupport.class, LoggerConfig.class})
class VerificationApiControllerDocsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    VerificationService verificationService;

    @Autowired
    MemberService memberService;

    @Autowired
    RestDocumentationResultHandler restDocs;

    private static final VerificationRequest SEND_EMAIL_REQ =
            new VerificationRequest("test@pinup.com", VerifyPurpose.REGISTER);

    private static final VerificationConfirm VERIFY_SUCCESS_REQ =
            new VerificationConfirm("test@pinup.com", "123456", VerifyPurpose.REGISTER);

    @TestConfiguration
    static class MockConfig {
        @Bean
        public VerificationService verificationService() {
            return org.mockito.Mockito.mock(VerificationService.class);
        }

        @Bean
        public MemberService memberService() {
            return org.mockito.Mockito.mock(MemberService.class);
        }
    }

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @Test
    @DisplayName("POST /api/verification/send - 인증 코드 메일 전송 문서화")
    void sendEmailSuccess() throws Exception {
        when(verificationService.sendCode(any(VerificationRequest.class)))
                .thenReturn("인증 코드가 성공적으로 전송되었습니다.");

        mockMvc.perform(post("/api/verification/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SEND_EMAIL_REQ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 성공적으로 전송되었습니다."))
                .andDo(document("verification-send-success",
                        requestFields(
                                fieldWithPath("email").description("인증 코드를 받을 이메일 주소"),
                                fieldWithPath("purpose").description("인증 목적 (REGISTER / RESET_PASSWORD)")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("POST /api/verification/verifyCode - 계정 인증 성공 문서화")
    void verifyCodeSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();

        doNothing().when(verificationService)
                .verifyCode(any(VerificationConfirm.class), any(MockHttpSession.class));

        mockMvc.perform(post("/api/verification/verifyCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(VERIFY_SUCCESS_REQ))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("본인 확인이 완료되었습니다."))
                .andDo(document("verification-verify-success",
                        requestFields(
                                fieldWithPath("email").description("인증할 이메일 주소"),
                                fieldWithPath("code").description("사용자가 입력한 인증 코드"),
                                fieldWithPath("purpose").description("인증 목적 (REGISTER / RESET_PASSWORD)")
                        ),
                        responseBody()
                ));
    }
}
