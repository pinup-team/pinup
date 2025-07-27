package kr.co.pinup.stores.scheduler;

import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreStatusScheduler {

    private final StoreRepository storeRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateStoreStatuses() {
        List<Store> stores = storeRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Store store : stores) {
            StoreStatus newStatus;

            if (store.getStartDate().isAfter(today)) {
                newStatus = StoreStatus.PENDING;
            } else if (store.getEndDate().isBefore(today)) {
                newStatus = StoreStatus.DISMISSED;
            } else {
                newStatus = StoreStatus.RESOLVED;
            }

            if (store.getStoreStatus() != newStatus) {
                store.updateStatus(newStatus);
                log.info("스토어 [{}] 상태 {}로 변경", store.getId(), newStatus);
            }
        }

        storeRepository.saveAll(stores);
    }
}
