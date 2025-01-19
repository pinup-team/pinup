package kr.co.pinup.posts.service.impl;


import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.dto.PostImageDto;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.model.repository.PostRepository;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.imp.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageService postImageService;

    private PostEntity postEntity;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void testCreatePost() throws IOException {
        PostDto postDto = new PostDto();
        postDto.setTitle("New Post");
        postDto.setContent("This is a new post");
        postDto.setStoreId(1L);
        postDto.setUserId(1L);

        List<MultipartFile> images = Arrays.asList(
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes()),
                new MockMultipartFile("image2", "image2.jpg", "image/jpeg", "dummy content".getBytes())
        );
        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImages(images);

        postDto.setPostImageDto(postImageDto);

        PostEntity postEntity = new PostEntity();
        postEntity.setTitle(postDto.getTitle());
        postEntity.setContent(postDto.getContent());
        postEntity.setStoreId(postDto.getStoreId());
        postEntity.setUserId(postDto.getUserId());

        List<PostImageEntity> postImageEntities = images.stream()
                .map(multipartFile -> new PostImageEntity(
                        postEntity,
                        "http://example.com/" + multipartFile.getOriginalFilename()
                ))
                .collect(Collectors.toList());

        when(postRepository.save(any(PostEntity.class))).thenReturn(postEntity);
        when(postImageService.savePostImages(any(PostImageDto.class), any(PostEntity.class)))
                .thenReturn(postImageEntities);

        PostEntity savedPost = postService.createPost(postDto);

        verify(postImageService, times(1)).savePostImages(any(PostImageDto.class), any(PostEntity.class));

        assertNotNull(savedPost.getThumbnail());
        assertEquals("http://example.com/image1.jpg", savedPost.getThumbnail());
    }

    @Test
    void testGetPostById() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));

        PostEntity result = postService.getPostById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Post", result.getTitle());
        assertEquals("This is a test post.", result.getContent());
        assertEquals("http://example.com/image1.jpg", result.getThumbnail());
    }

    @Test
    public void testGetPostByIdNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> {
            postService.getPostById(1L);
        });
    }

    @Test
    public void testDeletePost() {
        PostEntity postEntity = new PostEntity();
        postEntity.setId(1L);
        postEntity.setStoreId(1L);

        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));

        doNothing().when(postRepository).delete(postEntity);

        doNothing().when(postImageService).deleteAllByPost(1L);

        postService.deletePost(1L);

        verify(postRepository, times(1)).delete(postEntity);

        verify(postImageService, times(1)).deleteAllByPost(1L);
    }

    @Test
    void testUpdatePost() throws IOException {
        PostDto postDto = new PostDto();
        postDto.setTitle("Updated Post");
        postDto.setContent("This is an updated post");
        postDto.setStoreId(1L);
        postDto.setUserId(1L);

        List<MultipartFile> images = Arrays.asList(
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes())
        );
        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImages(images);

        postDto.setPostImageDto(postImageDto);

        List<PostImageEntity> postImageEntities = images.stream()
                .map(multipartFile -> {
                    try {
                        return new PostImageEntity(multipartFile.getOriginalFilename(), multipartFile.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        when(postRepository.findById(anyLong())).thenReturn(Optional.of(postEntity));

        when(postRepository.save(any(PostEntity.class))).thenReturn(postEntity);

        when(postImageService.savePostImages(any(PostImageDto.class), any(PostEntity.class)))
                .thenReturn(postImageEntities);

        PostEntity updatedPost = postService.updatePost(postEntity.getId(), postDto);

        verify(postImageService, times(1)).savePostImages(any(PostImageDto.class), any(PostEntity.class));
        assertNotNull(updatedPost.getThumbnail());
        assertEquals("http://example.com/image1.jpg", updatedPost.getThumbnail());
    }
}