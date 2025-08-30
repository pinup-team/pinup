package kr.co.pinup.verification.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.dto.VerificationConfirm;
import kr.co.pinup.verification.model.dto.VerificationRequest;
import kr.co.pinup.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/verification", produces = "application/json;charset=UTF-8")
public class VerificationApiController {

    private final VerificationService verificationService;
    private final AppLogger appLogger;

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody @Valid VerificationRequest request) {
        String maskingEmail = verificationService.maskEmail(request.email());
        appLogger.info(new InfoLog("이메일 인증 요청: " + maskingEmail
                + ", 목적=" + request.purpose()));
        try {
            String message = verificationService.sendCode(request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (VerificationBadRequestException e) {
            appLogger.warn(new WarnLog("이메일 전송 실패: " + maskingEmail));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("이메일 전송 실패: " + maskingEmail, e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerificationConfirm confirm, HttpSession session) {
        String maskingEmail = verificationService.maskEmail(confirm.email());

        appLogger.info(new InfoLog("인증 코드 검증 요청: " + maskingEmail
                + ", 목적=" + confirm.purpose()));
        try {
            verificationService.verifyCode(confirm, session);
            return ResponseEntity.ok(Map.of("message", "본인 확인이 완료되었습니다."));
        } catch (VerificationBadRequestException e) {
            appLogger.warn(new WarnLog("인증 코드 검증 실패: " + maskingEmail));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("인증 코드 검증 실패: " + maskingEmail, e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
