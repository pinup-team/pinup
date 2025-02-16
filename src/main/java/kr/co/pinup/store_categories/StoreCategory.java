package kr.co.pinup.store_categories;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "store_categories")
public class StoreCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;
}