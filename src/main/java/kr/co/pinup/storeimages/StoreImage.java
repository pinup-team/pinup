package kr.co.pinup.storeimages;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.stores.Store;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "store_images")
public class StoreImage extends BaseEntity {

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean isThumbnail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder
    private StoreImage(final String imageUrl, final boolean isThumbnail, final Store store) {
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
        this.store = store;
    }
}
