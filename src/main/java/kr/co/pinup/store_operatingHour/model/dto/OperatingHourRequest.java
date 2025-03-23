package kr.co.pinup.store_operatingHour.model.dto;

import java.time.LocalTime;

public record OperatingHourRequest(
        String day,
        LocalTime startTime,
        LocalTime endTime
) {}
