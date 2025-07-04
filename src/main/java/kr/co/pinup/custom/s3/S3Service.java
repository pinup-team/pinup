package kr.co.pinup.custom.s3;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.s3.exception.ImageDeleteFailedException;
import kr.co.pinup.custom.s3.exception.ImageUploadException;
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
    private final AppLogger appLogger;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String pathPrefix) {
        String fileName = pathPrefix + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
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

                String url = s3Client.utilities()
                        .getUrl(builder -> builder.bucket(bucketName).key(fileName))
                        .toString();

                appLogger.info(new InfoLog("이미지 업로드 성공")
                        .setTargetId(fileName)
                        .addDetails("url", url));

                return url;
            }
        } catch (IOException e) {
            appLogger.error(new ErrorLog("파일 업로드 실패 (IO)", e)
                    .setStatus("500")
                    .addDetails("file", fileName));
            throw new ImageUploadException("파일 업로드 실패", e);
        } catch (Exception e) {
            appLogger.error(new ErrorLog("파일 업로드 실패 (기타)", e)
                    .setStatus("500")
                    .addDetails("file", fileName));
            throw new ImageUploadException("파일 업로드 중 예기치 않은 오류 발생", e);
        }
    }

    public void deleteFromS3(String fileName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
            appLogger.info(new InfoLog("이미지 삭제 성공").setTargetId(fileName));
        } catch (SdkClientException e) {
            appLogger.error(new ErrorLog("S3 클라이언트 오류 발생", e)
                    .setTargetId(fileName)
                    .setStatus("500"));
            throw new ImageDeleteFailedException("S3 클라이언트 오류 발생: " + fileName, e);

        } catch (Exception e) {
            appLogger.error(new ErrorLog("S3에서 파일 삭제 실패", e)
                    .setTargetId(fileName)
                    .setStatus("500"));
            throw new ImageDeleteFailedException("S3에서 파일 삭제 실패: " + fileName, e);
        }
    }

    public String extractFileName(String fileUrl) {
        String[] urlParts = fileUrl.split("/");
        return urlParts[urlParts.length - 1];
    }

}

