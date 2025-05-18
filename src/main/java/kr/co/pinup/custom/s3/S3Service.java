package kr.co.pinup.custom.s3;

import kr.co.pinup.custom.s3.exception.s3.ImageDeleteFailedException;
import kr.co.pinup.custom.s3.exception.s3.ImageUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    public String uploadFile(MultipartFile file, String pathPrefix) {
        try {
            String fileName = pathPrefix + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(
                        putObjectRequest,
                        RequestBody.fromInputStream(inputStream, file.getSize())
                );

                return s3Client.utilities()
                        .getUrl(builder -> builder.bucket(bucketName).key(fileName))
                        .toString();
            }
        } catch (IOException e) {
            throw new ImageUploadException("파일 업로드 실패", e);
        } catch (Exception e) {
            throw new ImageUploadException("파일 업로드 중 예기치 않은 오류 발생", e);
        }
    }

    public void deleteFromS3(String fileName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
        } catch (SdkClientException e) {
            throw new ImageDeleteFailedException("S3 클라이언트 오류 발생: " + fileName, e);
        } catch (Exception e) {
            throw new ImageDeleteFailedException("S3에서 파일 삭제 실패: " + fileName, e);
        }
    }

    public String extractFileName(String fileUrl) {
        String[] urlParts = fileUrl.split("/");
        return urlParts[urlParts.length - 1];
    }

}

