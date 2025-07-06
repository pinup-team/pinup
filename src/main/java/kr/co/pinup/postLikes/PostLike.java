package kr.co.pinup.postLikes;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.members.Member;
import kr.co.pinup.posts.Post;
import lombok.*;

@Getter
@Entity
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "post_like" ,uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "member_id"}))
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

}
