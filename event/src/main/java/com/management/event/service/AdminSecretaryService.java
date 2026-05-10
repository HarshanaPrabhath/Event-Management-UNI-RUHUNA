package com.management.event.service;

import com.management.event.dto.club.ClubSecretaryResponseDto;
import com.management.event.entity.ClubSecretary;
import com.management.event.repository.ClubSecretaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSecretaryService {

    private final ClubSecretaryRepository clubSecretaryRepository;

    @Transactional(readOnly = true)
    public List<ClubSecretaryResponseDto> listAllSecretaries() {
        return clubSecretaryRepository.findAllWithUserAndClub()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ClubSecretaryResponseDto toDto(ClubSecretary cs) {
        return ClubSecretaryResponseDto.builder()
                .regNumber(cs.getUser().getRegNumber())
                .userName(cs.getUser().getUserName())
                .email(cs.getUser().getEmail())
                .clubId(cs.getClub().getId())
                .clubName(cs.getClub().getClubName())
                .assignedAt(cs.getCreatedAt())
                .build();
    }
}

