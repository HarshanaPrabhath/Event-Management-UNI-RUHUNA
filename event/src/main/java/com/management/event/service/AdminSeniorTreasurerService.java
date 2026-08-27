package com.management.event.service;

import com.management.event.dto.club.ClubSeniorTreasurerResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.ClubExecutive;
import com.management.event.entity.ClubExecutiveRole;
import com.management.event.entity.User;
import com.management.event.repository.ClubExecutiveRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSeniorTreasurerService {

    private final ClubExecutiveRepository clubExecutiveRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClubSeniorTreasurerResponseDto> listAllSeniorTreasurers() {
        Map<String, ClubExecutive> clubByReg = clubExecutiveRepository.findAllWithUserAndClubByRole(ClubExecutiveRole.SENIOR_TREASURER)
                .stream()
                .collect(Collectors.toMap(ce -> ce.getUser().getRegNumber(), ce -> ce));

        return userRepository.findByRoleName(AppRole.ROLE_SENIOR_TRESURER)
                .stream()
                .map(u -> toDto(u, clubByReg.get(u.getRegNumber())))
                .toList();
    }

    private ClubSeniorTreasurerResponseDto toDto(User user, ClubExecutive cst) {
        return ClubSeniorTreasurerResponseDto.builder()
                .regNumber(user.getRegNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .clubId(cst != null ? cst.getClub().getId() : null)
                .clubName(cst != null ? cst.getClub().getClubName() : null)
                .assignedAt(cst != null ? cst.getCreatedAt() : null)
                .build();
    }
}
