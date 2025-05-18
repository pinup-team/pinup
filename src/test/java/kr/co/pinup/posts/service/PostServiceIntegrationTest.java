package kr.co.pinup.posts.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageUpdateCountException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(PostServiceIntegrationTest.TestMockConfig.class)
public class PostServiceIntegrationTest {

    @Autowired private PostService postService;
    @Autowired private PostRepository postRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;

    @Autowired private MemberService memberService;
    @Autowired private S3Service s3Service;
    @Autowired private PostImageService postImageService;

    private Member mockMember;
    private Post mockPost;

    @TestConfiguration
    static class TestMockConfig {
        @Bean public S3Service s3Service() { return mock(S3Service.class); }
        @Bean public PostImageService postImageService() { return mock(PostImageService.class); }
        @Bean public MemberService memberService() { return mock(MemberService.class); }
    }

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("Category"));
        Location location = locationRepository.save(new Location("Loc", "123", "State", "District", 0.0, 0.0, "Addr", "Detail"));

        Store store = Store.builder().name("Store").description("desc").category(category).location(location).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10)).status(Status.RESOLVED).build();
        store = storeRepository.save(store);

        mockMember = Member.builder().email("test@naver.com").nickname("행복한돼지").name("test").providerId("pid").providerType(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();
        mockMember = memberRepository.save(mockMember);

        mockPost = Post.builder().title("제목").content("내용").store(store).member(mockMember).build();
        mockPost = postRepository.save(mockPost);

        postImageRepository.save(PostImage.builder().post(mockPost).s3Url("url.jpg").build());
    }

    @Test
    @DisplayName("게시물 생성 - 이미지 포함")
    void createPost_whenValidRequestWithImages_thenSuccess() {
        // Given
        List<MultipartFile> validFiles = List.of(
                new MockMultipartFile("img", "test1.jpg", "image/jpeg", "data1".getBytes()),
                new MockMultipartFile("img", "test2.jpg", "image/jpeg", "data2".getBytes())
        );

        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(validFiles)
                .build();
        MemberInfo info = new MemberInfo(mockMember.getNickname(), mockMember.getProviderType(), mockMember.getRole());
        CreatePostRequest req = new CreatePostRequest(mockPost.getStore().getId(), "New Title", "New Content");

        when(s3Service.uploadFile(any(), anyString())).thenReturn("https://s3.com/test.jpg");
        when(s3Service.extractFileName(any())).thenReturn("test.jpg");

        // When
        PostResponse res = postService.createPost(info, req, request);

        // Then
        assertNotNull(res);
        assertEquals("New Title", res.title());
    }

    @Test
    @DisplayName("게시물 조회 - 게시물 존재")
    void getPostById_whenPostExists_thenReturnsPostDetail() {
        // When
        PostResponse res = postService.getPostById(mockPost.getId(), false);

        // Then
        assertNotNull(res);
        assertEquals("제목", res.title());
    }

    @Test
    @DisplayName("게시물 삭제 - 이미지 포함")
    void deletePost_whenExistingPost_thenDeletesPostAndImages() {
        // When
        postService.deletePost(mockPost.getId());

        // Then
        verify(postImageService).deleteAllByPost(mockPost.getId());
    }

    @Test
    @DisplayName("게시물 목록 조회 - 존재하는 Store ID")
    void getPostsByStore_whenPostsExist_thenReturnsPostList() {
        // When
        List<PostResponse> list = postService.findByStoreId(mockPost.getStore().getId(), false);

        // Then
        assertFalse(list.isEmpty());
        assertEquals("제목", list.get(0).title());
    }

    @Test
    @DisplayName("게시물 수정 - 이미지 삭제 및 업로드 포함")
    void updatePost_whenImagesDeletedAndUploaded_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Content");

        when(postImageService.findImagesByPostId(mockPost.getId())).thenReturn(
                List.of(
                        PostImageResponse.builder().id(1L).postId(mockPost.getId()).s3Url("img1").build(),
                        PostImageResponse.builder().id(2L).postId(mockPost.getId()).s3Url("remain_url").build()
                ),
                List.of(
                        PostImageResponse.builder().id(2L).postId(mockPost.getId()).s3Url("remain_url").build()
                )
        );

        doNothing().when(postImageService).deleteSelectedImages(eq(mockPost.getId()), any());

        MultipartFile img = new MockMultipartFile("img", "file.jpg", "image/jpeg", "data".getBytes());
        when(postImageService.savePostImages(any(), eq(mockPost)))
                .thenReturn(List.of(new PostImage(mockPost, "new_url")));

        // When
        PostResponse result = postService.updatePost(mockPost.getId(), req, new MultipartFile[]{img}, List.of("img1"));

        // Then
        assertNotNull(result);
        assertEquals("remain_url", result.thumbnail());
    }

    @Test
    @DisplayName("게시물 수정 - 이미지 삭제만")
    void updatePost_whenImagesDeletedOnly_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Content");

        when(postImageService.findImagesByPostId(mockPost.getId())).thenReturn(
                List.of(
                        PostImageResponse.builder().id(1L).postId(mockPost.getId()).s3Url("img1").build(),
                        PostImageResponse.builder().id(2L).postId(mockPost.getId()).s3Url("img2").build(),
                        PostImageResponse.builder().id(3L).postId(mockPost.getId()).s3Url("remaining_image_url.jpg").build()
                ),
                List.of(
                        PostImageResponse.builder().id(2L).postId(mockPost.getId()).s3Url("img2").build(),
                        PostImageResponse.builder().id(3L).postId(mockPost.getId()).s3Url("remaining_image_url.jpg").build()
                )
        );

        doNothing().when(postImageService).deleteSelectedImages(eq(mockPost.getId()), any());

        // When
        PostResponse result = postService.updatePost(mockPost.getId(), req, new MultipartFile[0], List.of("img1"));

        // Then
        assertNotNull(result);
        assertEquals("Updated", result.title());
    }

    @Test
    @DisplayName("게시물 수정 - 이미지만 업로드")
    void updatePost_whenImagesUploadedOnly_thenSuccess() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Content");

        MultipartFile[] imgs = {
                new MockMultipartFile("img", "file1.jpg", "image/jpeg", "data1".getBytes()),
                new MockMultipartFile("img", "file2.jpg", "image/jpeg", "data2".getBytes())
        };

        PostImage postImage1 = PostImage.builder()
                .post(mockPost)
                .s3Url("https://s3.com/file1.jpg")
                .build();

        PostImage postImage2 = PostImage.builder()
                .post(mockPost)
                .s3Url("https://s3.com/file2.jpg")
                .build();

        List<PostImage> uploadedImages = List.of(postImage1, postImage2);
        when(postImageService.savePostImages(any(), eq(mockPost))).thenReturn(uploadedImages);

        List<PostImageResponse> uploadedResponses = List.of(
                PostImageResponse.from(postImage1),
                PostImageResponse.from(postImage2)
        );
        when(postImageService.findImagesByPostId(mockPost.getId())).thenReturn(uploadedResponses);

        // When
        PostResponse result = postService.updatePost(mockPost.getId(), req, imgs, List.of());

        // Then
        assertNotNull(result);
        assertEquals("https://s3.com/file1.jpg", result.thumbnail());
    }

    @Test
    @DisplayName("게시물 수정 실패 - 모든 이미지 삭제 후 예외 발생 (이미지 2장 미만)")
    void updatePost_whenAllImagesDeleted_thenThrowsException() {
        // Given
        UpdatePostRequest req = new UpdatePostRequest("Updated", "Content");

        postImageRepository.save(PostImage.builder()
                .post(mockPost)
                .s3Url("img1")
                .build());

        // When & Then
        assertThrows(PostImageUpdateCountException.class, () -> {
            postService.updatePost(mockPost.getId(), req, new MultipartFile[0], List.of("img1"));
        });
    }

}
