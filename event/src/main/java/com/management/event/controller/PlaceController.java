package com.management.event.controller;

import com.management.event.dto.PlaceResponsiblePersonResponseDto;
import com.management.event.dto.PlaceSendDto;
import com.management.event.entity.Place;
import com.management.event.service.PlaceService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    // eventDate/startTime/endTime are optional - when given, each resource's remainingQuantity
    // reflects what's still free for that slot instead of the raw total. excludeLetterId excludes
    // a letter's own earlier claim (used when re-listing resources on that letter's resend form).
    @GetMapping
    public ResponseEntity<List<PlaceSendDto>> getPlaces(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeLetterId
    ) {
        return ResponseEntity.ok(placeService.getPlaces(eventDate, startTime, endTime, excludeLetterId));
    }

    @GetMapping("/responsible-person")
    public ResponseEntity<PlaceResponsiblePersonResponseDto> getResponsiblePerson(
            @RequestParam String placeName) {
        return ResponseEntity.ok(placeService.getResponsiblePersonByPlaceName(placeName));
    }
}
