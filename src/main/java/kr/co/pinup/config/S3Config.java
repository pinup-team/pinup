package kr.co.pinup.config;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final AppLogger appLogger;

    @Value("${cloud.aws.credentials.accessKey}")
    String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    String secretKey;

    @Value("${cloud.aws.region.static}")
    String region;

    @Value("${cloud.aws.s3.endpoint:}")
    String endpoint;

    @Bean
    public S3Client s3Client() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        try {
            if (endpoint != null && !endpoint.isEmpty()) {
                appLogger.info(new InfoLog("S3Client 생성 - 커스텀 엔드포인트 사용")
                        .addDetails("endpoint", endpoint));
                return S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.US_EAST_1)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                        .overrideConfiguration(ClientOverrideConfiguration.builder()
                                .apiCallTimeout(Duration.ofMinutes(2))
                                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                .build())
                        .build();
            }
            appLogger.info(new InfoLog("S3Client 생성 - 기본 리전 사용")
                    .addDetails("region", region));
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();

        } catch (Exception e) {
            appLogger.error(new ErrorLog("S3Client 생성 실패", e)
                    .setStatus("500")
                    .addDetails("endpoint", endpoint)
                    .addDetails("region", region));
            throw e;
        }
    }
}
