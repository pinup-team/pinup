package kr.co.pinup.stores.scheduler;

import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class StoreStatusScheduler {

    private final StoreRepository storeRepository;

    private final Supplier<LocalDate> todaySupplier;

    public StoreStatusScheduler(
            final StoreRepository storeRepository,
            @Qualifier("todaySupplier") final Supplier<LocalDate> todaySupplier
    ) {
        this.storeRepository = storeRepository;
        this.todaySupplier = todaySupplier;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateStoreStatuses() {
        final LocalDate today = todaySupplier.get();

        final List<Store> stores = storeRepository.findByStoreStatusInAndIsDeletedFalse(
                List.of(StoreStatus.PENDING, StoreStatus.RESOLVED)
        );

        final long updatedCount = stores.stream()
                .filter(store -> {
                    final StoreStatus changeStoreStatus = calculateStoreStatus(store, today);
                    if (store.getStoreStatus() != changeStoreStatus) {
                        store.updateStatus(changeStoreStatus);
                        log.info("스토어 [{}] 상태 {}로 변경", store.getId(), changeStoreStatus);
                        return true;
                    }

                    return false;
                })
                .count();

        log.info("총 {}개의 스토어 상태를 갱신했습니다.", updatedCount);
    }

    private StoreStatus calculateStoreStatus(final Store store, final LocalDate today) {
        final StoreStatus storeStatus = store.getStoreStatus();
        if (storeStatus == StoreStatus.PENDING &&
                !store.getStartDate().isAfter(today)) {
            return StoreStatus.RESOLVED;
        }

        if (storeStatus == StoreStatus.RESOLVED &&
                store.getEndDate().isBefore(today)) {
            return StoreStatus.DISMISSED;
        }

        return storeStatus;
    }
}
