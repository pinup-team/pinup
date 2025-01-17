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
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)  // lenient strictness 설정
@ExtendWith(MockitoExtension.class)  // JUnit 5와 Mockito 통합
public class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postService;  // 실제 서비스 객체

    @Mock
    private PostRepository postRepository;  // 모킹된 PostRepository

    @Mock
    private PostImageService postImageService;  // 모킹된 PostImageService

    private PostEntity postEntity;

    @BeforeEach
    void setUp() {
        // PostEntity 객체를 설정합니다.
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
        // given: 테스트 데이터 설정
        PostDto postDto = new PostDto();
        postDto.setTitle("New Post");
        postDto.setContent("This is a new post");
        postDto.setStoreId(1L);
        postDto.setUserId(1L);

        // MultipartFile로 이미지 리스트 생성
        List<MultipartFile> images = Arrays.asList(
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes()),
                new MockMultipartFile("image2", "image2.jpg", "image/jpeg", "dummy content".getBytes())
        );
        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImages(images);  // 이미지 리스트 설정

        postDto.setPostImageDto(postImageDto);  // PostDto에 PostImageDto 설정

        // PostEntity 객체 생성
        PostEntity postEntity = new PostEntity();
        postEntity.setTitle(postDto.getTitle());
        postEntity.setContent(postDto.getContent());
        postEntity.setStoreId(postDto.getStoreId());
        postEntity.setUserId(postDto.getUserId());

        // PostImageEntity로 변환하여 반환할 리스트 생성
        List<PostImageEntity> postImageEntities = images.stream()
                .map(multipartFile -> new PostImageEntity(
                        postEntity,  // PostEntity 설정
                        "http://example.com/" + multipartFile.getOriginalFilename()  // S3 URL 설정
                ))
                .collect(Collectors.toList());

        // mock 설정
        when(postRepository.save(any(PostEntity.class))).thenReturn(postEntity);  // postRepository가 postEntity를 반환하도록 설정
        when(postImageService.savePostImages(any(PostImageDto.class), any(PostEntity.class)))
                .thenReturn(postImageEntities);  // 이미지 저장 서비스가 postImageEntities 반환

        // when: 서비스 메서드 호출
        PostEntity savedPost = postService.createPost(postDto);  // 테스트 메서드 실행

        // then: 적절한 검증
        verify(postImageService, times(1)).savePostImages(any(PostImageDto.class), any(PostEntity.class));  // 이미지 저장 메서드 호출 검증

        // 썸네일 설정 확인
        assertNotNull(savedPost.getThumbnail());  // 썸네일이 null이 아니어야 함
        assertEquals("http://example.com/image1.jpg", savedPost.getThumbnail());  // 썸네일 URL이 예상대로 설정되었는지 확인
    }


    @Test
    void testGetPostById() {
        // given: PostRepository가 mock되어 postEntity를 반환하도록 설정
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));

        // when: getPostById 메서드 호출
        PostEntity result = postService.getPostById(1L);

        // then: 반환된 결과가 postEntity와 일치하는지 검증
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Post", result.getTitle());
        assertEquals("This is a test post.", result.getContent());
        assertEquals("http://example.com/image1.jpg", result.getThumbnail());

    }

    @Test
    public void testGetPostByIdNotFound() {
        // given
        // Optional.empty()로 빈 결과를 반환하도록 설정
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(PostNotFoundException.class, () -> {
            postService.getPostById(1L);  // 예외가 발생해야 함
        });
    }

    @Test
    public void testDeletePost() {
        // given
        PostEntity postEntity = new PostEntity();
        postEntity.setId(1L);
        postEntity.setStoreId(1L);

        // 모킹 설정: findById가 Optional.of(postEntity)를 반환하도록 설정
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));

        // postRepository의 delete 메서드가 아무 동작도 하지 않도록 설정
        doNothing().when(postRepository).delete(postEntity);

        // postImageService의 deleteAllByPost 메서드가 아무 동작도 하지 않도록 설정
        doNothing().when(postImageService).deleteAllByPost(1L);

        // when
        postService.deletePost(1L);

        // then
        // postRepository.delete가 한 번 호출되었는지 검증
        verify(postRepository, times(1)).delete(postEntity);

        // postImageService.deleteAllByPost가 한 번 호출되었는지 검증
        verify(postImageService, times(1)).deleteAllByPost(1L);
    }

    @Test
    void testUpdatePost() throws IOException {
        // given: 테스트 데이터 설정
        PostDto postDto = new PostDto();
        postDto.setTitle("Updated Post");
        postDto.setContent("This is an updated post");
        postDto.setStoreId(1L);
        postDto.setUserId(1L);

        // MultipartFile로 이미지 리스트 생성
        List<MultipartFile> images = Arrays.asList(
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes())
        );
        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImages(images);  // 이미지 리스트 설정

        postDto.setPostImageDto(postImageDto);  // PostDto에 PostImageDto 설정

        // PostImageEntity로 변환하여 반환할 리스트 생성
        List<PostImageEntity> postImageEntities = images.stream()
                .map(multipartFile -> {
                    try {
                        return new PostImageEntity(multipartFile.getOriginalFilename(), multipartFile.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);  // IOException을 RuntimeException으로 감싸서 던짐
                    }
                })
                .collect(Collectors.toList());

        // 기존 게시글을 반환하는 설정
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(postEntity));

        // postRepository.save()가 postEntity를 반환하도록 설정
        when(postRepository.save(any(PostEntity.class))).thenReturn(postEntity);

        // postImageService.savePostImages 메서드가 PostImageEntity 리스트를 반환하도록 설정
        when(postImageService.savePostImages(any(PostImageDto.class), any(PostEntity.class)))
                .thenReturn(postImageEntities);  // PostImageEntity 리스트 반환

        // when: 서비스 메서드 호출
        PostEntity updatedPost = postService.updatePost(postEntity.getId(), postDto);  // 테스트 메서드 실행

        // then: 적절한 검증
        verify(postImageService, times(1)).savePostImages(any(PostImageDto.class), any(PostEntity.class));  // 이미지 저장 메서드 호출 검증
        assertNotNull(updatedPost.getThumbnail());  // 썸네일 설정 확인
        assertEquals("http://example.com/image1.jpg", updatedPost.getThumbnail());  // 썸네일 URL이 예상대로 설정되었는지 확인
    }
}
