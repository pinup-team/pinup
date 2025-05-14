package kr.co.pinup.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretsFetcherUnitTest {

    private SecretsFetcher secretsFetcher;

    @Mock
    private SecretsManagerClient secretsManagerClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("SecretsFetcher - 특정 필드 조회 (Mock)")
    void getSecretsField() throws Exception {
        // Mocking SecretsManagerClient
        String mockSecretJson = """
            {
                "kakao.api.key.rest": "test-rest-key",
                "kakao.api.key.js": "test-js-key"
            }
            """;

        GetSecretValueResponse mockResponse = GetSecretValueResponse.builder()
                .secretString(mockSecretJson)
                .build();

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(mockResponse);

        String restKey = secretsFetcher.getSecretField("kakao.api.key.rest");
        String jsKey = secretsFetcher.getSecretField("kakao.api.key.js");

        assertEquals("test-rest-key", restKey);
        assertEquals("test-js-key", jsKey);
    }

    @Test
    @DisplayName("SecretsFetcher - 잘못된 필드 조회 (Mock)")
    void getInvalidSecretsField() throws Exception {
        String mockSecretJson = """
            {
                "kakao.api.key.rest": "test-rest-key"
            }
            """;

        GetSecretValueResponse mockResponse = GetSecretValueResponse.builder()
                .secretString(mockSecretJson)
                .build();

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(mockResponse);

        String invalidKey = secretsFetcher.getSecretField("invalid-key");

        assertEquals("", invalidKey);
    }
}
