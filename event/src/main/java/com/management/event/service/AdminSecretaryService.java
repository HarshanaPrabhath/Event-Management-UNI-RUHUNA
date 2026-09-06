package com.management.event.service;

import com.management.event.dto.club.ClubSecretaryResponseDto;
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
public class AdminSecretaryService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClubSecretaryResponseDto> listAllSecretaries() {
        return userRepository.findByRoleName(AppRole.ROLE_SECRETARY)
                .stream()
                .map(u -> toDto(u, clubRepository.findBySecretaryRegNumber(u.getRegNumber()).orElse(null)))
                .toList();
    }

    private ClubSecretaryResponseDto toDto(User user, Club club) {
        return ClubSecretaryResponseDto.builder()
                .regNumber(user.getRegNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .clubId(club != null ? club.getId() : null)
                .clubName(club != null ? club.getClubName() : null)
                .build();
    }
}
