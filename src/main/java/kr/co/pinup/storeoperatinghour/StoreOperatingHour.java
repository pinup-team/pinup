package kr.co.pinup.storeoperatinghour;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.stores.Store;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOperatingHour extends BaseEntity {

    @Column(nullable = false)
    private String days;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder
    private StoreOperatingHour(
            final String days,
            final LocalTime startTime,
            final LocalTime endTime,
            final Store store
    ) {
        this.days = days;
        this.startTime = startTime;
        this.endTime = endTime;
        this.store = store;
    }

}
