package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.CalendarConflictResponseDto;
import com.management.event.dto.CalendarEventResponseDto;
import com.management.event.service.CalendarEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarEventService calendarEventService;
    private final AuthenticatedUser authenticatedUser;

    // If from/to are omitted, returns all events. By default only APPROVED events are returned.
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponseDto>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String placeName,
            @RequestParam(required = false) Boolean includePending
    ) {
        boolean isInternalUser = isAuthenticatedSystemUser();

        boolean effectiveIncludePending;
        if (!isInternalUser) {
            // Public viewers only see approved events.
            effectiveIncludePending = false;
        } else {
            // Signed-in system users see pending bookings too by default.
            effectiveIncludePending = includePending == null || includePending;
        }

        // Ensure older letters also appear in the calendar.
        calendarEventService.backfillCalendarEvents(from, to, effectiveIncludePending);

        return ResponseEntity.ok(calendarEventService.getEvents(from, to, placeName, effectiveIncludePending));
    }

    @GetMapping("/conflicts")
    public ResponseEntity<CalendarConflictResponseDto> checkConflict(
            @RequestParam String placeName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime eventTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return ResponseEntity.ok(calendarEventService.checkConflict(placeName, eventDate, eventTime, endTime));
    }

    private boolean isAuthenticatedSystemUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }
        try {
            authenticatedUser.getAuthenticatedUser();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
