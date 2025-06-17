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

import java.util.Collections;
import java.util.List;

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
        Member member = Member.builder().nickname("행복한 돼지").build();
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
    void savePostImages_whenNoImagesProvided_thenThrowsException() {
        // Given
        CreatePostImageRequest request = CreatePostImageRequest.builder().images(null).build();

        // When & Then
        assertThrows(PostImageNotFoundException.class,
                () -> postImageService.savePostImages(request, mockPost));
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
    @DisplayName("전체 삭제 실패 - S3 삭제 오류")
    void deleteAllImages_whenS3Fails_thenThrowsException() {
        // Given
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn("img.jpg");
        doThrow(new ImageDeleteFailedException("fail")).when(s3Service).deleteFromS3("img.jpg");

        // When & Then
        assertThrows(PostImageDeleteFailedException.class,
                () -> postImageService.deleteAllByPost(mockPost.getId()));
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
    @DisplayName("선택 이미지 삭제 실패 - S3 삭제 오류")
    void deleteSelectedImages_whenS3Fails_thenThrowsException() {
        // Given
        String url = "https://s3.com/img.jpg";
        String file = "img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostIdAndS3UrlIn(mockPost.getId(), List.of(url)))
                .thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn(file);
        doThrow(new ImageDeleteFailedException("fail")).when(s3Service).deleteFromS3("post/"+file);

        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(List.of(url)).build();

        // When & Then
        assertThrows(ImageDeleteFailedException.class,
                () -> postImageService.deleteSelectedImages(mockPost.getId(), request));
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
}
