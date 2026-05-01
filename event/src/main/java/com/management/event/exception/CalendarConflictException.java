package com.management.event.exception;

import com.management.event.dto.CalendarEventResponseDto;
import lombok.Getter;

import java.util.List;

@Getter
public class CalendarConflictException extends RuntimeException {
    private final List<CalendarEventResponseDto> conflicts;

    public CalendarConflictException(String message, List<CalendarEventResponseDto> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }
}

