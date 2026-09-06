package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.club.ClubResponseDto;
import com.management.event.dto.club.SecretaryClubUpdateRequestDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ForbiddenException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ClubSecretaryService {

    private final AuthenticatedUser authenticatedUser;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubFileStorageService clubFileStorageService;
    private final UploadUrlMapper uploadUrlMapper;

    public ClubResponseDto getMyClub() {
        Club club = requireMyClub();
        return toDto(club);
    }

    @Transactional
    public ClubResponseDto updateMyClub(SecretaryClubUpdateRequestDto req) {
        if (req == null) throw new BadRequestException("Request body is required");
        Club club = requireMyClub();

        club.setVision(nullIfBlank(req.getVision()));
        club.setMission(nullIfBlank(req.getMission()));
        if (req.getDescription() != null) club.setDescription(req.getDescription());
        if (req.getExecutiveBoardJson() != null) club.setExecutiveBoardJson(req.getExecutiveBoardJson().trim());
        if (req.getMembersJson() != null) club.setMembersJson(req.getMembersJson().trim());

        club.setUpdatedAt(Instant.now());
        clubRepository.save(club);
        return toDto(club);
    }

    @Transactional
    public ClubResponseDto uploadMyClubBgImage(MultipartFile bgImage) {
        Club club = requireMyClub();
        String storedPath = clubFileStorageService.storeClubBgImage(club.getId(), bgImage);
        club.setBgImagePath(storedPath);
        club.setUpdatedAt(Instant.now());
        clubRepository.save(club);
        return toDto(club);
    }

    private Club requireMyClub() {
        User user = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(user, AppRole.ROLE_SECRETARY)) {
            throw new ForbiddenException("Secretary privileges required");
        }
        return clubRepository.findBySecretaryRegNumber(user.getRegNumber())
                .orElseThrow(() -> new ForbiddenException("No club assigned to this secretary"));
    }

    private ClubResponseDto toDto(Club club) {
        String secretaryName = club.getSecretaryRegNumber() == null ? null
                : userRepository.findByRegNumber(club.getSecretaryRegNumber()).map(User::getUserName).orElse(null);
        String treasurerName = club.getSeniorTreasurerRegNumber() == null ? null
                : userRepository.findByRegNumber(club.getSeniorTreasurerRegNumber()).map(User::getUserName).orElse(null);

        return ClubResponseDto.builder()
                .id(club.getId())
                .clubName(club.getClubName())
                .bgImageUrl(uploadUrlMapper.toPublicUrl(club.getBgImagePath()))
                .vision(club.getVision())
                .mission(club.getMission())
                .description(club.getDescription())
                .executiveBoardJson(club.getExecutiveBoardJson())
                .membersJson(club.getMembersJson())
                .secretaryRegNumber(club.getSecretaryRegNumber())
                .secretaryName(secretaryName)
                .seniorTreasurerRegNumber(club.getSeniorTreasurerRegNumber())
                .seniorTreasurerName(treasurerName)
                .build();
    }

    private static String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
