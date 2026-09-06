package com.management.event.controller;

import com.management.event.dto.GeneralResourceResponseDto;
import com.management.event.entity.GeneralResource;
import com.management.event.entity.User;
import com.management.event.repository.GeneralResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only listing for any authenticated user (e.g. a secretary picking equipment for a letter).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/general-resources")
public class GeneralResourceController {

    private final GeneralResourceRepository generalResourceRepository;

    @GetMapping
    public ResponseEntity<List<GeneralResourceResponseDto>> list() {
        List<GeneralResourceResponseDto> out = generalResourceRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(out);
    }

    private GeneralResourceResponseDto toDto(GeneralResource r) {
        User responsible = r.getResponsiblePerson();
        return GeneralResourceResponseDto.builder()
                .id(r.getId())
                .name(r.getName())
                .quantity(r.getQuantity())
                .responsiblePersonRegNumber(responsible != null ? responsible.getRegNumber() : null)
                .responsiblePersonName(responsible != null ? responsible.getUserName() : null)
                .build();
    }
}
