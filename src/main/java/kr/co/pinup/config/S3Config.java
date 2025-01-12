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
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        // LocalStack 엔드포인트가 설정되어 있으면 엔드포인트를 지정
        if (endpoint != null && !endpoint.isEmpty()) {
            return S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1)  // LocalStack에서 사용하는 리전 (US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials)) // LocalStack에서 사용하는 가짜 자격 증명
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(2)) // API 호출 타임아웃 설정
                            .apiCallAttemptTimeout(Duration.ofSeconds(30)) // 각 시도에 대한 타임아웃 설정
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
