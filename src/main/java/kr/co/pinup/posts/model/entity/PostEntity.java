package kr.co.pinup.posts.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
//TODO store 생기면 연결 필요
//    @ManyToOne(fetch = FetchType.LAZY)  // store와의 다대일 관계 설정
//    @JoinColumn(name = "store_id", nullable = false, insertable = false, updatable = false)
//    private Store store;  // Store 객체와 연결
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "thumbnail_url")
    private String thumbnail;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant  updatedAt;

    // @OneToMany: 하나의 게시글에는 여러 개의 이미지를 가질 수 있다.
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImageEntity> postImages = new ArrayList<>();

    // @OneToMany: 하나의 게시글에는 여러 개의 댓글을 가질 수 있다.
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();


    public PostEntity(long l, long l1, String testPost, String thisIsATestPost, String image) {
    }

    public PostEntity(long l, long l1, String postTitle, String postContent, Instant now, Instant now1) {
    }

}

