package kr.co.pinup.stores;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
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

    @Column(nullable = false)
    private String imageUrl;

}
