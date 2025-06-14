package kr.co.pinup.api.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.api.aws.exception.SecretsManagerFetchException;
import kr.co.pinup.api.kakao.model.dto.KakaoSecret;
import kr.co.pinup.config.AwsSecretsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AwsSecretsManagerProviderTest {

    private static final String KAKAO_API_REST_KEY = "kakao.api.key.rest";
    private static final String SECRET_NAME = "local/api/kakaomap";

    @Mock
    private SecretsManagerClientFactory secretsManagerClientFactory;

    @Mock
    private SecretsManagerClient secretsManagerClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AwsSecretsProperties secretsProperties;

    @InjectMocks
    private AwsSecretsManagerProvider awsSecretsManagerProvider;

    @DisplayName("Secret 값을 가져온다.")
    @Test
    void getSecretValue() throws JsonProcessingException {
        // Arrange
        final String restKey = "test-rest-key";
        final String jsKey = "test-js-key";
        final String secretJson = String.format("""
                    {
                        "%s": "%s",
                        "kakao.api.key.js": "%s"
                    }
                """, KAKAO_API_REST_KEY, restKey, jsKey);
        final KakaoSecret kakaoSecret = new KakaoSecret(restKey, jsKey);

        given(secretsManagerClientFactory.create()).willReturn(secretsManagerClient);
        given(secretsProperties.getSecretName()).willReturn(SECRET_NAME);
        given(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
                .willReturn(GetSecretValueResponse.builder()
                        .secretString(secretJson)
                        .build());
        given(objectMapper.readValue(secretJson, KakaoSecret.class)).willReturn(kakaoSecret);

        // Act
        final String result = awsSecretsManagerProvider.getSecretValue(KAKAO_API_REST_KEY);

        // Assert
        assertThat(result).isEqualTo(restKey);
    }

    @DisplayName("잘못된 Secret Key로 값을 가져오려 하면 예외가 발생한다.")
    @Test
    void invalidSecretKey() throws JsonProcessingException {
        // Arrange
        final String restSecretKey = "kakao.rest.api.key";
        final String restKey = "test-rest-key";
        final String jsKey = "test-js-key";
        final String secretJson = String.format("""
                    {
                        "%s": "%s",
                        "kakao.api.key.js": "%s"
                    }
                """, KAKAO_API_REST_KEY, restKey, jsKey);
        final KakaoSecret kakaoSecret = new KakaoSecret(restKey, jsKey);

        given(secretsManagerClientFactory.create()).willReturn(secretsManagerClient);
        given(secretsProperties.getSecretName()).willReturn(SECRET_NAME);
        given(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
                .willReturn(GetSecretValueResponse.builder()
                        .secretString(secretJson)
                        .build());
        given(objectMapper.readValue(secretJson, KakaoSecret.class)).willReturn(kakaoSecret);

        // Act Assert
        assertThatThrownBy(() -> awsSecretsManagerProvider.getSecretValue(restSecretKey))
                .isInstanceOf(SecretsManagerFetchException.class)
                .hasMessage(restSecretKey + " 키가 일치하지 않습니다.");
    }
}