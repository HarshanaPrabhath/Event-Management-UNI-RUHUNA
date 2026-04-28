package com.management.event.controller;

import com.management.event.entity.Place;
import com.management.event.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<List<Place>> getPlaces() {
        return ResponseEntity.ok(placeService.getPlaces());
    }
}
