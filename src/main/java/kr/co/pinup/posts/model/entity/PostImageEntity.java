package kr.co.pinup.posts.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "post")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "post_images")
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;  // 게시글과의 관계

    @Column(name = "url", nullable = false)
    private String s3Url;

    public PostImageEntity(PostEntity post, String s3Url) {
        this.post = post;
        this.s3Url = s3Url;
    }

    public PostImageEntity(String originalFilename, byte[] bytes) {

    }

    public void setPost(PostEntity post) {
        this.post = post;
        if (!post.getPostImages().contains(this)) {
            post.getPostImages().add(this);
        }
    }

}

