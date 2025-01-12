package kr.co.pinup.posts.service.imp;


import kr.co.pinup.posts.exception.post.InvalidPostContentException;

import jakarta.transaction.Transactional;
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.model.repository.PostRepository;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;

    @Override
    public PostEntity createPost(PostDto postDto) {
        if (postDto.getTitle() == null || postDto.getContent() == null) {
            throw new InvalidPostContentException("제목 또는 내용이 비어 있습니다.");
        }

        PostEntity post = new PostEntity();
        post.setStoreId(postDto.getStoreId());
        post.setUserId(postDto.getUserId());
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());

        // 게시글 저장
        post = postRepository.save(post);

        // 이미지 저장
        List<PostImageEntity> postImages = postImageService.savePostImages(postDto.getImages(), post);

        // 썸네일을 첫 번째 이미지로 설정
        if (!postImages.isEmpty()) {
            post.setThumbnail(postImages.get(0).getS3Url()); // 썸네일로 첫 번째 이미지를 설정
            postRepository.save(post);
        }

        return post;
    }

    @Override
    public List<PostEntity> findByStoreId(Long storeId) {
        return postRepository.findByStoreId(storeId);
    }

    @Override
    public PostEntity getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }

    public void deletePost(Long postId) {
        // 게시글 조회
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        // 해당 게시글에 연관된 이미지 삭제
        try {
            postImageService.deleteAllByPost(postId);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 중 이미지 삭제 실패. ID: " + postId);
        }

        // 게시글 삭제
        try {
            postRepository.delete(post);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 실패. ID: " + postId);
        }
    }

    @Override
    public PostEntity updatePost(Long id, PostDto postDto) {
        // 기존 게시글 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));

        // 게시글 정보 수정
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setStoreId(postDto.getStoreId());
        post.setUserId(postDto.getUserId());

        // 기존 이미지를 처리 (기존 이미지를 삭제하고 새 이미지 추가)
        if (postDto.getImagesToDelete() != null && !postDto.getImagesToDelete().isEmpty()) {
            // 삭제할 이미지 URL 리스트를 처리하여 S3에서 해당 파일 삭제
            postImageService.deleteSelectedImages(id, postDto.getImagesToDelete());
        }

        if (postDto.getImages() != null && !postDto.getImages().isEmpty()) {
            // 새 이미지 저장
            List<PostImageEntity> postImages = postImageService.savePostImages(postDto.getImages(), post);

            if (!postImages.isEmpty()) {
                // 썸네일을 첫 번째 이미지로 설정
                PostImageEntity firstImage = postImageService.findFirstImageByPostId(post.getId());

                if (firstImage != null) {
                    post.setThumbnail(firstImage.getS3Url()); // 썸네일로 첫 번째 이미지를 설정
                }
            }
        }
        return postRepository.save(post);
    }
}
