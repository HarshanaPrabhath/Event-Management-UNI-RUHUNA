package com.management.event.service;

import com.management.event.dto.club.ClubSecretaryResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.ClubSecretary;
import com.management.event.entity.User;
import com.management.event.repository.ClubSecretaryRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSecretaryService {

    private final ClubSecretaryRepository clubSecretaryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClubSecretaryResponseDto> listAllSecretaries() {
        Map<String, ClubSecretary> clubByReg = clubSecretaryRepository.findAllWithUserAndClub()
                .stream()
                .collect(Collectors.toMap(cs -> cs.getUser().getRegNumber(), cs -> cs));

        return userRepository.findByRoleName(AppRole.ROLE_SECRETARY)
                .stream()
                .map(u -> toDto(u, clubByReg.get(u.getRegNumber())))
                .toList();
    }

    private ClubSecretaryResponseDto toDto(User user, ClubSecretary cs) {
        return ClubSecretaryResponseDto.builder()
                .regNumber(user.getRegNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .clubId(cs != null ? cs.getClub().getId() : null)
                .clubName(cs != null ? cs.getClub().getClubName() : null)
                .assignedAt(cs != null ? cs.getCreatedAt() : null)
                .build();
    }
}

