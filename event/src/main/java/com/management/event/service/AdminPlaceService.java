package com.management.event.service;

import com.management.event.dto.PlaceResourceResponseDto;
import com.management.event.dto.PlaceResourceUpsertRequestDto;
import com.management.event.dto.PlaceSendDto;
import com.management.event.dto.PlaceUpsertRequestDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Place;
import com.management.event.entity.PlaceResource;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.PlaceResourceRepository;
import com.management.event.repository.RoleRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceResourceRepository placeResourceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PlaceFileStorageService placeFileStorageService;
    private final UploadUrlMapper uploadUrlMapper;

    @Transactional
    public PlaceSendDto create(PlaceUpsertRequestDto req) {
        if (!StringUtils.hasText(req.getPlaceName())) throw new BadRequestException("placeName is required");

        Place place = new Place();
        place.setPlaceName(req.getPlaceName().trim());
        place.setDepartment(req.getDepartment());
        place.setCapacity(req.getCapacity());
        place.setResponsiblePerson(resolveResponsiblePerson(req.getResponsiblePersonRegNumber()));
        place = placeRepository.save(place);

        syncResources(place, req.getResources());
        return toDto(place);
    }

    @Transactional
    public PlaceSendDto update(Long placeId, PlaceUpsertRequestDto req) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        if (StringUtils.hasText(req.getPlaceName())) {
            place.setPlaceName(req.getPlaceName().trim());
        }
        if (req.getDepartment() != null) place.setDepartment(req.getDepartment());
        if (req.getCapacity() != null) place.setCapacity(req.getCapacity());
        if (req.getResponsiblePersonRegNumber() != null) {
            place.setResponsiblePerson(resolveResponsiblePerson(req.getResponsiblePersonRegNumber()));
        }
        place = placeRepository.save(place);

        if (req.getResources() != null) {
            syncResources(place, req.getResources());
        }
        return toDto(place);
    }

    @Transactional
    public void delete(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        placeResourceRepository.deleteByPlace_PlaceId(placeId);
        placeRepository.delete(place);
    }

    @Transactional
    public PlaceSendDto uploadPhoto(Long placeId, MultipartFile photo) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        String storedPath = placeFileStorageService.storePlacePhoto(placeId, photo);
        place.setPhotoPath(storedPath);
        return toDto(placeRepository.save(place));
    }

    // Simplest way to keep a place's resource list in sync with the admin's edit form: replace
    // the whole set each time rather than diffing individual rows. Equipment listed here has no
    // responsible person of its own - it's always approved by the place's own responsible person.
    private void syncResources(Place place, List<PlaceResourceUpsertRequestDto> resources) {
        if (resources == null) return;

        placeResourceRepository.deleteByPlace_PlaceId(place.getPlaceId());
        placeResourceRepository.flush();

        for (PlaceResourceUpsertRequestDto item : resources) {
            if (!StringUtils.hasText(item.getName())) throw new BadRequestException("Resource name is required");
            if (item.getQuantity() == null || item.getQuantity() < 0) {
                throw new BadRequestException("Resource quantity must be zero or more");
            }

            PlaceResource resource = new PlaceResource();
            resource.setPlace(place);
            resource.setName(item.getName().trim());
            resource.setQuantity(item.getQuantity());
            placeResourceRepository.save(resource);
        }
    }

    // Picking someone as a place's responsible person is how they become a Technical Officer in
    // this system - grant the role automatically, same as assigning a club secretary/treasurer does.
    private User resolveResponsiblePerson(String regNumber) {
        if (!StringUtils.hasText(regNumber)) return null;
        User user = userRepository.findByRegNumber(regNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", regNumber));
        if (!RoleUtil.hasRole(user, AppRole.ROLE_TO)) {
            Role toRole = roleRepository.findByRoleName(AppRole.ROLE_TO)
                    .orElseThrow(() -> new BadRequestException("ROLE_TO missing in role table"));
            user.getRoles().add(toRole);
            userRepository.save(user);
        }
        return user;
    }

    private PlaceSendDto toDto(Place place) {
        User responsible = place.getResponsiblePerson();
        List<PlaceResourceResponseDto> resources = placeResourceRepository
                .findByPlace_PlaceId(place.getPlaceId())
                .stream()
                .map(r -> toResourceDto(r, responsible))
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

    private PlaceResourceResponseDto toResourceDto(PlaceResource r, User placeResponsible) {
        return PlaceResourceResponseDto.builder()
                .id(r.getId())
                .name(r.getName())
                .quantity(r.getQuantity())
                .responsiblePersonRegNumber(placeResponsible != null ? placeResponsible.getRegNumber() : null)
                .responsiblePersonName(placeResponsible != null ? placeResponsible.getUserName() : null)
                .build();
    }
}
