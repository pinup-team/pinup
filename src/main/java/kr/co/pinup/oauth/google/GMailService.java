package kr.co.pinup.oauth.google;

import jakarta.mail.internet.MimeMessage;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.oauth.OAuthMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class GMailService implements OAuthMailService {
    // private final GoogleApiClient googleApiClient;
    private final JavaMailSenderImpl javaMailSender;
    private final AppLogger appLogger;

    // 환경변수에 저장한 refresh token
    // private final String refreshToken = System.getenv("GMAIL_REFRESH_TOKEN");

    @Override
    public void sendVerificationCode(String toEmail, String code) {
//        GoogleToken googleToken = googleApiClient.refreshAccessToken(refreshToken);
//        String accessToken = googleToken.getAccessToken();
//        mailSender.setPassword(accessToken); // OAuth2용 토큰

        // OAuth 관련 코드 제거, 앱 비밀번호 사용

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            Resource resource = new ClassPathResource("templates/views/mail.html");
            String mailHtml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // ${code} 부분 치환
            mailHtml = mailHtml.replace("${code}", code);

            helper.setTo(toEmail);
            helper.setSubject("[Pinup] 비밀번호 재설정 인증코드 안내");
            helper.setText(mailHtml, true); // true = HTML 메일

            javaMailSender.send(message);
            appLogger.info(new InfoLog("메일 전송 완료 - 이메일=" + toEmail.replaceAll("(^..)[^@]+", "$1***") + ", 코드=" + code));

        } catch (Exception e) {
            appLogger.error(new ErrorLog("메일 전송 실패 - 이메일=" + toEmail.replaceAll("(^..)[^@]+", "$1***") + ", 오류=" + e.getMessage(), e));
            throw new RuntimeException("메일 전송 실패", e);
        }
    }
}
