package kr.co.pinup.storeoperatinghour.model.dto;

import java.time.LocalTime;

public record StoreOperatingHourResponse(
        String day,
        LocalTime startTime,
        LocalTime endTime
) {
}