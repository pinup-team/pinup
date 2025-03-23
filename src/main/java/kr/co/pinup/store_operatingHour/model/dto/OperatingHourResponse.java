package kr.co.pinup.store_operatingHour.model.dto;

import java.time.LocalTime;

public record OperatingHourResponse(
        String day,
        LocalTime startTime,
        LocalTime endTime
) {}