package kr.co.pinup.storeoperatinghour;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.stores.Store;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOperatingHour extends BaseEntity {

    @Column(nullable = false)
    private String day;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder
    private StoreOperatingHour(
            final String day,
            final LocalTime startTime,
            final LocalTime endTime,
            final Store store
    ) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.store = store;
    }

}
