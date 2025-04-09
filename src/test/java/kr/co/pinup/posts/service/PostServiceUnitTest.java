package kr.co.pinup.posts.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageUpdateCountException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceUnitTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostImageService postImageService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StoreRepository storeRepository;

    private Member member;
    private Store store;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-id")
                .role(MemberRole.ROLE_USER)
                .build();

        store = Store.builder()
                .name("Test Store")
                .description("Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .status(Status.RESOLVED)
                .imageUrl("image.jpg")
                .category(new StoreCategory("Cat"))
                .location(new Location("Loc", "12345", "State", "Dist", 1.1, 2.2, "Addr", "Detail"))
                .build();
    }

    @Test
    @DisplayName("게시물 생성 - 이미지 포함")
    void createPost_whenValidRequestWithImages_thenSuccess() {
        // Given
        MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        CreatePostRequest req = new CreatePostRequest(1L, "New Post", "Content");
        List<MultipartFile> validFiles = List.of(
                new MockMultipartFile("img", "test1.jpg", "image/jpeg", "data1".getBytes()),
                new MockMultipartFile("img", "test2.jpg", "image/jpeg", "data2".getBytes())
        );

        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(validFiles)
                .build();

        Post post = Post.builder().store(store).member(member).title(req.title()).content(req.content()).build();

        when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postImageService.savePostImages(any(), any())).thenReturn(List.of(new PostImage(post, "image_url")));

        // When
        PostResponse result = postService.createPost(memberInfo, req, request);

        // Then
        assertEquals("New Post", result.title());
        assertEquals("image_url", result.thumbnail());
    }

    @Test
    @DisplayName("Store ID에 대한 게시물 목록 조회")
    void getPostsByStore_whenPostsExist_thenReturnsPostList() {
        // Given
        Post post = Post.builder().store(store).member(member).title("Post Title").content("Content").build();
        when(postRepository.findByStoreIdAndIsDeleted(1L, false)).thenReturn(List.of(post));

        // When
        List<PostResponse> result = postService.findByStoreId(1L, false);

        // Then
        assertEquals(1, result.size());
        assertEquals("Post Title", result.get(0).title());
    }

    @Test
    @DisplayName("게시물 ID로 게시물 조회")
    void getPostById_whenPostExists_thenReturnsPostDetail() {
        // Given
        Post post = Post.builder().store(store).member(member).title("Post Title").content("Content").build();
        when(postRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(post));

        // When
        PostResponse result = postService.getPostById(1L, false);

        // Then
        assertEquals("Post Title", result.title());
    }

    @Test
    @DisplayName("게시물 삭제 - 이미지 포함")
    void deletePost_whenExistingPost_thenSuccess() {
        // Given
        Post post = Post.builder().title("Post Title").content("Content").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // When
        postService.deletePost(1L);

        // Then
        verify(postImageService).deleteAllByPost(1L);
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("게시물 수정 - 이미지 삭제 및 업로드 포함")
    void updatePost_whenImagesDeletedAndUploaded_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
        Post post = Post.builder().title("Old").content("Old").build();
        List<String> toDelete = List.of("url1");

        when(postImageService.findImagesByPostId(1L)).thenReturn(
                List.of(
                        PostImageResponse.builder().id(1L).postId(1L).s3Url("url1").build(),
                        PostImageResponse.builder().id(2L).postId(1L).s3Url("remain_url").build()
                ),
                List.of(
                        PostImageResponse.builder().id(2L).postId(1L).s3Url("remain_url").build()
                )
        );

        doNothing().when(postImageService).deleteSelectedImages(eq(1L), any());

        MultipartFile[] newImages = {
                new MockMultipartFile("img", "new.jpg", "image/jpeg", "valid data".getBytes())
        };
        when(postImageService.savePostImages(any(), eq(post)))
                .thenReturn(List.of(new PostImage(post, "new_url")));

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        // When
        Post result = postService.updatePost(1L, req, newImages, toDelete);

        // Then
        assertEquals("Updated", result.getTitle());
        assertEquals("remain_url", result.getThumbnail());
    }

    @Test
    @DisplayName("게시물 수정 - 이미지만 업로드")
    void updatePost_whenImagesUploadedOnly_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
        Post post = Post.builder().title("Old").content("Old").build();

        when(postImageService.findImagesByPostId(1L)).thenReturn(List.of(
                PostImageResponse.builder().id(1L).postId(1L).s3Url("existing_url.jpg").build()
        ));

        MultipartFile[] images = {
                new MockMultipartFile("img", "upload1.jpg", "image/jpeg", "data1".getBytes())
        };

        when(postImageService.savePostImages(any(), eq(post)))
                .thenReturn(List.of(new PostImage(post, "new_url")));

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        // When
        Post result = postService.updatePost(1L, req, images, List.of());

        // Then
        assertEquals("Updated", result.getTitle());
        assertEquals("existing_url.jpg", result.getThumbnail()); // ✅ 기존 이미지 우선
    }

    @Test
    @DisplayName("게시물 수정 - 제목과 내용만 변경, 이미지 변경 없음")
    void updatePost_whenTitleAndContentUpdatedOnly_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
        Post post = Post.builder().title("Old").content("Old").build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenReturn(post);

        // When
        Post result = postService.updatePost(1L, req, new MultipartFile[0], List.of());

        // Then
        assertEquals("Updated", result.getTitle());
        verify(postImageService, never()).deleteSelectedImages(anyLong(), any());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 모든 이미지 삭제 후 예외 발생 (이미지 2장 미만)")
    void updatePost_whenAllImagesDeleted_thenThrowsUpdateCountException() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");

        Post post = Post.builder()
                .title("Old")
                .content("Old")
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        when(postImageService.findImagesByPostId(1L)).thenReturn(List.of(
                PostImageResponse.builder()
                        .id(1L)
                        .postId(1L)
                        .s3Url("url1")
                        .build()
        ));

        List<String> toDelete = List.of("url1");
        MultipartFile[] upload = new MultipartFile[0];

        // When & Then
        assertThrows(PostImageUpdateCountException.class, () ->
                postService.updatePost(1L, req, upload, toDelete)
        );
    }

    @Test
    @DisplayName("게시물 수정 - 이미지가 2장 미만이면 예외 발생")
    void updatePost_whenImageCountIsLessThanTwo_thenThrowException() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("제목", "내용");

        Post post = Post.builder().store(store).member(member).title("Old Title").content("Old Content").build();

        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postImageService.findImagesByPostId(post.getId()))
                .thenReturn(List.of(
                        PostImageResponse.builder()
                                .id(1L)
                                .postId(post.getId())
                                .s3Url("img1")
                                .build()
                ));

        List<String> toDelete = List.of("img1");
        MultipartFile[] upload = new MultipartFile[0];

        // When & Then
        assertThrows(PostImageUpdateCountException.class, () ->
                postService.updatePost(post.getId(), req, upload, toDelete)
        );
    }

}
