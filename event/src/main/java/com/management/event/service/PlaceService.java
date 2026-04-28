package com.management.event.service;

import com.management.event.dto.PlaceResponsiblePersonResponseDto;
import com.management.event.dto.PlaceSendDto;
import com.management.event.entity.Place;
import com.management.event.entity.User;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
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
    public List<PlaceSendDto> getPlaces() {

        List<Place> places = placeRepository.findAll();

        if (places.isEmpty()) {
            throw new ApiException("No places found");
        }

        return places.stream()
                .map(place -> new PlaceSendDto(
                        place.getPlaceId(),
                        place.getPlaceName(),
                        place.getDepartment(),
                        place.getCapacity()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaceResponsiblePersonResponseDto getResponsiblePersonByPlaceName(String placeName) {
        Place place = placeRepository.findByPlaceName(placeName.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Place", "name", placeName));

        User responsible = place.getResponsiblePerson();
        if (responsible == null) {
            throw new ApiException("No responsible person is assigned to this place");
        }

        return PlaceResponsiblePersonResponseDto.builder()
                .placeId(place.getPlaceId())
                .placeName(place.getPlaceName())
                .responsiblePersonRegNumber(responsible.getRegNumber())
                .responsiblePersonName(responsible.getUserName())
                .message("For this place, your letter should go through " + responsible.getUserName() + " first.")
                .build();
    }
}
