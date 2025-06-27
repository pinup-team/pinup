package kr.co.pinup.postLike.service;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postLike.PostLike;
import kr.co.pinup.postLike.model.dto.PostLikeResponse;
import kr.co.pinup.postLike.repository.PostLikeRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostService postService;

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

    @Transactional
    public PostLikeResponse toggleLike(Long postId, MemberInfo memberInfo) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        Post post = postRepository.findByIdWithOptimisticLock(postId)
                .orElseThrow(PostNotFoundException::new);

        boolean liked;

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, member.getId());

        if (existingLike.isPresent()) {

            postLikeRepository.deleteByPostIdAndMemberId(post.getId(), member.getId());
            post.decreaseLikeCount();
            liked = false;
        } else {
            try {
                postLikeRepository.save(new PostLike(post, member));
                post.increaseLikeCount();
                liked = true;
            } catch (Exception e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause instanceof ConstraintViolationException || rootCause instanceof DataIntegrityViolationException) {
                    log.warn("좋아요 중복 저장 시도 (무시됨): postId={}, memberId={}", postId, member.getId());
                    liked = true;
                } else {
                    throw e;
                }
            }
        }

        System.out.println("toggleLike : "+post.getLikeCount());
        System.out.println("toggleLike : "+liked);
        return PostLikeResponse.of(post.getLikeCount(), liked);
    }


}
