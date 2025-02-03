package kr.co.pinup.postImages.service;


import jakarta.transaction.Transactional;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.exception.postimage.PostImageUploadException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageService  {

    private final PostImageRepository postImageRepository;
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    @Transactional
    public List<PostImage> savePostImages(PostImageRequest postImageRequest, Post post) {
        if (postImageRequest.getImages() == null || postImageRequest.getImages().isEmpty()) {
            throw new PostImageUploadException("업로드할 이미지가 없습니다.");
        }

        List<String> imageUrls = uploadFiles(postImageRequest.getImages());

        List<PostImage> postImages = imageUrls.stream()
                .map(s3Url -> new PostImage(post, s3Url))
                .collect(Collectors.toList());

        try {
            postImageRepository.saveAll(postImages);
        } catch (Exception e) {
            throw new PostImageUploadException("이미지 저장 실패", e);
        }

        return postImages;
    }

    @Transactional
    public void deleteAllByPost(Long postId) {
        List<PostImage> postImages = postImageRepository.findByPostId(postId);
        log.info("삭제할 이미지 : , {}",postImages);

        if (postImages.isEmpty()) {
            log.info("삭제할 이미지가 없습니다. 게시글 ID: {}", postId);
            return;
        }

        postImages.forEach(postImage -> {
            String fileUrl = postImage.getS3Url();
            String fileName = extractFileName(fileUrl);
            try {
                deleteFromS3(fileName);
            } catch (SdkClientException e) {
                throw new PostImageDeleteFailedException("S3 클라이언트 오류 발생: " + fileName, e);
            } catch (Exception e) {
                throw new PostImageDeleteFailedException("S3에서 이미지 삭제 실패: " + fileName, e);
            }
        });

        try {
            postImageRepository.deleteAllByPostId(postId);
        } catch (Exception e) {
            throw new PostImageDeleteFailedException("게시글 ID " + postId + "의 이미지 삭제 실패", e);
        }
    }


    public void deleteSelectedImages(Long postId, PostImageRequest postImageRequest) {

        List<String> imagesToDelete = postImageRequest.getImagesToDelete();

        if (imagesToDelete != null && !imagesToDelete.isEmpty()) {

            List<PostImage> postImages = postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete);

            postImages.forEach(postImage -> {
                String fileUrl = postImage.getS3Url();
                String fileName = extractFileName(fileUrl);
                try {
                    deleteFromS3(fileName);
                } catch (Exception e) {
                    throw new PostImageDeleteFailedException("S3에서 이미지 삭제 실패: " + fileName, e);
                }
            });

            try {

                postImageRepository.deleteAll(postImages);
            } catch (Exception e) {
                throw new PostImageDeleteFailedException("선택한 이미지 삭제 실패", e);
            }
        } else {
            throw new PostImageNotFoundException("삭제할 이미지 URL이 없습니다.");
        }
    }


    public PostImage findFirstImageByPostId(Long postId) {
        return postImageRepository.findTopByPostIdOrderByIdAsc(postId);
    }


    public List<PostImageResponse> findImagesByPostId(Long postId) {
        List<PostImage> postImages = postImageRepository.findByPostId(postId);
        return postImages.stream()
                .map(PostImageResponse::from)
                .collect(Collectors.toList());
    }

    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }


    private String uploadFile(MultipartFile file) {
        try {

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

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
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new PostImageUploadException("파일 업로드 실패", e);
        } catch (Exception e) {
            log.error("Unexpected error occurred during file upload: {}", e.getMessage(), e);
            throw new PostImageUploadException("파일 업로드 중 예기치 않은 오류 발생", e);
        }
    }


    public void deleteFromS3(String fileName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
        } catch (SdkClientException e) {
            throw new PostImageDeleteFailedException("S3 클라이언트 오류 발생: " + fileName, e);
        } catch (Exception e) {
            throw new PostImageDeleteFailedException("S3에서 파일 삭제 실패: " + fileName, e);
        }
    }


    private String extractFileName(String fileUrl) {
        String[] urlParts = fileUrl.split("/");
        return urlParts[urlParts.length - 1];
    }
}

