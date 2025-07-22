package kr.co.pinup.storeoperatinghour.model.dto;

import java.time.LocalTime;

public record StoreOperatingHourResponse(
        String days,
        LocalTime startTime,
        LocalTime endTime
) {
}