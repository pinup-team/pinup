package kr.co.pinup.storeimages;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.stores.Store;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "store_images")
public class StoreImage extends BaseEntity {

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean isThumbnail;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder
    private StoreImage(final String imageUrl, final boolean isThumbnail, final Store store) {
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
        this.store = store;
        this.isDeleted = false;
    }

    public void changeThumbnail(final boolean isThumbnail) {
        this.isThumbnail = isThumbnail;
    }

    public void changeDeleted(final boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
