package kr.co.pinup.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")  // application-test.yml 활성화
public class S3ConfigTest {

    @Autowired
    private S3Client s3Client;  // 기본 S3Client로 testS3Client가 주입됨

    private final String bucketName = "pinup";
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

        // 업로드 후 잠시 대기하여 파일이 완전히 업로드되도록 합니다.
        Thread.sleep(1000);  // 1초 대기 (업로드가 완료되는데 충분한 시간으로 조정)

        // 업로드된 파일을 S3에서 가져옵니다.
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        // S3에서 파일을 가져오기
        ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

        // 파일 내용을 바이트 배열로 읽고, UTF-8로 변환합니다.
        String uploadedContent = new String(responseInputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 파일 내용이 예상대로 업로드되었는지 확인합니다.
        assertTrue(uploadedContent.contains(fileContent), "업로드된 파일의 내용이 예상과 다릅니다.");
    }


    @Test
    public void testFileExistsInS3() throws InterruptedException {
        // 업로드된 파일이 S3에 존재하는지 확인합니다.
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .build(),
                RequestBody.fromString(fileContent));

        // 파일이 S3에 존재할 때까지 기다리기
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
            // S3에서 객체의 메타데이터를 확인하여 존재 여부를 체크
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(keyName).build());
            return true;  // 객체가 존재하면 true 반환
        } catch (S3Exception e) {
            // 객체가 존재하지 않으면 false 반환
            if (e.statusCode() == 404) {
                System.err.println("파일을 찾을 수 없음: " + e.getMessage());
                return false;
            }
            // 그 외의 예외는 다른 오류로 처리
            System.err.println("파일 존재 여부 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    @AfterEach
    public void tearDown() {
        // 테스트 후 업로드한 파일 삭제
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build());
        } catch (S3Exception e) {
            // 예외가 발생해도 테스트가 실패하지 않도록 처리
            System.err.println("파일 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    @TestConfiguration
    static class S3TestConfig {

        @Bean
        @Primary  // 기본 S3Client로 설정
        public S3Client testS3Client() {
            return S3Client.builder()
                    .endpointOverride(URI.create("http://192.168.45.32:4566"))  // LocalStack 엔드포인트 설정
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .build();
        }
    }
}
