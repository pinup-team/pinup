package kr.co.pinup.posts.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;
    private final MemberRepository memberRepository;
    private final StoreRepository  storeRepository;

    @Transactional
    public PostResponse createPost(MemberInfo memberInfo, CreatePostRequest createPostRequest, MultipartFile[] images) {

        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        Store store = storeRepository.findById(createPostRequest.storeId())
                .orElseThrow(() -> new StoreNotFoundException(createPostRequest.storeId() + "을 찾을 수 없습니다."));

        Post post = Post.builder()
                .store(store)
                .member(member)
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .build();

        post = postRepository.save(post);

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .build();

        List<PostImage> postImages = postImageService.savePostImages(postImageRequest, post);

        if (!postImages.isEmpty()) {
            post.updateThumbnail(postImages.get(0).getS3Url());
        }

        return PostResponse.from(post);
    }

    public List<PostResponse> findByStoreId(Long storeId) {
        List<Post> posts = postRepository.findByStoreId(storeId);
        return posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }

    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
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

    @Transactional
    public Post updatePost(Long id, UpdatePostRequest updatePostRequest, MultipartFile[] images, List<String> imagesToDelete) {

        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));

        boolean hasNewImages = images != null && images.length > 0 && Arrays.stream(images).noneMatch(MultipartFile::isEmpty);
        boolean hasImagesToDelete = imagesToDelete != null && !imagesToDelete.isEmpty();

        existingPost.update(updatePostRequest.title(), updatePostRequest.content());

        if (!hasNewImages && !hasImagesToDelete) {
            return postRepository.save(existingPost);
        }

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(hasNewImages ? Arrays.asList(images) : Collections.emptyList())
                .imagesToDelete(hasImagesToDelete ? imagesToDelete : Collections.emptyList())
                .build();

        if (hasImagesToDelete) {
            postImageService.deleteSelectedImages(id, postImageRequest);
        }

        List<PostImageResponse> remainingImages = postImageService.findImagesByPostId(id);

        List<PostImage> uploadedImages = hasNewImages ? postImageService.savePostImages(postImageRequest, existingPost) : Collections.emptyList();

        if (!remainingImages.isEmpty()) {
            existingPost.updateThumbnail(remainingImages.get(0).getS3Url());
        } else if (!uploadedImages.isEmpty()) {
            existingPost.updateThumbnail(uploadedImages.get(0).getS3Url());
        } else {
            throw new PostImageNotFoundException("썸네일을 설정할 수 없습니다. 게시물에 남은 이미지가 없습니다.");
        }

        return postRepository.save(existingPost);
    }
}