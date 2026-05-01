package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponseDto {
    private Long calendarEventId;
    private Long letterId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalTime endTime;
    private String placeName;
    private String status; // PENDING_BOOKING | APPROVED
}
