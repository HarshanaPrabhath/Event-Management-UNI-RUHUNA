package com.management.event.service;

import com.management.event.dto.PlaceResourceResponseDto;
import com.management.event.dto.PlaceResponsiblePersonResponseDto;
import com.management.event.dto.PlaceSendDto;
import com.management.event.entity.Place;
import com.management.event.entity.PlaceResource;
import com.management.event.entity.User;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.PlaceResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceResourceRepository placeResourceRepository;
    private final UploadUrlMapper uploadUrlMapper;
    private final ResourceAvailabilityService resourceAvailabilityService;

    @Transactional(readOnly = true)
    public List<PlaceSendDto> getPlaces() {
        return getPlaces(null, null, null, null);
    }

    // eventDate/startTime/endTime, when given, make each resource's remainingQuantity reflect
    // what's actually still free for that slot (excludeLetterId excludes a letter's own earlier
    // claim, e.g. when the resend form re-lists resources for the letter being edited).
    @Transactional(readOnly = true)
    public List<PlaceSendDto> getPlaces(LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeLetterId) {

        List<Place> places = placeRepository.findAll();

        if (places.isEmpty()) {
            return List.of();
        }

        return places.stream().map(place -> toDto(place, eventDate, startTime, endTime, excludeLetterId)).toList();
    }

    private PlaceSendDto toDto(Place place, LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeLetterId) {
        User responsible = place.getResponsiblePerson();
        List<PlaceResourceResponseDto> resources = placeResourceRepository
                .findByPlace_PlaceId(place.getPlaceId())
                .stream()
                .map(r -> toResourceDto(r, responsible, eventDate, startTime, endTime, excludeLetterId))
                .toList();

        return PlaceSendDto.builder()
                .placeId(place.getPlaceId())
                .placeName(place.getPlaceName())
                .department(place.getDepartment())
                .capacity(place.getCapacity())
                .photoUrl(uploadUrlMapper.toPublicUrl(place.getPhotoPath()))
                .responsiblePersonRegNumber(responsible != null ? responsible.getRegNumber() : null)
                .responsiblePersonName(responsible != null ? responsible.getUserName() : null)
                .resources(resources)
                .build();
    }

    private PlaceResourceResponseDto toResourceDto(
            PlaceResource r, User placeResponsible,
            LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeLetterId
    ) {
        boolean hasSlot = eventDate != null && startTime != null && endTime != null;
        int remaining = hasSlot
                ? resourceAvailabilityService.remaining(r.getQuantity(), r.getId(), false, eventDate, startTime, endTime, excludeLetterId)
                : (r.getQuantity() == null ? 0 : r.getQuantity());
        int pending = hasSlot
                ? resourceAvailabilityService.pendingElsewhere(r.getId(), false, eventDate, startTime, endTime, excludeLetterId)
                : 0;

        return PlaceResourceResponseDto.builder()
                .id(r.getId())
                .name(r.getName())
                .quantity(r.getQuantity())
                .remainingQuantity(remaining)
                .pendingElsewhere(pending)
                .responsiblePersonRegNumber(placeResponsible != null ? placeResponsible.getRegNumber() : null)
                .responsiblePersonName(placeResponsible != null ? placeResponsible.getUserName() : null)
                .build();
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
