package com.management.event.service;

import com.management.event.dto.club.ClubSeniorTreasurerResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.User;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSeniorTreasurerService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClubSeniorTreasurerResponseDto> listAllSeniorTreasurers() {
        return userRepository.findByRoleName(AppRole.ROLE_SENIOR_TRESURER)
                .stream()
                .map(u -> toDto(u, clubRepository.findBySeniorTreasurerRegNumber(u.getRegNumber()).orElse(null)))
                .toList();
    }

    private ClubSeniorTreasurerResponseDto toDto(User user, Club club) {
        return ClubSeniorTreasurerResponseDto.builder()
                .regNumber(user.getRegNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .clubId(club != null ? club.getId() : null)
                .clubName(club != null ? club.getClubName() : null)
                .build();
    }
}
