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

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSeniorTreasurerService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    // Lecturers are eligible to be picked as a club's senior treasurer too (a lecturer, including
    // a HOD, can hold both roles at once) - so the candidate list is every existing senior
    // treasurer PLUS every lecturer, not just users who already carry ROLE_SENIOR_TRESURER.
    // Whoever the admin actually picks gets ROLE_SENIOR_TRESURER auto-added by
    // ClubAdminService.assignSeniorTreasurer(), so no extra seeding is needed here.
    @Transactional(readOnly = true)
    public List<ClubSeniorTreasurerResponseDto> listAllSeniorTreasurers() {
        return userRepository.findByRoleNameIn(List.of(AppRole.ROLE_SENIOR_TRESURER, AppRole.ROLE_LECTURER))
                .stream()
                .sorted(Comparator.comparing(User::getUserName, String.CASE_INSENSITIVE_ORDER))
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
