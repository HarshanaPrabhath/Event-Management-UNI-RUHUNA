package com.management.event.service;

import com.management.event.dto.CalendarConflictResponseDto;
import com.management.event.dto.CalendarEventResponseDto;
import com.management.event.entity.CalendarEvent;
import com.management.event.entity.CalendarEventStatus;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.Place;
import com.management.event.exception.ApiException;
import com.management.event.exception.CalendarConflictException;
import com.management.event.repository.CalendarEventRepository;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final PlaceRepository placeRepository;
    private final LetterRepository letterRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponseDto> getEvents(LocalDate from, LocalDate to, String placeName, boolean includePending) {
        List<CalendarEvent> events;
        boolean noRange = from == null && to == null;
        if (noRange) {
            if (StringUtils.hasText(placeName)) {
                events = calendarEventRepository.findByPlaceNameOrderByEventDateAscEventTimeAsc(placeName.trim());
            } else {
                events = calendarEventRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing(CalendarEvent::getEventDate)
                                .thenComparing(CalendarEvent::getEventTime)
                                .thenComparing(CalendarEvent::getId))
                        .toList();
            }
        } else {
            LocalDate effectiveFrom = from != null ? from : LocalDate.now();
            LocalDate effectiveTo = to != null ? to : effectiveFrom.plusMonths(1);
            if (StringUtils.hasText(placeName)) {
                events = calendarEventRepository.findByPlaceNameAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
                        placeName.trim(), effectiveFrom, effectiveTo
                );
            } else {
                events = calendarEventRepository.findByEventDateBetweenOrderByEventDateAscEventTimeAsc(effectiveFrom, effectiveTo);
            }
        }

        return events.stream()
                .filter(e -> includePending || e.getStatus() == CalendarEventStatus.APPROVED)
                .map(this::toDto)
                .toList();
    }

    // Creates missing calendar_events for older letters (ex: approvals done before calendar feature was added).
    // If includePending=true, it will also backfill PENDING_BOOKING letters so internal users can see them.
    @Transactional
    public void backfillCalendarEvents(LocalDate from, LocalDate to, boolean includePending) {
        boolean noRange = from == null && to == null;
        Set<LetterStatus> statuses = includePending
                ? Set.of(LetterStatus.APPROVED, LetterStatus.PENDING_BOOKING)
                : Set.of(LetterStatus.APPROVED);

        List<Letter> letters;
        if (noRange) {
            letters = letterRepository.findByGlobalStatusInOrderByEventDateAscEventTimeAsc(statuses);
        } else {
            LocalDate effectiveFrom = from != null ? from : LocalDate.now();
            LocalDate effectiveTo = to != null ? to : effectiveFrom.plusMonths(1);
            letters = letterRepository.findByGlobalStatusInAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
                    statuses, effectiveFrom, effectiveTo
            );
        }

        for (Letter letter : letters) {
            if (letter.getEventDate() == null || letter.getEventTime() == null) {
                continue;
            }
            CalendarEvent existing = calendarEventRepository.findByLetterId(letter.getId()).orElse(null);
            if (existing != null) {
                // Patch legacy events that don't have an end time yet.
                if (existing.getEndTime() == null) {
                    LocalTime fallbackEnd = letter.getEventEndTime() != null
                            ? letter.getEventEndTime()
                            : normalizeEndTime(letter.getEventTime(), null);
                    existing.setEndTime(fallbackEnd);
                    calendarEventRepository.save(existing);
                }
                continue;
            }

            CalendarEvent event = new CalendarEvent();
            event.setLetter(letter);
            event.setStatus(letter.getGlobalStatus() == LetterStatus.PENDING_BOOKING
                    ? CalendarEventStatus.PENDING_BOOKING
                    : CalendarEventStatus.APPROVED);
            event.setTitle(nullToEmpty(letter.getTitle()));
            event.setDescription(letter.getDescription());
            event.setEventDate(letter.getEventDate());
            event.setEventTime(letter.getEventTime());
            LocalTime end = letter.getEventEndTime() != null ? letter.getEventEndTime() : normalizeEndTime(letter.getEventTime(), null);
            event.setEndTime(end);
            String placeName = StringUtils.hasText(letter.getEventPlace()) ? letter.getEventPlace().trim() : null;
            event.setPlaceName(placeName);
            event.setPlace(placeName == null ? null : resolvePlace(placeName));
            calendarEventRepository.save(event);
        }
    }

    @Transactional(readOnly = true)
    public CalendarConflictResponseDto checkConflict(String placeName, LocalDate eventDate, LocalTime startTime, LocalTime endTime) {
        if (!StringUtils.hasText(placeName)) {
            throw new ApiException("placeName is required");
        }
        if (eventDate == null) {
            throw new ApiException("eventDate is required");
        }
        if (startTime == null) {
            throw new ApiException("eventTime (startTime) is required");
        }
        if (endTime == null) {
            throw new ApiException("endTime is required");
        }

        validateTimeRangeOrThrow(startTime, endTime);
        List<CalendarEventResponseDto> conflicts = findOverlaps(placeName.trim(), eventDate, startTime, endTime, null);

        boolean hasConflict = !conflicts.isEmpty();
        return CalendarConflictResponseDto.builder()
                .conflict(hasConflict)
                .message(hasConflict ? "This place is already booked for the selected date/time" : "No conflict")
                .conflicts(conflicts)
                .build();
    }

    @Transactional
    public void ensurePendingBookingForLetterOrThrow(Letter letter) {
        validateLetterHasBookingFields(letter);

        if (!StringUtils.hasText(letter.getEventPlace())) {
            throw new ApiException("Place name is required to create a pending booking");
        }

        assertNoConflictOrThrow(letter.getId(), letter.getEventPlace().trim(), letter.getEventDate(), letter.getEventTime(), letter.getEventEndTime());

        CalendarEvent existing = calendarEventRepository.findByLetterId(letter.getId()).orElse(null);
        if (existing != null) {
            existing.setStatus(CalendarEventStatus.PENDING_BOOKING);
            existing.setTitle(nullToEmpty(letter.getTitle()));
            existing.setDescription(letter.getDescription());
            existing.setEventDate(letter.getEventDate());
            existing.setEventTime(letter.getEventTime());
            existing.setEndTime(letter.getEventEndTime());
            existing.setPlaceName(letter.getEventPlace().trim());
            existing.setPlace(resolvePlace(letter.getEventPlace()));
            calendarEventRepository.save(existing);
            return;
        }

        CalendarEvent event = new CalendarEvent();
        event.setLetter(letter);
        event.setStatus(CalendarEventStatus.PENDING_BOOKING);
        event.setTitle(nullToEmpty(letter.getTitle()));
        event.setDescription(letter.getDescription());
        event.setEventDate(letter.getEventDate());
        event.setEventTime(letter.getEventTime());
        event.setEndTime(letter.getEventEndTime());
        event.setPlaceName(letter.getEventPlace().trim());
        event.setPlace(resolvePlace(letter.getEventPlace()));
        calendarEventRepository.save(event);
    }

    @Transactional
    public void markApprovedForLetterOrThrow(Letter letter) {
        validateLetterHasBookingFields(letter);

        String placeName = StringUtils.hasText(letter.getEventPlace()) ? letter.getEventPlace().trim() : null;
        if (placeName != null) {
            assertNoConflictOrThrow(letter.getId(), placeName, letter.getEventDate(), letter.getEventTime(), letter.getEventEndTime());
        }

        CalendarEvent existing = calendarEventRepository.findByLetterId(letter.getId()).orElse(null);
        if (existing != null) {
            existing.setStatus(CalendarEventStatus.APPROVED);
            existing.setTitle(nullToEmpty(letter.getTitle()));
            existing.setDescription(letter.getDescription());
            existing.setEventDate(letter.getEventDate());
            existing.setEventTime(letter.getEventTime());
            existing.setEndTime(letter.getEventEndTime());
            existing.setPlaceName(placeName);
            existing.setPlace(placeName == null ? null : resolvePlace(placeName));
            calendarEventRepository.save(existing);
            return;
        }

        CalendarEvent event = new CalendarEvent();
        event.setLetter(letter);
        event.setStatus(CalendarEventStatus.APPROVED);
        event.setTitle(nullToEmpty(letter.getTitle()));
        event.setDescription(letter.getDescription());
        event.setEventDate(letter.getEventDate());
        event.setEventTime(letter.getEventTime());
        event.setEndTime(letter.getEventEndTime());
        event.setPlaceName(placeName);
        event.setPlace(placeName == null ? null : resolvePlace(placeName));
        calendarEventRepository.save(event);
    }

    @Transactional
    public void deleteByLetterId(Long letterId) {
        CalendarEvent existing = calendarEventRepository.findByLetterId(letterId).orElse(null);
        if (existing != null) {
            calendarEventRepository.delete(existing);
        }
    }

    @Transactional(readOnly = true)
    public void assertSlotAvailableOrThrow(String placeName, LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeLetterId) {
        if (!StringUtils.hasText(placeName)) {
            throw new ApiException("placeName is required");
        }
        if (eventDate == null) {
            throw new ApiException("eventDate is required");
        }
        if (startTime == null) {
            throw new ApiException("eventTime is required");
        }
        if (endTime == null) {
            throw new ApiException("eventEndTime is required");
        }
        assertNoConflictOrThrow(excludeLetterId, placeName.trim(), eventDate, startTime, endTime);
    }

    private void validateLetterHasBookingFields(Letter letter) {
        if (letter.getEventDate() == null) {
            throw new ApiException("eventDate is required");
        }
        if (letter.getEventTime() == null) {
            throw new ApiException("eventTime is required");
        }
        if (letter.getEventEndTime() == null) {
            throw new ApiException("eventEndTime is required");
        }
        validateTimeRangeOrThrow(letter.getEventTime(), letter.getEventEndTime());
    }

    private void assertNoConflictOrThrow(Long currentLetterIdOrNull, String placeName, LocalDate date, LocalTime start, LocalTime end) {
        validateTimeRangeOrThrow(start, end);
        List<CalendarEventResponseDto> conflicts = findOverlaps(placeName.trim(), date, start, end, currentLetterIdOrNull);

        if (!conflicts.isEmpty()) {
            throw new CalendarConflictException("Place is already booked for this date/time", conflicts);
        }
    }

    private Place resolvePlace(String placeName) {
        if (!StringUtils.hasText(placeName)) {
            return null;
        }
        return placeRepository.findByPlaceName(placeName.trim()).orElse(null);
    }

    private CalendarEventResponseDto toDto(CalendarEvent event) {
        return CalendarEventResponseDto.builder()
                .calendarEventId(event.getId())
                .letterId(event.getLetter() != null ? event.getLetter().getId() : null)
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventTime(event.getEventTime())
                .endTime(event.getEndTime())
                .placeName(event.getPlaceName())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void validateTimeRangeOrThrow(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new ApiException("Both start and end times are required");
        }
        if (!start.isBefore(end)) {
            throw new ApiException("eventEndTime must be after eventTime");
        }
    }

    private List<CalendarEventResponseDto> findOverlaps(
            String placeName,
            LocalDate eventDate,
            LocalTime newStart,
            LocalTime newEnd,
            Long excludeLetterId
    ) {
        List<CalendarEvent> candidates = calendarEventRepository.findByPlaceNameAndEventDateAndStatusIn(
                placeName.trim(),
                eventDate,
                Set.of(CalendarEventStatus.PENDING_BOOKING, CalendarEventStatus.APPROVED)
        );

        return candidates.stream()
                .filter(ev -> excludeLetterId == null
                        || ev.getLetter() == null
                        || ev.getLetter().getId() == null
                        || !ev.getLetter().getId().equals(excludeLetterId))
                .filter(ev -> {
                    LocalTime existingStart = ev.getEventTime();
                    LocalTime existingEnd = normalizeEndTime(ev.getEventTime(), ev.getEndTime());
                    // overlap check for [start,end)
                    return existingStart.isBefore(newEnd) && newStart.isBefore(existingEnd);
                })
                .map(this::toDto)
                .toList();
    }

    private static LocalTime normalizeEndTime(LocalTime start, LocalTime end) {
        if (start == null) {
            return end; // should not happen for stored events
        }
        if (end != null && start.isBefore(end)) {
            return end;
        }
        // Legacy rows (no end time): assume 1 hour duration.
        return start.plusHours(1);
    }
}
