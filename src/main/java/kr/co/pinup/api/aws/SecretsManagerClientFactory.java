package kr.co.pinup.api.aws;

import kr.co.pinup.config.AwsCredentialsProperties;
import kr.co.pinup.config.AwsSecretsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class SecretsManagerClientFactory {

    private final AwsSecretsProperties secretsProperties;
    private final AwsCredentialsProperties credentialsProperties;

    public SecretsManagerClient create() {
        final SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder()
                .region(Region.of(secretsProperties.getRegion()));

        if ("prod".equals(credentialsProperties.getActiveProfile())) {
            return clientBuilder.build();
        }

        return clientBuilder
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                credentialsProperties.getAccessKey(),
                                credentialsProperties.getSecretKey()
                        )
                ))
                .endpointOverride(URI.create(secretsProperties.getEndpoint()))
                .build();
    }
}
