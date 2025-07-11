package kr.co.pinup.postLikes.service;

import jakarta.persistence.EntityManager;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostService postService;
    private final PostLikeRetryExecutor postLikeRetryExecutor;
    private final AppLogger appLogger;
    private final EntityManager entityManager;

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

        postService.findByIdOrThrow(postId);

        return postLikeRetryExecutor.likeWithRetry(() -> {
            Post post = postRepository.findByIdWithOptimisticLock(postId)
                    .orElseThrow(PostNotFoundException::new);

            try {
                postLikeRepository.save(new PostLike(post, member));
                post.increaseLikeCount();
                appLogger.info(new InfoLog("좋아요 등록")
                        .setTargetId(postId.toString())
                        .addDetails("memberId", member.getId().toString()));

                return PostLikeResponse.of(post.getLikeCount(), true);

            } catch (DataIntegrityViolationException e) {
                entityManager.clear();
                postLikeRepository.deleteByPostIdAndMemberId(postId, member.getId());

                Post managedPost = entityManager.merge(post);
                managedPost.decreaseLikeCount();

                post.decreaseLikeCount();

                appLogger.warn(new InfoLog("중복 좋아요 감지 - 취소 처리로 전환")
                        .setTargetId(postId.toString())
                        .addDetails("reason", "UNIQUE 제약 위반 → 토글 처리"));

                return PostLikeResponse.of(post.getLikeCount() , false);
            }
        });
    }

}