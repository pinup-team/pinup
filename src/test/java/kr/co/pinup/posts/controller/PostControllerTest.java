package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfigTest.class)
@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Inject MockMvc

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private PostImageService postImageService;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("게시물 리스트 페이지 이동")
    void listPage() throws Exception {
        Member mockMember = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("hdiJZoHQ-XDUkGvVCDLr1_NnTNZGcJjyxSAEUFjEi6A")
                .role(MemberRole.ROLE_USER)
                .build();
        // given
        Long storeId = 1L;
        List<PostResponse> mockPosts = IntStream.range(0, 10)
                .mapToObj(i -> PostResponse.builder()
                        .title("게시물 제목 " + (10 - i))
                        .content("게시물 내용 " + (10 - i))
                        .member(MemberResponse.fromMember(mockMember))
                        .build())
                .toList();

        // when
        when(postService.findByStoreId(storeId)).thenReturn(mockPosts);

        // expected
        mockMvc.perform(get("/post/list/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("posts", is(mockPosts)))
                .andExpect(model().attribute("storeId", is(storeId)))
                .andDo(print());
    }

    @Test
    @DisplayName("게시물 상세 페이지 이동")
    void postDetailPage() throws Exception {

        Member mockMember = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("hdiJZoHQ-XDUkGvVCDLr1_NnTNZGcJjyxSAEUFjEi6A")
                .role(MemberRole.ROLE_USER)
                .build();
        // given
        Long postId = 1L;
        PostResponse mockPost = PostResponse.builder()
                .title("게시물 제목")
                .content("게시물 내용")
                .member(MemberResponse.fromMember(mockMember))
                .build();


        List<CommentResponse> mockComments = List.of(new CommentResponse(
                1L,
                postId,
                mockMember,
                "댓글 내용",
                LocalDateTime.now()
        ));

        List<PostImageResponse> mockImages = List.of(new PostImageResponse(1L, 1L, "image.jpg"));

        // when
        when(postService.getPostById(postId)).thenReturn(mockPost);
        when(commentService.findByPostId(postId)).thenReturn(mockComments);
        when(postImageService.findImagesByPostId(postId)).thenReturn(mockImages);

        // expected
        mockMvc.perform(get("/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/detail"))
                .andExpect(model().attributeExists("post", "comments", "images"))
                .andExpect(model().attribute("post", is(mockPost)))
                .andExpect(model().attribute("comments", is(mockComments)))
                .andExpect(model().attribute("images", is(mockImages)))
                .andDo(print());
    }


    @Test
    @DisplayName("게시물 생성 페이지 이동")
    void createPostPage() throws Exception {
        // given
        Long storeId = 1L;

        // expected
        mockMvc.perform(get("/post/create")
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/create"))
                .andExpect(model().attribute("storeId", is(storeId)))
                .andDo(print());
    }

    @Test
    @DisplayName("게시물 수정 페이지 이동")
    void updatePostPage() throws Exception {
        // given
        Long postId = 1L;
        PostResponse mockPost = PostResponse.builder()
                .title("게시물 제목")
                .content("게시물 내용")
                .build();
        List<PostImageResponse> mockImages = List.of(new PostImageResponse(1L, 1L, "image.jpg"));

        // when
        when(postService.getPostById(postId)).thenReturn(mockPost);
        when(postImageService.findImagesByPostId(postId)).thenReturn(mockImages);

        // expected
        mockMvc.perform(get("/post/update/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/update"))
                .andExpect(model().attributeExists("post", "images"))
                .andExpect(model().attribute("post", is(mockPost)))
                .andExpect(model().attribute("images", is(mockImages)))
                .andDo(print());
    }
}
