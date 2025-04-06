package kr.co.pinup.store_images;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.stores.Store;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "store_images")
public class StoreImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

}
