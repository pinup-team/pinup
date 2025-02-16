package kr.co.pinup.stores;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.locations.Location;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.model.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "stores")
public class Store extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private StoreCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    public void updateStore(StoreUpdateRequest request, StoreCategory category, Location location) {
        if (request.getName() != null) this.name = request.getName();

        if (request.getDescription() != null) this.description = request.getDescription();
        if (request.getCategoryId() != null) this.category = category;
        if (request.getLocationId() != null) this.location = location;
        if (request.getStartDate() != null) this.startDate = request.getStartDate();
        if (request.getEndDate() != null) this.endDate = request.getEndDate();
        if (request.getStatus() != null) this.status = request.getStatus();
        if (request.getImageUrl() != null) this.imageUrl = request.getImageUrl();
    }

}


