package kr.co.pinup.stores;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.locations.Location;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.stores.model.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Setter
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
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(length = 50)
    private String contactNumber;

    @Column(length = 255)
    private String websiteUrl;

    @Column(length = 255)
    private String snsUrl;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN default false")
    private boolean deleted;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer thumbnailIndex = 0;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreOperatingHour> operatingHours = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreImage> storeImages = new ArrayList<>();

    public void deleteStore() {
        this.deleted = true;
    }

    public void setThumbnailIndex(int index) {
        if (index >= 0 && index < storeImages.size()) {
            this.thumbnailIndex = index;
            this.imageUrl = storeImages.get(index).getImageUrl();
        }
    }

    public int getThumbnailIndex() {
        return this.thumbnailIndex;
    }

    public String getThumbnailUrl() {
        if (thumbnailIndex >= 0 && thumbnailIndex < storeImages.size()) {
            return storeImages.get(thumbnailIndex).getImageUrl();
        }
        return null;
    }

    public void addOperatingHours(final List<StoreOperatingHour> operatingHours) {
        this.operatingHours.addAll(operatingHours);
    }

    public void addImages(final List<StoreImage> storeImages) {
        this.storeImages.addAll(storeImages);
    }
}


