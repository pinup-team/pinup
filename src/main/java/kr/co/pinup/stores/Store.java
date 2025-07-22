package kr.co.pinup.stores;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.locations.Location;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.model.enums.StoreStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_status", nullable = false)
    private StoreStatus storeStatus;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "sns_url")
    private String snsUrl;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private StoreCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreOperatingHour> operatingHours = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreImage> storeImages = new ArrayList<>();

    @Builder
    private Store(
            final String name, final String description, final StoreStatus storeStatus, final LocalDate startDate,
            final LocalDate endDate, final String websiteUrl, final String snsUrl,
            final StoreCategory category, final Location location
    ) {
        this.name = name;
        this.description = description;
        this.storeStatus = storeStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.websiteUrl = websiteUrl;
        this.snsUrl = snsUrl;
        this.viewCount = 0L;
        this.isDeleted = false;
        this.category = category;
        this.location = location;
    }

    public void updateStatus(final StoreStatus newStatus) {
        this.storeStatus = newStatus;
    }

    public void update(final StoreUpdateRequest request, final StoreCategory category, final Location location) {
        name = request.name();
        description = request.description();
        startDate = request.startDate();
        endDate = request.endDate();
        websiteUrl = request.websiteUrl();
        snsUrl = request.snsUrl();
        this.category = category;
        this.location = location;
    }

    public void deleteStore(final boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void addOperatingHours(final List<StoreOperatingHour> operatingHours) {
        this.operatingHours.addAll(operatingHours);
        for (StoreOperatingHour operatingHour : operatingHours) {
            operatingHour.setStore(this);
        }
    }

    public void addImages(final List<StoreImage> storeImages) {
        this.storeImages.addAll(storeImages);
        for (StoreImage storeImage : storeImages) {
            storeImage.setStore(this);
        }
    }

    public void operatingHoursClear() {
        operatingHours.clear();
    }
}