package kr.co.pinup.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.net.URI;

@Component
public class SecretsManagerUtil {

    private final SecretsManagerClient client;

    public SecretsManagerUtil() {
        this.client = SecretsManagerClient.builder()
                .region(Region.of("us-east-1"))
                .endpointOverride(URI.create("http://localhost:4566"))
                .build();
    }

    public String getSecretField(String secretName, String fieldName) {
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        String secretJson = getSecretValueResponse.secretString();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(secretJson);
            return root.path(fieldName).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse field '" + fieldName + "' from secret '" + secretName + "'", e);
        }
    }
}
