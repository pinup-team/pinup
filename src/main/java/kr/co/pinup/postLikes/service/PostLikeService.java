package kr.co.pinup.postLikes.service;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postLikes.PostLike;
import kr.co.pinup.postLikes.PostLikeRetryExecutor;
import kr.co.pinup.postLikes.model.dto.PostLikeResponse;
import kr.co.pinup.postLikes.repository.PostLikeRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostService postService;
    private final PostLikeRetryExecutor postLikeRetryExecutor;

    public PostLikeResponse getLikeInfo(Long postId, MemberInfo memberInfo) {

        Post post = postService.findByIdOrThrow(postId);
        int likeCount = post.getLikeCount();

        boolean liked = false;
        if (memberInfo != null) {
            Member member = memberRepository.findByNickname(memberInfo.nickname())
                    .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

            liked = postLikeRepository.existsByPostIdAndMemberId(postId, member.getId());
        }

        return PostLikeResponse.of(likeCount, liked);
    }

    public PostLikeResponse toggleLike(Long postId, MemberInfo memberInfo) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        AtomicReference<Boolean> liked = new AtomicReference<>(false);

        postLikeRetryExecutor.likeWithRetry(() -> {
            Post post = postRepository.findByIdWithOptimisticLock(postId)
                    .orElseThrow(PostNotFoundException::new);

            Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, member.getId());

            if (existingLike.isPresent()) {
                postLikeRepository.delete(existingLike.get());
                post.decreaseLikeCount();
                liked.set(false);
            } else {
                postLikeRepository.save(new PostLike(post, member));
                post.increaseLikeCount();
                liked.set(true);
            }
        });

        Post updated = postRepository.findById(postId).orElseThrow();
        return PostLikeResponse.of(updated.getLikeCount(), liked.get());
    }
}
