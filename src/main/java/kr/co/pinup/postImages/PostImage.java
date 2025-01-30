package kr.co.pinup.postImages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.posts.Post;
import lombok.*;


@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(exclude = "post")
@Entity
@Table(name = "post_images")
public class PostImage extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;  // 게시글과의 관계

    @Column(name = "url", nullable = false)
    private String s3Url;
}

