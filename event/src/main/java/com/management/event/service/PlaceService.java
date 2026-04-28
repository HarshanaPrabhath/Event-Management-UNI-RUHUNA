package com.management.event.service;

import com.management.event.entity.Place;
import com.management.event.exception.ApiException;
import com.management.event.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public List<Place> getPlaces() {
        List<Place> places = placeRepository.findAll();
        if (places.isEmpty()) {
            throw new ApiException("No places found");
        }
        return places;
    }
}
