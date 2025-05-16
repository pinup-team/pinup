package kr.co.pinup.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.net.URI;

@Component
public class SecretsFetcher {

    private final String secretName;
    private final String region;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    public SecretsFetcher(
            @Value("${cloud.aws.secretsmanager.endpoint}") String endpoint,
            @Value("${cloud.aws.secretsmanager.region}") String region,
            @Value("${cloud.aws.secretsmanager.secret-name}") String secretName,
            @Value("${cloud.aws.credentials.accessKey") String accessKey,
            @Value("${cloud.aws.credentials.secretKey") String secretKey
    ) {

        this.secretName = secretName;
        this.region = region;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getSecret() {
        SecretsManagerClient client;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);


        if (endpoint != null && !endpoint.isBlank()) {
            client = SecretsManagerClient.builder()
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(credentialsProvider)
                    .build();
        } else {
            client = SecretsManagerClient.builder()
                    .region(Region.of(region))
                    .build();
        }

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
            throw new RuntimeException("파싱 실패, 핊드명: " + fieldName + ", 시크릿명: " + secretName, e);
        }
    }
}
