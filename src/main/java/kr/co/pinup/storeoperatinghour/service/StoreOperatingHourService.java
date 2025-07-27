package kr.co.pinup.storeoperatinghour.service;

import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.stores.Store;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class StoreOperatingHourService {

    public List<StoreOperatingHour> createOperatingHours(
            final Store store,
            final List<StoreOperatingHourRequest> operatingHourRequest
    ) {
        return operatingHourRequest.stream()
                .map(operatingHour -> StoreOperatingHour.builder()
                        .days(operatingHour.days())
                        .startTime(operatingHour.startTime())
                        .endTime(operatingHour.endTime())
                        .store(store)
                        .build())
                .toList();
    }
}
