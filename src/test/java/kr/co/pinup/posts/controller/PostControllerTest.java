package kr.co.pinup.posts.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.service.CommentService;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    @InjectMocks
    private PostController postController;

    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private PostImageService postImageService;

    private MockMvc mockMvc;

    private PostEntity postEntity;
    private List<PostEntity> posts;

    @BeforeEach
    public void setUp() {
        postEntity = new PostEntity(1L, 1L, "Test Post", "This is a test post", "thumbnail.jpg");
        posts = List.of(postEntity);
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    public void testGetAllPosts() throws Exception {
        List<PostEntity> posts = new ArrayList<>();
        posts.add(new PostEntity());
        when(postService.findByStoreId(anyLong())).thenReturn(posts);

        mockMvc.perform(get("/post/list/{storeid}", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"))
                .andExpect(view().name("post/list"));
    }

    @Test
    public void testGetPostById() throws Exception {
        when(postService.getPostById(anyLong())).thenReturn(postEntity);
        when(commentService.findByPostId(anyLong())).thenReturn(List.of());
        when(postImageService.findImagesByPostId(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/post/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("comments"))
                .andExpect(model().attributeExists("images"))
                .andExpect(view().name("post/detail"));
    }

    @Test
    public void testCreatePostForm() throws Exception {
        mockMvc.perform(get("/post/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("post/create"));
    }

    @Test
    public void testCreatePost() throws Exception {
        PostDto postDto = new PostDto();
        postDto.setTitle("New Post");
        postDto.setContent("This is a new post");
        postDto.setUserId(1L);
        postDto.setStoreId(1L);

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "dummy image 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "dummy image 2".getBytes());

        PostEntity postEntity = new PostEntity();
        postEntity.setId(1L);

        when(postService.createPost(any(PostDto.class))).thenReturn(postEntity);

        mockMvc.perform(multipart("/post/create")
                        .file(image1)
                        .file(image2)
                        .param("title", postDto.getTitle())
                        .param("content", postDto.getContent())
                        .param("userId", postDto.getUserId().toString())
                        .param("storeId", postDto.getStoreId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/1"))
                .andExpect(flash().attributeExists("post"));
    }

    @Test
    public void testDeletePost() throws Exception {
        PostEntity postEntity = new PostEntity();
        postEntity.setStoreId(1L);
        when(postService.getPostById(anyLong())).thenReturn(postEntity);

        mockMvc.perform(delete("/post/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/list/1"));
    }

    @Test
    public void testUpdatePost() throws Exception {
        PostDto postDto = new PostDto();
        postDto.setTitle("Updated Post");
        postDto.setContent("This is an updated post");

        PostEntity updatedPostEntity = new PostEntity();
        updatedPostEntity.setId(1L);
        updatedPostEntity.setTitle(postDto.getTitle());
        updatedPostEntity.setContent(postDto.getContent());

        when(postService.updatePost(anyLong(), any(PostDto.class))).thenReturn(updatedPostEntity);

        mockMvc.perform(put("/post/{id}", 1L)
                        .param("title", postDto.getTitle())
                        .param("content", postDto.getContent()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/1"));
    }

}
