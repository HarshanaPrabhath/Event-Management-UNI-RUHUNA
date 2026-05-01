package com.management.event.repository;

import com.management.event.entity.CalendarEvent;
import com.management.event.entity.CalendarEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    Optional<CalendarEvent> findByLetterId(Long letterId);

    List<CalendarEvent> findByEventDateBetweenOrderByEventDateAscEventTimeAsc(LocalDate from, LocalDate to);

    List<CalendarEvent> findByPlaceNameOrderByEventDateAscEventTimeAsc(String placeName);

    List<CalendarEvent> findByPlaceNameAndEventDateAndStatusIn(
            String placeName,
            LocalDate eventDate,
            Collection<CalendarEventStatus> statuses
    );

    List<CalendarEvent> findByPlaceNameAndEventDateAndEventTimeAndStatusIn(
            String placeName,
            LocalDate eventDate,
            LocalTime eventTime,
            Collection<CalendarEventStatus> statuses
    );

    List<CalendarEvent> findByPlaceNameAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
            String placeName,
            LocalDate from,
            LocalDate to
    );
}
