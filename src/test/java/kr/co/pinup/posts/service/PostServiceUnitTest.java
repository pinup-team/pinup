package kr.co.pinup.posts.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
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
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
public class
PostServiceUnitTest {

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

    @Nested
    @DisplayName("게시물 생성(createPost)")
    class CreatePost {
        @Test
        @DisplayName("게시물 생성 - 이미지 포함 시 성공")
        void createPost_whenValidRequestWithImages_thenSuccess() {
            // Given
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(1L, "New Post", "Content");
            List<MultipartFile> validFiles = List.of(
                    new MockMultipartFile("img", "test1.jpg", "image/jpeg", "data1".getBytes()),
                    new MockMultipartFile("img", "test2.jpg", "image/jpeg", "data2".getBytes())
            );
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder().images(validFiles).build();
            Post post = Post.builder().store(store).member(member).title(req.title()).content(req.content()).build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
            when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            when(postImageService.savePostImages(any(), eq(post))).thenReturn(
                    List.of(new PostImage(post, "thumb.jpg"))
            );

            // When
            PostResponse response = postService.createPost(memberInfo, req, imageReq);

            // Then
            assertEquals("New Post", response.title());
            assertEquals("thumb.jpg", response.thumbnail());
        }

        @Test
        @DisplayName("게시물 생성 - 닉네임 조회 실패시 예외 발생")
        void createPost_whenMemberNotFound_thenThrowsException() {
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(1L, "제목", "내용");
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder()
                    .images(List.of(new MockMultipartFile("img", "file.jpg", "image/jpeg", "data".getBytes())))
                    .build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.empty());

            assertThrows(MemberNotFoundException.class, () ->
                    postService.createPost(memberInfo, req, imageReq));
        }

        @Test
        @DisplayName("게시물 생성 - 스토어 조회 실패시 예외 발생")
        void createPost_whenStoreNotFound_thenThrowsException() {
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(999L, "제목", "내용");
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder()
                    .images(List.of(new MockMultipartFile("img", "file.jpg", "image/jpeg", "data".getBytes())))
                    .build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
            when(storeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(StoreNotFoundException.class, () ->
                    postService.createPost(memberInfo, req, imageReq));
        }

        @Test
        @DisplayName("게시물 생성 - 이미지 수 2개 미만이면 예외 발생")
        void createPost_whenImageCountIsLessThanTwo_thenThrowsException() {
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(1L, "제목", "내용");
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder()
                    .images(List.of(new MockMultipartFile("img", "only1.jpg", "image/jpeg", "data".getBytes())))
                    .build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
            when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
            when(postRepository.save(any(Post.class))).thenReturn(Post.builder().store(store).member(member).title("제목").content("내용").build());

            when(postImageService.savePostImages(any(), any())).thenThrow(PostImageUpdateCountException.class);

            assertThrows(PostImageUpdateCountException.class, () ->
                    postService.createPost(memberInfo, req, imageReq));
        }

        @Test
        @DisplayName("게시물 생성 - 이미지 저장 호출 확인")
        void createPost_whenValidImages_thenSavesAllImages() {
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(1L, "제목", "내용");
            List<MultipartFile> files = List.of(
                    new MockMultipartFile("img", "1.jpg", "image/jpeg", "data1".getBytes()),
                    new MockMultipartFile("img", "2.jpg", "image/jpeg", "data2".getBytes())
            );
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder().images(files).build();

            Post post = Post.builder().store(store).member(member).title("제목").content("내용").build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
            when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
            when(postRepository.save(any())).thenReturn(post);

            postService.createPost(memberInfo, req, imageReq);

            verify(postImageService).savePostImages(eq(imageReq), any(Post.class));
        }

        @Test
        @DisplayName("게시물 생성 - 썸네일이 첫 번째 이미지로 설정됨")
        void createPost_whenValidImages_thenSetsThumbnailToFirstImage() {
            MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
            CreatePostRequest req = new CreatePostRequest(1L, "제목", "내용");
            List<MultipartFile> files = List.of(
                    new MockMultipartFile("img", "1.jpg", "image/jpeg", "data1".getBytes()),
                    new MockMultipartFile("img", "2.jpg", "image/jpeg", "data2".getBytes())
            );
            CreatePostImageRequest imageReq = CreatePostImageRequest.builder().images(files).build();

            Post post = Post.builder().store(store).member(member).title("제목").content("내용").build();

            when(memberRepository.findByNickname("행복한돼지")).thenReturn(Optional.of(member));
            when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
            when(postRepository.save(any())).thenReturn(post);
            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "thumb1.jpg"), new PostImage(post, "thumb2.jpg")));

            PostResponse response = postService.createPost(memberInfo, req, imageReq);

            assertEquals("thumb1.jpg", response.thumbnail());
        }
    }

    @Nested
    @DisplayName("게시물 수정(updatePost)")
    class UpdatePost {
        @Test
        @DisplayName("게시물 수정 - 제목과 내용만 변경, 이미지 변경 없음")
        void updatePost_whenOnlyTextUpdated_thenSkipsImageHandling() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], List.of());

            assertEquals("Updated", result.title());
            verify(postImageService, never()).deleteSelectedImages(anyLong(), any());
            verify(postImageService, never()).savePostImages(any(), any());
        }

        @Test
        @DisplayName("게시물 수정 - 내용만 수정")
        void updatePost_whenOnlyContentUpdated_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("Old Title", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], List.of());

            assertEquals("Updated Content", result.content());
            verify(postImageService, never()).deleteSelectedImages(anyLong(), any());
            verify(postImageService, never()).savePostImages(any(), any());
        }

        @Test
        @DisplayName("게시물 수정 - 제목과 내용 모두 수정")
        void updatePost_whenTitleAndContentUpdated_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("Updated Title", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], List.of());

            assertEquals("Updated Title", result.title());
            assertEquals("Updated Content", result.content());
        }

        @Test
        @DisplayName("게시물 수정 - 제목과 내용 수정 + 이미지 삭제")
        void updatePost_whenTitleContentAndImagesDeleted_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("New Title", "New Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("img1.jpg");

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(
                            PostImageResponse.builder().id(1L).postId(1L).s3Url("img1.jpg").build(),
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    ),
                    List.of(
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    )
            );

            doNothing().when(postImageService).deleteSelectedImages(eq(1L), any());

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], toDelete);

            assertEquals("New Title", result.title());
            assertEquals("New Content", result.content());
            assertEquals("img2.jpg", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 - 제목 수정 + 이미지 업로드")
        void updatePost_whenTitleUpdatedAndImagesUploaded_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("New Title", "Old Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            MultipartFile[] upload = {
                    new MockMultipartFile("img", "new1.jpg", "image/jpeg", "data".getBytes())
            };

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(PostImageResponse.builder().id(1L).postId(1L).s3Url("existing.jpg").build())
            );
            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "new1.jpg")));

            PostResponse result = postService.updatePost(1L, req, upload, List.of());

            assertEquals("New Title", result.title());
            assertEquals("existing.jpg", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 - 제목 수정 + 이미지 삭제")
        void updatePost_whenTitleUpdatedAndImagesDeleted_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("New Title", "Old Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("img1.jpg");

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(
                            PostImageResponse.builder().id(1L).postId(1L).s3Url("img1.jpg").build(),
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    ),
                    List.of(
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    )
            );

            doNothing().when(postImageService).deleteSelectedImages(eq(1L), any());

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], toDelete);

            assertEquals("New Title", result.title());
            assertEquals("img2.jpg", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 - 내용 수정 + 이미지 업로드")
        void updatePost_whenContentUpdatedAndImagesUploaded_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("Old Title", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            MultipartFile[] upload = {
                    new MockMultipartFile("img", "new1.jpg", "image/jpeg", "data".getBytes())
            };

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(PostImageResponse.builder().id(1L).postId(1L).s3Url("existing.jpg").build())
            );
            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "new1.jpg")));

            PostResponse result = postService.updatePost(1L, req, upload, List.of());

            assertEquals("Updated Content", result.content());
            assertEquals("existing.jpg", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 - 내용 수정 + 이미지 삭제")
        void updatePost_whenContentUpdatedAndImagesDeleted_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("Old Title", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("img1.jpg");

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(
                            PostImageResponse.builder().id(1L).postId(1L).s3Url("img1.jpg").build(),
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    ),

                    List.of(
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("img2.jpg").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("img3.jpg").build()
                    )
            );

            doNothing().when(postImageService).deleteSelectedImages(eq(1L), any());

            PostResponse result = postService.updatePost(1L, req, new MultipartFile[0], toDelete);

            assertEquals("Updated Content", result.content());
            assertEquals("img2.jpg", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 - 이미지 삭제만 수행 (남은 이미지 2장 이상)")
        void updatePost_whenImagesToDeleteProvided_thenDeletesImages() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("url1");

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(
                            PostImageResponse.builder().id(1L).postId(1L).s3Url("url1").build(),
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("remain_url1").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("remain_url2").build()
                    ),

                    List.of(
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("remain_url1").build(),
                            PostImageResponse.builder().id(3L).postId(1L).s3Url("remain_url2").build()
                    )
            );

            doNothing().when(postImageService).deleteSelectedImages(eq(1L), any());

            postService.updatePost(1L, req, new MultipartFile[0], toDelete);

            verify(postImageService).deleteSelectedImages(eq(1L), any());
        }

        @Test
        @DisplayName("게시물 수정 - 새 이미지 업로드만 수행")
        void updatePost_whenNewImagesUploaded_thenSavesNewImages() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();

            MultipartFile[] images = {
                    new MockMultipartFile("img", "upload1.jpg", "image/jpeg", "data1".getBytes())
            };

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(postImageService.findImagesByPostId(1L)).thenReturn(List.of(
                    PostImageResponse.builder().id(1L).postId(1L).s3Url("existing.jpg").build()
            ));

            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "upload1.jpg")));

            PostResponse result = postService.updatePost(1L, req, images, List.of());

            verify(postImageService).savePostImages(any(), eq(post));
            assertEquals("Updated", result.title());
        }

        @Test
        @DisplayName("게시물 수정 - 이미지 삭제 및 업로드 동시 처리")
        void updatePost_whenImagesDeletedAndUploaded_thenSuccess() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("url1");

            MultipartFile[] newImages = {
                    new MockMultipartFile("img", "new.jpg", "image/jpeg", "valid data".getBytes())
            };

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
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
            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "new_url")));

            PostResponse result = postService.updatePost(1L, req, newImages, toDelete);

            assertEquals("Updated", result.title());
            assertEquals("remain_url", result.thumbnail());
        }

        @Test
        @DisplayName("게시물 수정 실패 - 이미지 2장 미만이면 예외 발생")
        void updatePost_whenRemainingImageCountLessThanTwo_thenThrowsException() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").build();

            List<String> toDelete = List.of("url1");
            MultipartFile[] upload = new MultipartFile[0];

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postImageService.findImagesByPostId(1L)).thenReturn(List.of(
                    PostImageResponse.builder().id(1L).postId(1L).s3Url("url1").build()
            ));

            assertThrows(PostImageUpdateCountException.class, () ->
                    postService.updatePost(1L, req, upload, toDelete));
        }

        @Test
        @DisplayName("게시물 수정 - 썸네일 우선순위 검증 (삭제 후 업로드 이미지 적용)")
        void updatePost_whenThumbnailIsUpdated_thenAppliesPriorityRules() {
            UpdatePostRequest req = new UpdatePostRequest("Updated", "Updated Content");
            Post post = Post.builder().title("Old").content("Old").store(store).member(member).build();
            List<String> toDelete = List.of("existing.jpg");

            MultipartFile[] upload = {
                    new MockMultipartFile("img", "new1.jpg", "image/jpeg", "data".getBytes())
            };

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);

            when(postImageService.findImagesByPostId(1L)).thenReturn(
                    List.of(
                            PostImageResponse.builder().id(1L).postId(1L).s3Url("existing.jpg").build(),
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("to_keep.jpg").build()
                    ),
                    List.of(
                            PostImageResponse.builder().id(2L).postId(1L).s3Url("new1.jpg").build()
                    )
            );

            when(postImageService.savePostImages(any(), eq(post)))
                    .thenReturn(List.of(new PostImage(post, "new1.jpg")));

            PostResponse result = postService.updatePost(1L, req, upload, toDelete);

            assertEquals("new1.jpg", result.thumbnail());
        }
    }

    @Nested
    @DisplayName("게시물 삭제(deletePost)")
    class DeletePost {
        @Test
        @DisplayName("게시물 삭제 - 이미지 포함")
        void deletePost_whenExistingPost_thenSuccess() {
            Post post = Post.builder().title("Post Title").content("Content").build();
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            postService.deletePost(1L);

            verify(postImageService).deleteAllByPost(1L);
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("게시물 삭제 실패 - 이미지 삭제 실패")
        void deletePost_whenImageDeleteFails_thenThrows() {
            Post post = Post.builder().title("Post Title").content("Content").build();
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            doThrow(new RuntimeException("삭제 실패")).when(postImageService).deleteAllByPost(1L);

            assertThrows(PostDeleteFailedException.class, () -> postService.deletePost(1L));
        }

        @Test
        @DisplayName("게시물 삭제 실패 - DB 삭제 실패")
        void deletePost_whenRepositoryDeleteFails_thenThrows() {
            Post post = Post.builder().title("Post Title").content("Content").build();
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            doNothing().when(postImageService).deleteAllByPost(1L);
            doThrow(new RuntimeException("DB 실패")).when(postRepository).delete(post);

            assertThrows(PostDeleteFailedException.class, () -> postService.deletePost(1L));
        }
    }

    @Nested
    @DisplayName("게시물 조회(getPostById, findByStoreId, findByIdOrThrow)")
    class RetrievePost {
        @Test
        @DisplayName("게시물 ID로 게시물 조회 - 존재할 경우")
        void getPostById_whenPostExists_thenReturnsPostDetail() {
            Post post = Post.builder().title("조회제목").content("조회내용").member(member).store(store).build();
            when(postRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(post));

            PostResponse result = postService.getPostById(1L, false);

            assertEquals("조회제목", result.title());
        }

        @Test
        @DisplayName("게시물 ID로 게시물 조회 - 존재하지 않을 경우 예외")
        void getPostById_whenPostNotExists_thenThrowsException() {
            when(postRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPostById(1L, false));
        }

        @Test
        @DisplayName("Store ID에 대한 게시물 목록 조회")
        void findByStoreId_whenPostsExist_thenReturnsPostList() {
            Post post = Post.builder().title("스토어 게시글").content("내용").store(store).member(member).build();
            when(postRepository.findByStoreIdAndIsDeleted(1L, false)).thenReturn(List.of(post));

            List<PostResponse> results = postService.findByStoreId(1L, false);

            assertEquals(1, results.size());
            assertEquals("스토어 게시글", results.get(0).title());
        }

        @Test
        @DisplayName("findByIdOrThrow - 게시물이 존재할 경우 반환")
        void findByIdOrThrow_whenPostExists_thenReturnsPost() {
            Post post = Post.builder().title("존재하는 게시글").content("내용").build();
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            Post result = postService.findByIdOrThrow(1L);

            assertEquals("존재하는 게시글", result.getTitle());
        }

        @Test
        @DisplayName("findByIdOrThrow - 게시물이 없을 경우 예외 발생")
        void findByIdOrThrow_whenPostNotExists_thenThrowsException() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.findByIdOrThrow(1L));
        }
    }

}
