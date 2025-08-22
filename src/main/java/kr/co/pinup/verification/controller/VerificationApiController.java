package kr.co.pinup.verification.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.verification.exception.VerificationBadRequestException;
import kr.co.pinup.verification.model.VerificationRequest;
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
    private final MemberService memberService;
    private final AppLogger appLogger;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        appLogger.info(new InfoLog("이메일 인증 코드 전송: " + verificationService.maskEmail(email)));

        return verificationService.sendCode(email)
                ? ResponseEntity.ok("인증 코드가 성공적으로 전송되었습니다.")
                : ResponseEntity.status(HttpStatus.CONFLICT).body("인증 코드 전송에 실패하였습니다.");
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerificationRequest verificationRequest, HttpSession session) {
        String maskingEmail = verificationService.maskEmail(verificationRequest.email());
        appLogger.info(new InfoLog("이메일 인증 코드 검증: " + maskingEmail));

        try {
            verificationService.verifyCode(verificationRequest);

            OAuthProvider provider = memberService.getProviderType(verificationRequest.email());

            if (provider == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "이 계정은 가입되지 않았습니다.\n비밀번호 변경은 지원되지 않습니다."));
            } else if (provider != OAuthProvider.PINUP) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "이 계정은 " + provider.getDisplayName() + " 계정으로 가입되어 있습니다.\n비밀번호 변경은 지원되지 않습니다."));
            }

            // 인증 성공 시 비밀번호 변경을 위해 세션에 메일 저장
            session.setAttribute("verifiedEmail", verificationRequest.email());
            return ResponseEntity.ok(Map.of("message", "사용자 본인 확인이 완료되었습니다.\n비밀번호 변경 화면으로 이동합니다."));

        } catch (VerificationBadRequestException e) {
            appLogger.warn(new WarnLog("이메일 인증 실패 - email: " + maskingEmail)
                    .setStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                    .addDetails("reason", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("이메일 인증 처리 중 예외 발생 - email: " + maskingEmail, e));
            throw new VerificationBadRequestException("본인 인증에 실패하였습니다.");
        }
    }
}
