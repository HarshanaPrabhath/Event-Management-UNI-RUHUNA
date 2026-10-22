package com.management.event.controller;

import com.management.event.dto.GeneralResourceResponseDto;
import com.management.event.entity.GeneralResource;
import com.management.event.entity.User;
import com.management.event.repository.GeneralResourceRepository;
import com.management.event.service.ResourceAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Read-only listing for any authenticated user (e.g. a secretary picking equipment for a letter).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/general-resources")
public class GeneralResourceController {

    private final GeneralResourceRepository generalResourceRepository;
    private final ResourceAvailabilityService resourceAvailabilityService;

    // eventDate/startTime/endTime are optional - when given, remainingQuantity reflects what's
    // still free for that slot instead of the raw total. excludeLetterId excludes a letter's own
    // earlier claim (used when re-listing resources on that letter's resend form).
    @GetMapping
    public ResponseEntity<List<GeneralResourceResponseDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeLetterId
    ) {
        List<GeneralResourceResponseDto> out = generalResourceRepository.findAll().stream()
                .map(r -> toDto(r, eventDate, startTime, endTime, excludeLetterId))
                .toList();
        return ResponseEntity.ok(out);
    }

    private GeneralResourceResponseDto toDto(
            GeneralResource r, LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeLetterId
    ) {
        User responsible = r.getResponsiblePerson();
        boolean hasSlot = eventDate != null && startTime != null && endTime != null;
        int remaining = hasSlot
                ? resourceAvailabilityService.remaining(r.getQuantity(), r.getId(), true, eventDate, startTime, endTime, excludeLetterId)
                : (r.getQuantity() == null ? 0 : r.getQuantity());
        int pending = hasSlot
                ? resourceAvailabilityService.pendingElsewhere(r.getId(), true, eventDate, startTime, endTime, excludeLetterId)
                : 0;

        return GeneralResourceResponseDto.builder()
                .id(r.getId())
                .name(r.getName())
                .quantity(r.getQuantity())
                .remainingQuantity(remaining)
                .pendingElsewhere(pending)
                .responsiblePersonRegNumber(responsible != null ? responsible.getRegNumber() : null)
                .responsiblePersonName(responsible != null ? responsible.getUserName() : null)
                .build();
    }
}
