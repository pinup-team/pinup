package kr.co.pinup.posts;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.members.Member;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.stores.Store;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "thumbnail_url")
    private String thumbnail;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean isDeleted;

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostImage> postImages = new ArrayList<>();

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();


    public void updateThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void disablePost(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

}

