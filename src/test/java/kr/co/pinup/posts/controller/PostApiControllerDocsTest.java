package kr.co.pinup.posts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.locations.Location;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.service.PostService;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostApiController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({PostApiControllerDocsTest.MockConfig.class, PostApiControllerDocsTest.SecurityConfig.class, LoggerConfig.class})
class PostApiControllerDocsTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PostService postService;
    @Autowired CommentService commentService;
    @Autowired PostImageService postImageService;

    @TestConfiguration
    static class MockConfig {
        @Bean public PostService postService() { return mock(PostService.class); }
        @Bean public CommentService commentService() { return mock(CommentService.class); }
        @Bean public PostImageService postImageService() { return mock(PostImageService.class); }
    }

    @TestConfiguration
    static class SecurityConfig {
        @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("GET /api/post/list/{storeId} - 특정 스토어 게시글 목록 조회 문서화")
    void getPostListByStore_document() throws Exception {
        // given
        Long storeId = 10L;
        LocalDateTime now = LocalDateTime.now();

        MemberResponse writer = MemberResponse.builder()
                .id(1L)
                .nickname("행복한돼지")
                .email("test@example.com")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();

        List<PostResponse> posts = List.of(
                new PostResponse(1L,"writer","제목1", "thumb1.jpg", now, 1L,1,false),
                new PostResponse(2L,"writer","제목2", "thumb2.jpg", now, 1L,1,false)
        );

        given(postService.findByStoreId(eq(storeId), eq(false))).willReturn(posts);

        // when + then
        mockMvc.perform(get("/api/post/list/{storeId}", storeId))
                .andExpect(status().isOk())
                .andDo(document("post-get-list",
                        pathParameters(
                                parameterWithName("storeId").description("스토어 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("[].id").description("게시글 ID"),
                                fieldWithPath("[].member.nickname").description("작성자 닉네임"),
                                fieldWithPath("[].title").description("게시글 제목"),
                                fieldWithPath("[].thumbnail").description("썸네일 이미지 URL"),
                                fieldWithPath("[].createdAt").description("작성일시"),
                                fieldWithPath("[].commentCount").description("댓글 수"),
                                fieldWithPath("[].likeCount").description("좋아요 수").optional(),
                                fieldWithPath("[].likedByCurrentUser").description("현재 로그인한 사용자가 좋아요 눌렀는지 여부").optional()
                        )
                ));
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("GET /api/post/{postId} - 게시글 상세 조회 문서화")
    void getPostById_document() throws Exception {
        // given
        Long postId = 1L;
        LocalDateTime now = LocalDateTime.now();

        MemberResponse writer = MemberResponse.builder()
                .id(1L)
                .nickname("행복한돼지")
                .email("test@example.com")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();

        PostResponse postResponse =  new PostResponse(1L,"writer","제목1", "thumb1.jpg", now, 1L,1,false);

        Member commentWriter = Member.builder()
                .nickname("댓글유저")
                .email("comment@example.com")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();

        List<CommentResponse> comments = List.of(
                CommentResponse.builder()
                        .id(100L)
                        .postId(postId)
                        .content("댓글 내용")
                        .member(commentWriter)
                        .createdAt(now)
                        .build()
        );

        List<PostImageResponse> postImages = List.of(
                new PostImageResponse(201L, postId, "https://s3.bucket/image1.jpg"),
                new PostImageResponse(202L, postId, "https://s3.bucket/image2.jpg")
        );

        given(postService.getPostById(eq(postId), eq(false))).willReturn(postResponse);
        given(commentService.findByPostId(eq(postId))).willReturn(comments);
        given(postImageService.findImagesByPostId(eq(postId))).willReturn(postImages);

        // then
        mockMvc.perform(get("/api/post/{postId}", postId))
                .andExpect(status().isOk())
                .andDo(document("post-get-by-id",
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseFields(
                                subsectionWithPath("post").description("게시글 정보"),
                                fieldWithPath("post.id").description("게시글 ID"),
                                fieldWithPath("post.storeId").description("스토어 ID"),
                                fieldWithPath("post.member.id").description("작성자 ID"),
                                fieldWithPath("post.member.nickname").description("작성자 닉네임"),
                                fieldWithPath("post.member.email").description("작성자 이메일"),
                                fieldWithPath("post.member.providerType").description("OAuth 제공자"),
                                fieldWithPath("post.member.role").description("사용자 권한"),
                                fieldWithPath("post.member.deleted").description("탈퇴 여부"),

                                fieldWithPath("post.title").description("게시글 제목"),
                                fieldWithPath("post.content").description("게시글 내용"),
                                fieldWithPath("post.thumbnail").description("썸네일 이미지 URL"),
                                fieldWithPath("post.createdAt").description("게시글 생성일"),
                                fieldWithPath("post.updatedAt").description("게시글 수정일"),
                                fieldWithPath("post.commentCount").description("댓글 수"),

                                subsectionWithPath("comments").description("댓글 목록"),
                                fieldWithPath("comments[].id").description("댓글 ID"),
                                fieldWithPath("comments[].postId").description("댓글이 속한 게시글 ID"),
                                fieldWithPath("comments[].member.nickname").description("댓글 작성자 닉네임"),
                                fieldWithPath("comments[].content").description("댓글 내용"),
                                fieldWithPath("comments[].createdAt").description("댓글 작성일"),

                                subsectionWithPath("postImages").description("게시글 이미지 목록"),
                                fieldWithPath("postImages[].id").description("이미지 ID"),
                                fieldWithPath("postImages[].postId").description("이미지가 속한 게시글 ID"),
                                fieldWithPath("postImages[].s3Url").description("이미지 S3 URL")
                        )
                ));
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("POST /api/post/create - 게시글 생성 문서화")
    void post_create_document() throws Exception {
        // Given: 이미지 파일 2장
        MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "data1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "data2".getBytes());

        // 게시글 요청 JSON (storeId, title, content)
        CreatePostRequest createPostRequest = new CreatePostRequest(1L, "제목", "내용");
        String json = objectMapper.writeValueAsString(createPostRequest);
        MockMultipartFile post = new MockMultipartFile(
                "post", "post.json", "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8)
        );

        // Mock 객체 준비
        StoreCategory category = new StoreCategory("카테고리명");
        Location location = new Location("서울시", "12345", "서울", "강남구", 37.1234, 127.5678, "서울 강남구", "101호");
        Store mockStore = Store.builder()
                .name("테스트 스토어")
                .description("테스트 설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build();

        Member mockMember = Member.builder()
                .email("test@naver.com")
                .name("테스터")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver123")
                .role(MemberRole.ROLE_USER)
                .build();

        PostResponse createdPostResponse = PostResponse.builder()
                .id(1L)
                .storeId(mockStore.getId())
                .member(new MemberResponse(mockMember))
                .title("제목")
                .content("내용")
                .thumbnail("https://example.com/thumbnail.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .commentCount(0)
                .build();

        // When: postService.createPost() 호출 시 위 결과 반환
        given(postService.createPost(any(), any(), any())).willReturn(createdPostResponse);

        // Then: 문서화 수행
        mockMvc.perform(multipart("/api/post/create")
                        .file(post)
                        .file(image1)
                        .file(image2)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(req -> { req.setMethod("POST"); return req; }))
                .andExpect(status().isCreated())
                .andDo(result -> {
                    System.out.println("📦 Response Body: " + result.getResponse().getContentAsString());
                })
                .andDo(document("post-create",
                        requestParts(
                                partWithName("post").description("게시글 정보(JSON)"),
                                partWithName("images").description("이미지 파일들 (2장 이상)")
                        ),
                        requestPartFields("post",
                                fieldWithPath("storeId").description("스토어 ID"),
                                fieldWithPath("title").description("게시글 제목"),
                                fieldWithPath("content").description("게시글 내용")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("storeId").description("스토어 ID"),
                                fieldWithPath("member.id").description("작성자 ID (nullable)"),
                                fieldWithPath("member.name").description("작성자 이름"),
                                fieldWithPath("member.email").description("작성자 이메일"),
                                fieldWithPath("member.nickname").description("작성자 닉네임"),
                                fieldWithPath("member.providerType").description("OAuth 제공자"),
                                fieldWithPath("member.role").description("사용자 역할"),
                                fieldWithPath("member.deleted").description("삭제 여부"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                                fieldWithPath("thumbnail").description("썸네일 이미지 URL"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByCurrentUser").description("현재 로그인한 사용자가 좋아요 눌렀는지 여부").optional()
                        )
                ));

    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("DELETE /api/post/{postId} - 게시글 삭제 문서화")
    void deletePost_document() throws Exception {
        Long postId = 1L;

        // when + then
        mockMvc.perform(delete("/api/post/{postId}", postId)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(document("post-delete",
                        pathParameters(
                                parameterWithName("postId").description("삭제할 게시글 ID")
                        )
                ));
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("PUT /api/post/{postId} - 게시글 수정 문서화")
    void updatePost_document() throws Exception {
        // Given
        Long postId = 1L;

        MockMultipartFile updatePostRequest = new MockMultipartFile(
                "updatePostRequest",
                "updatePostRequest.json",
                "application/json; charset=UTF-8",
                objectMapper.writeValueAsBytes(new UpdatePostRequest("수정 제목", "수정 내용"))
        );

        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "image1.jpg",
                "image/jpeg",
                "이미지데이터1".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "image2.jpg",
                "image/jpeg",
                "이미지데이터2".getBytes()
        );

        // Mock Store, Member, Response 구성
        StoreCategory category = new StoreCategory("카테고리명");
        Location location = new Location("서울시", "12345", "서울", "강남구", 37.1234, 127.5678, "서울 강남구", "101호");
        Store mockStore = Store.builder()
                .name("테스트 스토어")
                .description("테스트 설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build();

        Member mockMember = Member.builder()
                .email("test@naver.com")
                .name("테스터")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver123")
                .role(MemberRole.ROLE_USER)
                .build();

        PostResponse updatedPostResponse = PostResponse.builder()
                .id(postId)
                .storeId(mockStore.getId())
                .member(new MemberResponse(mockMember))
                .title("수정 제목")
                .content("수정 내용")
                .thumbnail("https://example.com/updated-thumbnail.jpg")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .commentCount(0)
                .build();

        given(postService.updatePost(anyLong(), any(), any(), any())).willReturn(updatedPostResponse);

        // When + Then
        mockMvc.perform(
                        multipart("/api/post/{postId}", postId)
                                .file(updatePostRequest)
                                .file(image1)
                                .file(image2)
                                .param("imagesToDelete", "img1.jpg", "img2.jpg")
                                .with(csrf())
                                .with(req -> {
                                    req.setMethod("PUT"); // 명시적으로 PUT 메서드 설정
                                    return req;
                                })
                )
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("📦 Response Body: " + result.getResponse().getContentAsString());
                })
                .andDo(document("post-update",
                        pathParameters(
                                parameterWithName("postId").description("수정할 게시글 ID")
                        ),
                        requestParts(
                                partWithName("updatePostRequest").description("수정할 게시글 정보 (title, content 포함)"),
                                partWithName("images").description("추가로 업로드할 이미지 파일들 (0개 이상 가능)")
                        ),
                        requestPartFields("updatePostRequest",
                                fieldWithPath("title").description("수정할 제목"),
                                fieldWithPath("content").description("수정할 내용")
                        ),
                        queryParameters(
                                parameterWithName("imagesToDelete").optional().description("삭제할 이미지 파일 이름 목록 (선택)")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("storeId").description("스토어 ID"),
                                fieldWithPath("member.id").optional().description("작성자 ID"),
                                fieldWithPath("member.name").description("작성자 이름"),
                                fieldWithPath("member.email").description("작성자 이메일"),
                                fieldWithPath("member.nickname").description("작성자 닉네임"),
                                fieldWithPath("member.providerType").description("소셜 로그인 제공자"),
                                fieldWithPath("member.role").description("회원 권한"),
                                fieldWithPath("member.deleted").description("회원 삭제 여부"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                                fieldWithPath("thumbnail").description("썸네일 이미지 URL"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByCurrentUser").description("현재 로그인한 사용자가 좋아요 눌렀는지 여부").optional()
                        )

                ));
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("PATCH /api/post/{postId}/disable - 게시글 비활성화 문서화")
    void disablePost_document() throws Exception {
        // given
        Long postId = 1L;

        // when + then
        mockMvc.perform(
                        patch("/api/post/{postId}/disable", postId)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andDo(document("post-disable",
                        pathParameters(
                                parameterWithName("postId").description("비활성화할 게시글 ID")
                        )
                ));
    }

}
