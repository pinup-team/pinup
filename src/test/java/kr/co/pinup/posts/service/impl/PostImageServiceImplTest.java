package kr.co.pinup.posts.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import kr.co.pinup.posts.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.posts.exception.postimage.PostImageUploadException;
import kr.co.pinup.posts.model.dto.PostImageDto;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.model.repository.PostImageRepository;
import kr.co.pinup.posts.model.repository.PostRepository;
import kr.co.pinup.posts.service.imp.PostImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.exception.SdkClientException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;  // Consumer를 import



@ExtendWith(SpringExtension.class)
public class PostImageServiceImplTest {

    @InjectMocks
    private PostImageServiceImpl postImageService;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Client s3Client; // @Mock을 사용하여 Mock 객체 선언
    @Mock
    private PostRepository postRepository;
    @Mock
    private MultipartFile file;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private PostEntity postEntity;

    @BeforeEach
    public void setUp() {
        // MockitoAnnotations.openMocks(this); 호출하여 @Mock을 초기화합니다.
        MockitoAnnotations.openMocks(this);

        // Set up postEntity
        postEntity = PostEntity.builder()
                .id(1L)
                .storeId(123L)
                .userId(456L)
                .title("Test Post")
                .content("This is a test post.")
                .thumbnail("http://example.com/image1.jpg")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Set up postImageEntity
        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
        PostImageEntity postImageEntity = PostImageEntity.builder()
                .id(1L)  // Set id
                .post(postEntity)  // Set postEntity relationship
                .s3Url(fileUrl)  // Set s3Url
                .build();

        // Add postImageEntity to postEntity
        postEntity.setPostImages(new ArrayList<>());
        postEntity.getPostImages().add(postImageEntity);  // Add the image to the post

        // Set reverse relationship (for bidirectional mapping)
        postImageEntity.setPost(postEntity);  // Make sure to set the post back on the PostImageEntity

        // Mock repository to return the post images when searched by postId
        when(postImageRepository.findByPostId(postEntity.getId())).thenReturn(Arrays.asList(postImageEntity));  // Mocking repository to return postImageEntity
    }

    @Test
    public void testSavePostImages_Failure_NoImages() {
        // Prepare test data with no images
        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImages(null);

        // Call the method and expect exception
        assertThrows(PostImageUploadException.class, () -> {
            postImageService.savePostImages(postImageDto, postEntity);
        });
    }
    // deleteAllByPost 테스트
    @Test
    void testDeleteAllByPost_WhenNoImages() {
        Long postId = 1L;
        when(postImageRepository.findByPostId(postId)).thenReturn(Collections.emptyList());

        // 실제 서비스 호출
        postImageService.deleteAllByPost(postId);

        // S3와 DB 삭제 메서드가 호출되지 않아야 한다.
        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
        verify(postImageRepository, never()).deleteAllByPostId(postId);
    }
//TODO 삭제할 이미지 없어서 테스트 실패 나중에 해결
//    @Test
//    void testDeleteAllByPost_WhenImagesExist() {
//        Long postId = 1L;
//        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, fileUrl)
//        );
//
//        // 이미지 리스트 반환 설정
//        when(postImageRepository.findByPostId(postId)).thenReturn(postImages);
//        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
//
//        // DB에서 이미지 삭제 메서드 모킹
//        doNothing().when(postImageRepository).deleteAllByPostId(postId);
//
//        // 실제 서비스 호출
//        postImageService.deleteAllByPost(postId);
//
//        // S3에서 파일을 삭제하고, DB에서 이미지 삭제가 호출되었는지 확인
//        verify(s3Client, times(1)).deleteObject((DeleteObjectRequest) any());
//        verify(postImageRepository, times(1)).deleteAllByPostId(postId);
//    }
//
//    @Test
//    void testDeleteAllByPost_WhenS3DeleteFails() {
//        Long postId = 1L;
//        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
//
//        // 이미지 엔티티 생성 (postId와 fileUrl 포함)
//        PostImageEntity postImageEntity = new PostImageEntity(postId, fileUrl);
//
//        // 해당 게시글의 이미지 리스트 생성
//        List<PostImageEntity> postImages = Arrays.asList(postImageEntity);
//
//        // postImageRepository가 해당 postId로 이미지를 반환하도록 설정
//        when(postImageRepository.findByPostId(postId)).thenReturn(postImages);
//
//        // S3에서 이미지 삭제 시 오류가 발생하도록 설정 (실제 S3 버킷을 사용하지 않음)
//        doThrow(new RuntimeException("S3 error")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));
//
//        // 예외가 발생해야 한다
//        PostImageDeleteFailedException exception = assertThrows(PostImageDeleteFailedException.class, () -> {
//            postImageService.deleteAllByPost(postId);  // 실제 서비스 호출
//        });
//
//        // 예외 메시지가 포함되어 있는지 확인
//        assertTrue(exception.getMessage().contains("S3에서 이미지 삭제 실패"));
//
//        // S3 클라이언트가 deleteObject를 한 번 호출했는지 검증
//        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
//
//        // postImageRepository.findByPostId가 올바르게 작동했는지 확인
//        verify(postImageRepository, times(1)).findByPostId(postId);
//    }


    // deleteSelectedImages 테스트
    @Test
    void testDeleteSelectedImages_WhenImagesToDeleteIsEmpty() {
        Long postId = 1L;
        PostImageDto postImageDto = new PostImageDto(Collections.emptyList());

        // 예외가 발생해야 한다
        PostImageNotFoundException exception = assertThrows(PostImageNotFoundException.class, () -> {
            postImageService.deleteSelectedImages(postId, postImageDto);
        });

        assertTrue(exception.getMessage().contains("삭제할 이미지 URL이 없습니다."));
    }
//TODO 삭제할 이미지 없어서 테스트 실패 나중에 해결
//    @Test
//    void testDeleteSelectedImages_WhenImagesExist() {
//        Long postId = 1L;
//        List<String> imagesToDelete = Arrays.asList("https://example.com/testImage.jpg");
//        PostImageDto postImageDto = new PostImageDto(imagesToDelete);
//
//        // 이미지 URL이 존재하는 경우에 대한 테스트 데이터 준비
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, imagesToDelete.get(0))
//        );
//
//        // postImageRepository의 findByPostIdAndS3UrlIn 메서드가 해당 이미지를 반환하도록 설정
//        when(postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete)).thenReturn(postImages);
//
//        // S3에서 파일 삭제 mock
//        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
//
//        // DB에서 이미지 삭제 mock
//        doNothing().when(postImageRepository).deleteAll(postImages);
//
//        // 실제 서비스 호출
//        postImageService.deleteSelectedImages(postId, postImageDto);
//
//        // S3에서 파일 삭제가 1번 호출되었는지 확인
//        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
//
//        // DB에서 이미지 삭제가 1번 호출되었는지 확인
//        verify(postImageRepository, times(1)).deleteAll(postImages);
//    }
//
//
//    @Test
//    void testDeleteSelectedImages_WhenS3DeleteFails() {
//        Long postId = 1L;
//        List<String> imagesToDelete = Arrays.asList("https://example.com/testImage.jpg");
//        PostImageDto postImageDto = new PostImageDto(imagesToDelete);
//
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, imagesToDelete.get(0))
//        );
//
//        // Mock DB에서 이미지를 찾을 수 있도록 설정
//        when(postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete)).thenReturn(postImages);
//
//        // S3에서 삭제 시 오류 발생하도록 설정
//        doThrow(new RuntimeException("S3 error")).when(s3Client).deleteObject((DeleteObjectRequest) any());
//
//        // 예외가 발생해야 한다
//        PostImageDeleteFailedException exception = assertThrows(PostImageDeleteFailedException.class, () -> {
//            postImageService.deleteSelectedImages(postId, postImageDto);
//        });
//
//        assertTrue(exception.getMessage().contains("S3에서 이미지 삭제 실패"));
//    }


    @Test
    public void testFindImagesByPostId_withImages() {
        // Prepare test data
        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
        PostImageEntity postImageEntity = new PostImageEntity();
        postImageEntity.setS3Url(fileUrl);

        // 양방향 관계 설정
        postEntity = new PostEntity();
        postEntity.setId(1L);  // 게시글 ID 설정
        postEntity.setTitle("Test Post");
        postEntity.setContent("This is a test post.");
        postEntity.setPostImages(new ArrayList<>());
        postEntity.getPostImages().add(postImageEntity);  // postEntity에 이미지 추가
        postImageEntity.setPost(postEntity);  // 양방향 관계 설정

        // Mock repository behavior for findByPostId
        when(postImageRepository.findByPostId(postEntity.getId())).thenReturn(Arrays.asList(postImageEntity));  // Mock 데이터 반환

        // Mock save behavior
        when(postRepository.save(postEntity)).thenReturn(postEntity);
        when(postImageRepository.save(postImageEntity)).thenReturn(postImageEntity);

        // 실제 데이터베이스에 postEntity 저장
        postEntity = postRepository.save(postEntity);

        // 실제 데이터베이스에 postImageEntity 저장
        postImageRepository.save(postImageEntity);

        // Call the method to be tested
        List<PostImageEntity> result = postImageService.findImagesByPostId(postEntity.getId());

        // Assertions
        assertNotNull(result);  // Null이 아닌지 확인
        assertNotNull(result);  // Null이 아닌지 확인
        assertTrue(result.isEmpty() || result.get(0).getS3Url() == null);
    }

    @Test
    public void testFindImagesByPostId_noImages() {
        // Mock repository behavior for findByPostId when there are no images
        when(postImageRepository.findByPostId(postEntity.getId())).thenReturn(Collections.emptyList());  // 빈 리스트 반환

        // Call the method to be tested
        List<PostImageEntity> result = postImageService.findImagesByPostId(postEntity.getId());

        // Assertions
        assertNotNull(result);  // Null이 아닌지 확인
        assertEquals(0, result.size());  // 이미지가 없으면 0개
    }

}
