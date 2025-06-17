package kr.co.pinup.api.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.api.aws.exception.SecretsManagerFetchException;
import kr.co.pinup.api.kakao.model.dto.KakaoSecret;
import kr.co.pinup.config.AwsSecretsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsSecretsManagerProvider implements AwsSecretsProvider {

    private static final String KAKAO_API_REST_KEY = "kakao.api.key.rest";
    private static final String KAKAO_API_JS_KEY = "kakao.api.key.js";

    private final ObjectMapper objectMapper;
    private final AwsSecretsProperties secretsProperties;
    private final SecretsManagerClientFactory secretsManagerClientFactory;

    @Override
    public String getSecretValue(final String secretKey) {
        try (final SecretsManagerClient client = secretsManagerClientFactory.create()) {
            final String secret = client.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId(secretsProperties.getSecretName())
                            .build()
            ).secretString();

            final KakaoSecret kakaoSecret = objectMapper.readValue(secret, KakaoSecret.class);
            log.debug("getSecretValue method kakaoSecret={}", kakaoSecret);

            return switch (secretKey) {
                case KAKAO_API_REST_KEY -> kakaoSecret.restApiKey();
                case KAKAO_API_JS_KEY -> kakaoSecret.jsApiKey();
                default -> throw new SecretsManagerFetchException(secretKey + " 키가 일치하지 않습니다.");
            };
        } catch (SecretsManagerFetchException e) {
            throw new SecretsManagerFetchException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new SecretsManagerFetchException("일치하는 키가 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
