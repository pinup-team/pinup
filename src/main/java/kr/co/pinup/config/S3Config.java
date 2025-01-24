package kr.co.pinup.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;


import java.net.URI;
import java.time.Duration;

import software.amazon.awssdk.regions.Region;


@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.accessKey}")
    String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    String secretKey;

    @Value("${cloud.aws.region.static}")
    String region;

    @Value("${cloud.aws.s3.endpoint}")
    String endpoint;

    @Bean
    public S3Client s3Client() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        if (endpoint != null && !endpoint.isEmpty()) {
            return S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(2))
                            .apiCallAttemptTimeout(Duration.ofSeconds(30))
                            .build()) // 타임아웃 설정
                    .build();
        }

        // 일반적인 AWS 리전 설정
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
