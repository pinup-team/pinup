package kr.co.pinup.verification.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GMailService;
import kr.co.pinup.verification.Verification;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.dto.VerificationConfirm;
import kr.co.pinup.verification.model.dto.VerificationRequest;
import kr.co.pinup.verification.model.enums.VerifyPurpose;
import kr.co.pinup.verification.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationServiceUnitTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private GMailService oAuthMailService;

    @Mock
    private AppLogger appLogger;

    @Mock
    private HttpSession session;

    @InjectMocks
    private VerificationService verificationService;

    private VerificationConfirm validConfirm;
    private VerificationConfirm invalidConfirm;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validConfirm = new VerificationConfirm("test@pinup.com", "123456", VerifyPurpose.REGISTER);
        invalidConfirm = new VerificationConfirm("fail@pinup.com", "000000", VerifyPurpose.REGISTER);
    }

    @Nested
    @DisplayName("verifyCode() 테스트")
    class VerifyCodeTests {

        @Test
        @DisplayName("정상 인증 코드 검증")
        void testVerifyCode_Success() {
            Verification verification = new Verification(
                    validConfirm.email(),
                    validConfirm.code(),
                    LocalDateTime.now().plusMinutes(5)
            );

            when(verificationRepository.findByEmail(validConfirm.email()))
                    .thenReturn(Optional.of(verification));

            verificationService.verifyCode(validConfirm, session);

            verify(verificationRepository).delete(verification);
        }

        @Test
        @DisplayName("인증 코드 존재하지 않음")
        void testVerifyCode_NotFound() {
            when(verificationRepository.findByEmail(invalidConfirm.email()))
                    .thenReturn(Optional.empty());

            VerificationBadRequestException ex = assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.verifyCode(invalidConfirm, session)
            );

            assertTrue(ex.getMessage().contains("인증 코드가 발송되지 않았습니다."));
        }

        @Test
        @DisplayName("인증 코드 불일치")
        void testVerifyCode_CodeMismatch() {
            Verification verification = new Verification(
                    invalidConfirm.email(),
                    "999999",
                    LocalDateTime.now().plusMinutes(5)
            );

            when(verificationRepository.findByEmail(invalidConfirm.email()))
                    .thenReturn(Optional.of(verification));

            assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.verifyCode(invalidConfirm, session)
            );
        }

        @Test
        @DisplayName("인증 코드 만료")
        void testVerifyCode_Expired() {
            Verification verification = new Verification(
                    invalidConfirm.email(),
                    invalidConfirm.code(),
                    LocalDateTime.now().minusMinutes(1)
            );

            when(verificationRepository.findByEmail(invalidConfirm.email()))
                    .thenReturn(Optional.of(verification));

            assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.verifyCode(invalidConfirm, session)
            );
        }

        @Test
        @DisplayName("비밀번호 재설정 목적 시 세션 저장 확인")
        void testVerifyCode_ResetPassword_SessionSet() {
            VerificationConfirm resetConfirm = new VerificationConfirm(
                    "reset@pinup.com", "123456", VerifyPurpose.RESET_PASSWORD
            );

            Verification verification = new Verification(
                    resetConfirm.email(),
                    resetConfirm.code(),
                    LocalDateTime.now().plusMinutes(5)
            );

            when(verificationRepository.findByEmail(resetConfirm.email()))
                    .thenReturn(Optional.of(verification));

            verificationService.verifyCode(resetConfirm, session);

            verify(session).setAttribute("verifiedEmail", resetConfirm.email());
        }
    }

    @Nested
    @DisplayName("sendCode() 테스트")
    class SendCodeTests {

        @Test
        @DisplayName("회원가입 - 이미 다른 provider로 가입된 경우 예외 발생")
        void testSendCode_Register_AlreadyExists() {
            VerificationRequest request = new VerificationRequest("google@pinup.com", VerifyPurpose.REGISTER);

            when(memberService.getProviderType(request.email())).thenReturn(OAuthProvider.GOOGLE);

            assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.sendCode(request)
            );
        }

        @Test
        @DisplayName("비밀번호 재설정 - 미가입 이메일")
        void testSendCode_ResetPassword_NotRegistered() {
            VerificationRequest request = new VerificationRequest("unknown@pinup.com", VerifyPurpose.RESET_PASSWORD);

            when(memberService.getProviderType(request.email())).thenReturn(null);

            assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.sendCode(request)
            );
        }

        @Test
        @DisplayName("비밀번호 재설정 - 소셜 계정")
        void testSendCode_ResetPassword_SocialAccount() {
            VerificationRequest request = new VerificationRequest("google@pinup.com", VerifyPurpose.RESET_PASSWORD);

            when(memberService.getProviderType(request.email())).thenReturn(OAuthProvider.GOOGLE);

            assertThrows(VerificationBadRequestException.class, () ->
                    verificationService.sendCode(request)
            );
        }
    }

    @Nested
    @DisplayName("maskEmail() 테스트")
    class MaskEmailTests {
        @Test
        void testMaskEmail_NormalCase() {
            String masked = VerificationService.maskEmail("abcd@pinup.com");
            assertEquals("ab***@pinup.com", masked);
        }

        @Test
        void testMaskEmail_ShortLocalPart() {
            String masked = VerificationService.maskEmail("a@pinup.com");
            assertEquals("***@pinup.com", masked);
        }

        @Test
        void testMaskEmail_Null() {
            assertNull(VerificationService.maskEmail(null));
        }
    }
}
