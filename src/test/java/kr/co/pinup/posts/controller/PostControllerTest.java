package kr.co.pinup.posts.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.service.CommentService;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(PostController.class)  // PostController만 테스트합니다.
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 보내고 결과를 검증합니다.

    @MockBean
    private PostService postService;  // PostService는 Mock 객체로 주입됩니다.

    @MockBean
    private CommentService commentService;  // CommentService도 Mock 객체로 주입됩니다.

    @MockBean
    private PostImageService postImageService;  // PostImageService도 Mock 객체로 주입됩니다.

    // 테스트에 사용할 가짜 데이터
    private PostEntity postEntity;
    private List<PostEntity> posts;

    @BeforeEach
    public void setUp() {
        // 가짜 데이터를 생성
        postEntity = new PostEntity(1L, 1L, "Test Post", "This is a test post", "thumbnail.jpg");
        posts = List.of(postEntity);  // 가짜 게시글 목록
    }

    // 게시글 리스트 조회 테스트
    @Test
    public void testGetAllPosts() throws Exception {
        // given
        when(postService.findByStoreId(anyLong())).thenReturn(posts);

        // when & then
        mockMvc.perform(get("/post/list/{storeid}", 1L)) // HTTP POST 요청
                .andExpect(status().isOk())  // 응답 상태 코드가 200인지 검증
                .andExpect(model().attributeExists("posts")) // 모델에 'posts' 속성이 존재하는지 검증
                .andExpect(view().name("post/list"));  // 반환되는 뷰 이름이 'post/list'인지 검증
    }

    // 게시글 상세 조회 테스트
    @Test
    public void testGetPostById() throws Exception {
        // given
        when(postService.getPostById(anyLong())).thenReturn(postEntity);
        when(commentService.findByPostId(anyLong())).thenReturn(List.of());  // 댓글 데이터는 비어 있음
        when(postImageService.findImagesByPostId(anyLong())).thenReturn(List.of());  // 이미지 데이터도 비어 있음

        // when & then
        mockMvc.perform(get("/post/{postId}", 1L))  // HTTP GET 요청
                .andExpect(status().isOk())  // 응답 상태 코드가 200인지 검증
                .andExpect(model().attributeExists("post"))  // 모델에 'post' 속성이 존재하는지 검증
                .andExpect(model().attributeExists("comments"))  // 모델에 'comments' 속성이 존재하는지 검증
                .andExpect(model().attributeExists("images"))  // 모델에 'images' 속성이 존재하는지 검증
                .andExpect(view().name("post/detail"));  // 반환되는 뷰 이름이 'post/detail'인지 검증
    }

    // 유효하지 않은 게시글 ID로 조회 시 리다이렉트 테스트
    @Test
    public void testGetPostByIdWithValidId() throws Exception {
        // given
        PostEntity postEntity = new PostEntity(1L, 1L, "Test Post", "This is a test post", "thumbnail.jpg");
        when(postService.getPostById(1L)).thenReturn(postEntity);

        // when & then
        mockMvc.perform(get("/post/{postId}", 1L))
                .andExpect(status().isOk())  // 응답 상태가 200
                .andExpect(view().name("post/detail"))  // 뷰 이름이 "post/detail"인지
                .andExpect(model().attribute("post", postEntity));  // 모델에 "post" 객체가 포함되어 있는지
    }


    // 게시글 생성 페이지로 이동 테스트
    @Test
    public void testCreatePostForm() throws Exception {
        // when & then
        mockMvc.perform(get("/post/create"))  // HTTP GET 요청
                .andExpect(status().isOk())  // 응답 상태 코드가 200인지 검증
                .andExpect(view().name("/post/create"));  // 반환되는 뷰 이름이 '/post/create'인지 검증
    }

    // 게시글 생성 테스트
    @Test
    public void testCreatePost() throws Exception {
        // given
        PostDto postDto = new PostDto();
        postDto.setTitle("New Post");
        postDto.setContent("This is a new post");
        postDto.setUserId(1L);
        postDto.setStoreId(1L);

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "dummy image 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "dummy image 2".getBytes());

        when(postService.createPost(any(PostDto.class))).thenReturn(postEntity);

        // when & then
        mockMvc.perform(multipart("/post/create")
                        .file(image1)  // 이미지 1 파일 전송
                        .file(image2)  // 이미지 2 파일 전송
                        .param("title", postDto.getTitle())
                        .param("content", postDto.getContent())
                        .param("userId", postDto.getUserId().toString())
                        .param("storeId", postDto.getStoreId().toString()))
                .andExpect(status().isOk())  // 응답 상태 코드가 200인지 검증
                .andExpect(model().attributeExists("post"))  // 모델에 'post' 속성이 존재하는지 검증
                .andExpect(view().name("post/detail"));  // 수정된 템플릿 이름
    }


    // 게시글 삭제 테스트
    @Test
    public void testDeletePost() throws Exception {
        // given
        when(postService.getPostById(anyLong())).thenReturn(postEntity);

        // when & then
        mockMvc.perform(delete("/post/{id}", 1L))  // HTTP DELETE 요청
                .andExpect(status().is3xxRedirection())  // 리디렉션(3xx) 상태 코드 검증
                .andExpect(redirectedUrl("/post/list"));
    }

    // 게시글 수정 테스트
    @Test
    public void testUpdatePost() throws Exception {
        // given
        PostDto postDto = new PostDto();
        postDto.setTitle("Updated Post");
        postDto.setContent("This is an updated post");

        // mock service methods
        when(postService.getPostById(anyLong())).thenReturn(postEntity);
        when(postService.updatePost(anyLong(), any(PostDto.class))).thenReturn(postEntity);

        // when & then
        mockMvc.perform(put("/post/{id}", 1L)
                        .param("title", postDto.getTitle())  // postDto의 title 전달
                        .param("content", postDto.getContent())  // postDto의 content 전달
                )
                .andExpect(status().isOk())  // 응답 상태 코드가 200인지 검증
                .andExpect(view().name("post/detail"));  // 수정된 템플릿 이름
    }

}
