package kr.co.pinup.verification.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GMailService;
import kr.co.pinup.verification.Verification;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.dto.VerificationConfirm;
import kr.co.pinup.verification.model.dto.VerificationRequest;
import kr.co.pinup.verification.model.enums.VerifyPurpose;
import kr.co.pinup.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final MemberService memberService;
    private final GMailService OAuthMailService;
    private final AppLogger appLogger;

    public String sendCode(VerificationRequest request) {
        String email = request.email();
        VerifyPurpose purpose = request.purpose();
        OAuthProvider provider = memberService.getProviderType(email);

        switch (purpose) {
            case REGISTER:
                if (provider != null) {
                    throw new VerificationBadRequestException(provider.getDisplayName() + " 계정으로 이미 가입되어 있습니다.\n" +
                            provider.getDisplayName() + " 로그인을 이용해 주세요.");
                }
                break;

            case RESET_PASSWORD:
                if (provider == null) {
                    throw new VerificationBadRequestException("가입되지 않은 이메일입니다.");
                }
                if (provider != OAuthProvider.PINUP) {
                    throw new VerificationBadRequestException(provider.getDisplayName() + " 계정은 비밀번호 변경이 불가합니다.\n" +
                            provider.getDisplayName() + " 로그인을 이용해 주세요.");
                }
                break;
        }

        // 실제 메일 전송 로직
        boolean sent = sendEmailCode(email);
        if (!sent) throw new IllegalStateException("인증 코드 전송에 실패했습니다.");
        return "인증 코드가 성공적으로 전송되었습니다.";
    }

    public void verifyCode(VerificationConfirm confirm, HttpSession session) {
        appLogger.info(new InfoLog("인증 코드 검증 시작 - 이메일=" + maskEmail(confirm.email())));

        // 코드 검증
        boolean verified = checkCode(confirm.email(), confirm.code());
        if (!verified) throw new IllegalStateException("인증 코드가 일치하지 않습니다.");

        if (confirm.purpose() == VerifyPurpose.RESET_PASSWORD) {
            session.setAttribute("verifiedEmail", confirm.email());
        }
    }

    public static String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private boolean sendEmailCode(String email) {
        String maskingEmail = maskEmail(email);
        try {
            String code = String.format("%06d", new Random().nextInt(1000000));

            verificationRepository.deleteByEmail(email);
            verificationRepository.flush(); // 즉시 반영
            appLogger.info(new InfoLog("기존 인증 코드 삭제 완료 - 이메일=" + maskingEmail));

            OAuthMailService.sendVerificationCode(email, code);
            appLogger.info(new InfoLog("메일 전송 완료 - 이메일=" + maskingEmail));

            Verification verification = new Verification(email, code, LocalDateTime.now().plusMinutes(5));
            verificationRepository.save(verification);
            appLogger.info(new InfoLog("새 인증 코드 저장 완료 - 이메일=" + maskingEmail));

            return true;
        } catch (Exception e) {
            appLogger.error(new InfoLog("메일 전송 실패 - 이메일=" + maskingEmail + ", 오류=" + e.getMessage()));
            return false;
        }
    }

    private boolean checkCode(String email, String code) {
        Verification verification = verificationRepository.findByEmail(email)
                .orElseThrow(() -> new VerificationBadRequestException("인증 코드가 발송되지 않았습니다."));

        if (!verification.getCode().equals(code)) {
            throw new VerificationBadRequestException("인증 코드가 일치하지 않습니다.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerificationBadRequestException("인증 코드가 만료되었습니다.");
        }

        verificationRepository.delete(verification);
        return true;
    }
}
