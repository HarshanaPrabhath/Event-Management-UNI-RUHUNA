package com.management.event.service;

import com.management.event.entity.Letter;
import com.management.event.entity.LetterResourceRequest;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.StepStatus;
import com.management.event.repository.LetterResourceRequestRepository;
import com.management.event.repository.WorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Shared equipment (PlaceResource/GeneralResource) can be double-booked by two events at
 * different venues that happen to overlap in time - nothing about a venue conflict catches that.
 * This mirrors how {@link CalendarEventService} treats a venue: a request doesn't lock in its
 * share of the pool the moment it's submitted, only once the resource's own responsible person
 * actually approves their step (same moment a venue becomes PENDING_BOOKING). Two callers use
 * this: LetterService checks it (a) as a fast pre-check when a request is submitted/resent, and
 * (b) as the authoritative check right before a resource-owning step is actually approved.
 */
@Service
@RequiredArgsConstructor
public class ResourceAvailabilityService {

    private final LetterResourceRequestRepository letterResourceRequestRepository;
    private final WorkflowStepRepository workflowStepRepository;

    /**
     * How many units of this resource are already locked in by OTHER active letters whose event
     * overlaps [startTime, endTime) on eventDate. "Locked in" = the specific request's responsible
     * person has approved their step on that letter's live resend round.
     */
    @Transactional(readOnly = true)
    public int committedElsewhere(
            Long resourceId, boolean generalResource, LocalDate eventDate,
            LocalTime startTime, LocalTime endTime, Long excludeLetterId
    ) {
        if (resourceId == null || eventDate == null || startTime == null || endTime == null) {
            return 0;
        }

        int committed = 0;
        for (LetterResourceRequest row : letterResourceRequestRepository
                .findByResourceIdAndGeneralResourceAndLetter_EventDate(resourceId, generalResource, eventDate)) {

            Letter candidate = row.getLetter();
            if (candidate == null || candidate.getId() == null) continue;
            if (excludeLetterId != null && excludeLetterId.equals(candidate.getId())) continue;

            LetterStatus status = candidate.getGlobalStatus();
            if (status == LetterStatus.REJECTED || status == LetterStatus.CANCELLED
                    || status == LetterStatus.RETURNED_TO_SECRETARY) {
                continue;
            }

            LocalTime existingStart = candidate.getEventTime();
            if (existingStart == null) continue;
            LocalTime existingEnd = candidate.getEventEndTime() != null
                    ? candidate.getEventEndTime() : existingStart.plusHours(1);
            boolean overlaps = existingStart.isBefore(endTime) && startTime.isBefore(existingEnd);
            if (!overlaps) continue;

            if (!StringUtils.hasText(row.getResponsiblePersonRegNumber())) continue;
            if (row.getQuantityRequested() == null) continue;

            if (isApprovedByResponsible(candidate, row.getResponsiblePersonRegNumber())) {
                committed += row.getQuantityRequested();
            }
        }
        return committed;
    }

    /**
     * How many units of this resource OTHER still-live letters have asked for on an overlapping
     * slot, where the responsible person hasn't approved (or rejected) that request yet - i.e.
     * it's still sitting in someone's pending queue. This is a heads-up signal only: it never
     * blocks a submission, it just warns "you may be racing another club for this" - whichever
     * request the responsible person approves first is the one that actually locks the units in.
     */
    @Transactional(readOnly = true)
    public int pendingElsewhere(
            Long resourceId, boolean generalResource, LocalDate eventDate,
            LocalTime startTime, LocalTime endTime, Long excludeLetterId
    ) {
        if (resourceId == null || eventDate == null || startTime == null || endTime == null) {
            return 0;
        }

        int pending = 0;
        for (LetterResourceRequest row : letterResourceRequestRepository
                .findByResourceIdAndGeneralResourceAndLetter_EventDate(resourceId, generalResource, eventDate)) {

            Letter candidate = row.getLetter();
            if (candidate == null || candidate.getId() == null) continue;
            if (excludeLetterId != null && excludeLetterId.equals(candidate.getId())) continue;

            LetterStatus status = candidate.getGlobalStatus();
            if (status == LetterStatus.REJECTED || status == LetterStatus.CANCELLED
                    || status == LetterStatus.RETURNED_TO_SECRETARY) {
                continue;
            }

            LocalTime existingStart = candidate.getEventTime();
            if (existingStart == null) continue;
            LocalTime existingEnd = candidate.getEventEndTime() != null
                    ? candidate.getEventEndTime() : existingStart.plusHours(1);
            boolean overlaps = existingStart.isBefore(endTime) && startTime.isBefore(existingEnd);
            if (!overlaps) continue;

            if (!StringUtils.hasText(row.getResponsiblePersonRegNumber())) continue;
            if (row.getQuantityRequested() == null) continue;

            if (!isApprovedByResponsible(candidate, row.getResponsiblePersonRegNumber())) {
                pending += row.getQuantityRequested();
            }
        }
        return pending;
    }

    /** totalQuantity minus whatever's already committed elsewhere for that window (never negative). */
    @Transactional(readOnly = true)
    public int remaining(
            Integer totalQuantity, Long resourceId, boolean generalResource, LocalDate eventDate,
            LocalTime startTime, LocalTime endTime, Long excludeLetterId
    ) {
        int total = totalQuantity == null ? 0 : totalQuantity;
        int committed = committedElsewhere(resourceId, generalResource, eventDate, startTime, endTime, excludeLetterId);
        return Math.max(0, total - committed);
    }

    private boolean isApprovedByResponsible(Letter letter, String responsiblePersonRegNumber) {
        return workflowStepRepository
                .findByLetterIdAndResendRoundOrderByStepOrderAsc(letter.getId(), letter.getResendCount())
                .stream()
                .anyMatch(s -> s.getUser() != null
                        && responsiblePersonRegNumber.equals(s.getUser().getRegNumber())
                        && s.getStatus() == StepStatus.APPROVED);
    }
}
