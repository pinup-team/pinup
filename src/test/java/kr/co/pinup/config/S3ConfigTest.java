package kr.co.pinup.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class S3ConfigTest {

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    String bucketName;

    private final String keyName = "test-file.txt";
    private final String fileContent = "This is a test file.";

    @BeforeEach
    public void setUp() {
        // LocalStack에서 S3 버킷을 생성
        if (!doesBucketExist(bucketName)) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    @Test
    public void testUploadFileToS3() throws IOException, InterruptedException {
        // 파일을 S3에 업로드합니다.
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .build(),
                RequestBody.fromString(fileContent));


        Thread.sleep(1000);


        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

        String uploadedContent = new String(responseInputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(uploadedContent.contains(fileContent), "업로드된 파일의 내용이 예상과 다릅니다.");
    }


    @Test
    public void testFileExistsInS3() throws InterruptedException {

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .build(),
                RequestBody.fromString(fileContent));

        boolean fileExists = false;
        int retries = 0;
        while (!fileExists && retries < 10) {
            fileExists = doesObjectExist(bucketName, keyName);
            if (!fileExists) {
                retries++;
                Thread.sleep(500);  // 500ms 대기 후 재시도
            }
        }

        assertTrue(fileExists, "파일이 S3에 존재하지 않습니다.");
    }


    private boolean doesBucketExist(String bucketName) {
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            return response.buckets().stream().anyMatch(b -> b.name().equals(bucketName));
        } catch (S3Exception e) {
            // 예외 발생 시 bucket이 존재하지 않음
            System.err.println("버킷 존재 여부 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    private boolean doesObjectExist(String bucketName, String keyName) {
        try {

            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(keyName).build());
            return true;
        } catch (S3Exception e) {

            if (e.statusCode() == 404) {
                System.err.println("파일을 찾을 수 없음: " + e.getMessage());
                return false;
            }

            System.err.println("파일 존재 여부 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build());
        } catch (S3Exception e) {
            System.err.println("파일 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    @TestConfiguration
    static class S3TestConfig {
        @Value("${cloud.aws.s3.endpoint}")
        String endpoint;

        @Bean
        @Primary
        public S3Client testS3Client() {
            return S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .build();
        }
    }
}
