package kr.co.pinup.storecategories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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