package kr.co.pinup.verification.service;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.oauth.google.GMailService;
import kr.co.pinup.verification.Verification;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.VerificationRequest;
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
    private final GMailService OAuthMailService;
    private final AppLogger appLogger;

    public boolean sendCode(String email) {
        try {
            String code = String.format("%06d", new Random().nextInt(1000000));

            System.out.println("기존코드 삭제 deleteByEmail");
            // 기존 코드 삭제
            verificationRepository.deleteByEmail(email);
            verificationRepository.flush(); // 즉시 반영
            appLogger.info(new InfoLog("기존 인증 코드 삭제 완료 - 이메일=" + email));

            // 메일 발송
            OAuthMailService.sendVerificationCode(email, code);
            appLogger.info(new InfoLog("메일 전송 완료 - 이메일=" + email));

            // 새 코드 저장
            Verification verification = new Verification(email, code, LocalDateTime.now().plusMinutes(5));
            verificationRepository.save(verification);
            appLogger.info(new InfoLog("새 인증 코드 저장 완료 - 이메일=" + email + ", 코드=" + code));

            return true;
        } catch (Exception e) {
            appLogger.error(new InfoLog("메일 전송 실패 - 이메일=" + email + ", 오류=" + e.getMessage()));
            return false;
        }
    }

    public void verifyCode(VerificationRequest verificationRequest) {
        Verification verification = verificationRepository.findByEmail(verificationRequest.email())
                .orElseThrow(() -> new VerificationBadRequestException("인증 코드가 발송되지 않았습니다."));

        if (verificationRequest.code() == null || verificationRequest.code().isBlank()) {
            throw new VerificationBadRequestException("인증 코드가 필요합니다.");
        }

        if (!verification.getCode().equals(verificationRequest.code())) {
            throw new VerificationBadRequestException("인증 코드가 일치하지 않습니다.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerificationBadRequestException("인증 코드가 만료되었습니다.");
        }

        // 검증 성공 시 DB에서 해당 기록 삭제 (재사용 방지)
        verificationRepository.delete(verification);
    }
}
