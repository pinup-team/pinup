package kr.co.pinup.posts.service.imp;


import kr.co.pinup.posts.exception.post.InvalidPostContentException;

import jakarta.transaction.Transactional;
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.dto.PostImageDto;
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

        post = postRepository.save(post);

        List<PostImageEntity> postImageEntities = postImageService.savePostImages(postDto.getPostImageDto(), post);


        if (!postImageEntities.isEmpty()) {
            post.setThumbnail(postImageEntities.get(0).getS3Url());
        }

        return  postRepository.save(post);
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
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        try {
            postImageService.deleteAllByPost(postId);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 중 이미지 삭제 실패. ID: " + postId);
        }

        try {
            postRepository.delete(post);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 실패. ID: " + postId);
        }
    }

    @Override
    public PostEntity updatePost(Long id, PostDto postDto) {

        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));


        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setStoreId(postDto.getStoreId());
        post.setUserId(postDto.getUserId());
        post.setUpdatedAt(Instant.now());


        if (postDto.getPostImageDto() != null) {

            if (postDto.getPostImageDto().getImagesToDelete() != null && !postDto.getPostImageDto().getImagesToDelete().isEmpty()) {
                postImageService.deleteSelectedImages(id, postDto.getPostImageDto());
            }


            if (postDto.getPostImageDto().getImages() != null && !postDto.getPostImageDto().getImages().isEmpty()) {

                PostImageDto postImageDto = new PostImageDto();
                postImageDto.setImages(postDto.getPostImageDto().getImages());
                postImageDto.setPostId(post.getId());

                List<PostImageEntity> postImages = postImageService.savePostImages(postImageDto, post);


                if (!postImages.isEmpty()) {
                    PostImageEntity firstImage = postImageService.findFirstImageByPostId(post.getId());
                    if (firstImage != null) {
                        post.setThumbnail(firstImage.getS3Url());
                    }
                }
            }
        }


        return postRepository.save(post);
    }

}
