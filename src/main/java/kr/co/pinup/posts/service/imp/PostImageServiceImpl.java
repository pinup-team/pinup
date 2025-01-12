package kr.co.pinup.posts.service.imp;


import jakarta.transaction.Transactional;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.model.repository.PostImageRepository;
import kr.co.pinup.posts.service.PostImageService;
import lombok.RequiredArgsConstructor;


import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class PostImageServiceImpl implements PostImageService {

    private final PostImageRepository postImageRepository;
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    // 이미지 파일을 S3에 업로드하고, URL을 DB에 저장하는 로직
    @Transactional
    @Override
    public List<PostImageEntity> savePostImages(List<MultipartFile> images, PostEntity post) {
        // 파일을 S3에 업로드하고 URL을 리스트로 받음
        List<String> imageUrls = uploadFiles(images);

        // 이미지 URL을 PostImageEntity로 변환하여 DB에 저장
        List<PostImageEntity> postImages = imageUrls.stream()
                .map(s3Url -> new PostImageEntity(post, s3Url))
                .collect(Collectors.toList());

        // DB에 이미지 정보 저장
        postImageRepository.saveAll(postImages);

        return postImages;
    }


    @Transactional
    @Override
    public void deleteAllByPost(Long postId) {
        List<PostImageEntity> postImages = postImageRepository.findByPostId(postId);

        // S3에서 이미지 삭제
        postImages.forEach(postImage -> {
            String fileUrl = postImage.getS3Url();
            String fileName = extractFileName(fileUrl); // URL에서 파일 이름 추출
            deleteFromS3(fileName);  // S3에서 파일 삭제
        });


        postImageRepository.deleteAllByPostId(postId);
    }

    @Override
    public void deleteSelectedImages(Long id, List<String> images) {
        // 해당 게시글의 이미지들 중 URL이 일치하는 이미지를 조회
        List<PostImageEntity> postImages = postImageRepository.findByPostIdAndS3UrlIn(id, images);

        // 삭제할 이미지들만 S3에서 삭제
        postImages.forEach(postImage -> {
            String fileUrl = postImage.getS3Url();
            String fileName = extractFileName(fileUrl); // URL에서 파일 이름 추출
            deleteFromS3(fileName);  // S3에서 파일 삭제
        });

        // DB에서 이미지 엔티티 삭제
        postImageRepository.deleteAll(postImages);
    }

    @Override
    public PostImageEntity findFirstImageByPostId(Long postId) {
        return postImageRepository.findTopByPostIdOrderByIdAsc(postId);
    }

    @Override
    public List<PostImageEntity> findImagesByPostId(Long postId) {
        return postImageRepository.findByPostId(postId);
    }

    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadFile) // 각 파일을 S3에 업로드하고 URL 반환
                .collect(Collectors.toList());
    }
    public String uploadFile(MultipartFile file) {
        try {
            // 고유한 파일 이름을 생성
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            log.info("Uploading file: {}", fileName);
            log.info("File size: {}", file.getSize());
            log.info("Bucket name: {}", bucketName);

            // S3에 파일 업로드
            try (InputStream inputStream = file.getInputStream()) {
                // 요청 준비
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(file.getContentType())
                        .build();

                // 요청 로그 찍기
                log.info("PutObjectRequest: {}", putObjectRequest);

                s3Client.putObject(
                        putObjectRequest,
                        RequestBody.fromInputStream(inputStream, file.getSize())
                );

                // 파일 URL 반환
                String fileUrl = s3Client.utilities()
                        .getUrl(builder -> builder.bucket(bucketName).key(fileName))
                        .toString();

                log.info("File uploaded to S3. URL: {}", fileUrl);
                return fileUrl;
            }
        } catch (IOException e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    // S3에서 파일 삭제
    public void deleteFromS3(String fileName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("S3에서 파일 삭제 실패", e);
        }
    }

    // URL에서 파일 이름만 추출하는 메서드
    private String extractFileName(String fileUrl) {
        // 예를 들어, URL이 "https://my-bucket.s3.amazonaws.com/uuid_filename.jpg"라면
        // "uuid_filename.jpg"를 반환
        String[] urlParts = fileUrl.split("/");
        return urlParts[urlParts.length - 1];
    }
}

