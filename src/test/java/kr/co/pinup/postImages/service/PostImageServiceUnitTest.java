package kr.co.pinup.postImages.service;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.custom.s3.exception.ImageDeleteFailedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.model.dto.UpdatePostImageRequest;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.stores.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostImageServiceUnitTest {

    @InjectMocks
    private PostImageService postImageService;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private AppLogger appLogger;

    private Post mockPost;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().nickname("행복한 돼지"+ UUID.randomUUID()).build();
        Store store = Store.builder().name("Test Store").build();

        mockPost = Post.builder()
                .member(member)
                .store(store)
                .title("title")
                .content("content")
                .build();
        ReflectionTestUtils.setField(mockPost, "id", 1L);
    }

    @Test
    @DisplayName("이미지 저장 실패 - 이미지가 없는 경우")
    void uploadImagesOnly_whenNoImages_thenThrows() {
        CreatePostImageRequest req = CreatePostImageRequest.builder().images(null).build();
        assertThrows(PostImageNotFoundException.class, () -> postImageService.uploadImagesOnly(req));
    }

    @Test
    @DisplayName("전체 삭제 - 이미지가 없는 경우")
    void deleteAllImages_whenNoImagesExist_thenNoAction() {
        // Given
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(Collections.emptyList());

        // When
        postImageService.deleteAllByPost(mockPost.getId());

        // Then
        verify(s3Service, never()).deleteFromS3(anyString());
        verify(postImageRepository, never()).deleteAllByPostId(anyLong());
    }

    @Test
    @DisplayName("전체 삭제 실패 - S3 삭제 오류(quiet): 예외 미전파, afterCommit에서 S3 시도")
    void deleteAllImages_whenS3Fails_thenDoesNotThrow_andCallsS3AfterCommit() {
        // Given
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn("img.jpg");

        // 반드시 'post/' 프리픽스 포함해서 스텁
        doThrow(new ImageDeleteFailedException("fail"))
                .when(s3Service).deleteFromS3("post/img.jpg");

        // When & Then: 서비스 호출 시점에는 예외가 안 터짐
        assertDoesNotThrow(() -> runWithAfterCommit(
                () -> postImageService.deleteAllByPost(mockPost.getId())
        ));

        // afterCommit에서 실제로 호출됐는지 검증
        verify(s3Service).deleteFromS3("post/img.jpg");

    }


    @Test
    @DisplayName("전체 삭제 성공 - 이미지 존재")
    void deleteAllImages_whenImagesExist_thenSuccess() {
        // Given
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn("img.jpg");

        // When
        postImageService.deleteAllByPost(mockPost.getId());

        // Then
        verify(s3Service).deleteFromS3("post/img.jpg");
        verify(postImageRepository).deleteAllByPostId(mockPost.getId());
    }

    @Test
    @DisplayName("선택 이미지 삭제 실패 - 삭제 리스트 없음")
    void deleteSelectedImages_whenEmptyRequest_thenThrowsException() {
        // Given
        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(Collections.emptyList()).build();

        // When & Then
        assertThrows(PostImageNotFoundException.class,
                () -> postImageService.deleteSelectedImages(mockPost.getId(), request));
    }

    @Test
    @DisplayName("선택 이미지 삭제 성공")
    void deleteSelectedImages_whenValidRequest_thenSuccess() {
        // Given
        String url = "https://s3.com/img.jpg";
        String file = "img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostIdAndS3UrlIn(mockPost.getId(), List.of(url)))
                .thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn(file);

        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(List.of(url)).build();

        // When
        postImageService.deleteSelectedImages(mockPost.getId(), request);

        // Then
        verify(s3Service).deleteFromS3("post/"+file);
        verify(postImageRepository).deleteAll(List.of(img));
    }

    @Test
    @DisplayName("선택 이미지 삭제 실패 - S3 삭제 오류(quiet): 예외 미전파, afterCommit에서 S3 시도")
    void deleteSelectedImages_whenS3Fails_thenDoesNotThrow_andCallsS3AfterCommit() {
        // Given
        String url = "https://s3.com/img.jpg";
        String file = "img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostIdAndS3UrlIn(mockPost.getId(), List.of(url)))
                .thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn(file);

        // 프리픽스 주의!
        doThrow(new ImageDeleteFailedException("fail"))
                .when(s3Service).deleteFromS3("post/" + file);

        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(List.of(url)).build();

        // When & Then
        assertDoesNotThrow(() -> runWithAfterCommit(
                () -> postImageService.deleteSelectedImages(mockPost.getId(), request)
        ));

        verify(s3Service).deleteFromS3("post/" + file);
    }


    @Test
    @DisplayName("게시글 ID로 이미지 조회 - 이미지가 없는 경우")
    void getImagesByPostId_whenNoImages_thenReturnsEmptyList() {
        // Given
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(Collections.emptyList());

        // When
        List<PostImageResponse> result = postImageService.findImagesByPostId(mockPost.getId());

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("게시글 ID로 이미지 조회 - 이미지 존재")
    void getImagesByPostId_whenImagesExist_thenReturnsImageList() {
        // Given
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));

        // When
        List<PostImageResponse> result = postImageService.findImagesByPostId(mockPost.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(url, result.get(0).getS3Url());
    }

    private void runWithAfterCommit(Runnable call) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            call.run();
            // 서비스가 registerSynchronization 해놨다면 여기서 커밋 콜백을 직접 실행
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
                sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
