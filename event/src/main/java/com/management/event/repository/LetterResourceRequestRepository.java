package com.management.event.repository;

import com.management.event.entity.LetterResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LetterResourceRequestRepository extends JpaRepository<LetterResourceRequest, Long> {
    // Candidates for "is any of this resource already committed on this date" - overlap-in-time,
    // status, and per-request approval are all checked in Java afterwards (see
    // ResourceAvailabilityService), same as CalendarEventService does for venue bookings.
    List<LetterResourceRequest> findByResourceIdAndGeneralResourceAndLetter_EventDate(
            Long resourceId, boolean generalResource, LocalDate eventDate);
}
