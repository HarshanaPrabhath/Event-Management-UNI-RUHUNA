package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarConflictResponseDto {
    private Boolean conflict;
    private String message;
    private List<CalendarEventResponseDto> conflicts;
}

