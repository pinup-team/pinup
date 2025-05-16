package kr.co.pinup.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.net.URI;

@Component
public class SecretsFetcher {

    private final String secretName;
    private final String region;
    private final String endpoint;

    public SecretsFetcher(
            @Value("${cloud.aws.secretsmanager.endpoint}") String endpoint,
            @Value("${cloud.aws.region.static}") String region,
            @Value("${cloud.aws.secretsmanager.secret-name}") String secretName
    ) {
        this.secretName = secretName;
        this.region = region;
        this.endpoint = endpoint;
    }

    public String getSecret() {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .endpointOverride(URI.create(endpoint))
                .build();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        try {
            return client.getSecretValue(request).secretString();
        } catch (Exception e) {
            throw new RuntimeException("시크릿 실패 " + secretName, e);
        }
    }

    public String getSecretField(String fieldName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(getSecret());
            return root.path(fieldName).asText();
        } catch (Exception e) {
            throw new RuntimeException("파싱 실패, 필드명: " + fieldName + ", 시크릿명: " + secretName, e);
        }
    }
}
