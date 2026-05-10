package com.management.event.service;
import com.management.event.dto.club.AdminClubUpsertRequestDto;
import com.management.event.dto.club.ClubResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.ClubSecretary;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.ClubSecretaryRepository;
import com.management.event.repository.RoleRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubAdminService {

    private final ClubRepository clubRepository;
    private final ClubSecretaryRepository clubSecretaryRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UploadUrlMapper uploadUrlMapper;

    public ClubResponseDto getByName(String clubName) {
        Club club = clubRepository.findByClubName(clubName)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "clubName", clubName));
        return toDto(club);
    }

    public List<ClubResponseDto> listAll() {
        return clubRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public ClubResponseDto create(AdminClubUpsertRequestDto req) {
        if (req == null) throw new BadRequestException("Request body is required");
        if (!StringUtils.hasText(req.getClubName())) throw new BadRequestException("clubName is required");
        if (!StringUtils.hasText(req.getSecretaryRegNumber())) throw new BadRequestException("secretaryRegNumber is required");
        String name = req.getClubName().trim();
        if (clubRepository.existsByClubName(name)) throw new BadRequestException("clubName already exists");

        Club club = new Club();
        club.setClubName(name);
        // Admin registers the club; secretary fills in the rest later.
        club.setVision(null);
        club.setMission(null);
        club.setDescription(null);
        club.setExecutiveBoardJson(null);
        club.setCreatedAt(Instant.now());
        club.setUpdatedAt(Instant.now());
        club = clubRepository.save(club);

        assignSecretary(club.getId(), req.getSecretaryRegNumber().trim());
        return toDto(club);
    }

    @Transactional
    public ClubResponseDto update(Long clubId, AdminClubUpsertRequestDto req) {
        if (req == null) throw new BadRequestException("Request body is required");
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

        if (StringUtils.hasText(req.getClubName())) {
            String newName = req.getClubName().trim();
            if (!newName.equals(club.getClubName()) && clubRepository.existsByClubName(newName)) {
                throw new BadRequestException("clubName already exists");
            }
            club.setClubName(newName);
        }
        // Club details are managed by the secretary via /api/me/club.
        // Admin update only changes identity/assignment (clubName + secretaryRegNumber).
        club.setUpdatedAt(Instant.now());
        clubRepository.save(club);

        if (req.getSecretaryRegNumber() != null) {
            // if empty => unassign, if non-empty => assign/replace
            String sec = req.getSecretaryRegNumber().trim();
            if (sec.isBlank()) {
                clubSecretaryRepository.findByClub_Id(clubId).ifPresent(clubSecretaryRepository::delete);
            } else {
                assignSecretary(clubId, sec);
            }
        }
        return toDto(club);
    }

    @Transactional
    public void delete(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        // Remove secretary mapping first to avoid FK issues.
        clubSecretaryRepository.findByClub_Id(clubId).ifPresent(clubSecretaryRepository::delete);
        clubRepository.delete(club);
    }

    @Transactional
    public void assignSecretary(Long clubId, String secretaryRegNumber) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        User user = userRepository.findByRegNumber(secretaryRegNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", secretaryRegNumber));

        // Ensure ROLE_SECRETARY is present.
        if (!RoleUtil.hasRole(user, AppRole.ROLE_SECRETARY)) {
            Role secRole = roleRepository.findByRoleName(AppRole.ROLE_SECRETARY)
                    .orElseThrow(() -> new BadRequestException("ROLE_SECRETARY missing in role table"));
            user.getRoles().add(secRole);
            userRepository.save(user);
        }

        // Replace existing mapping for this club, and also enforce one club per secretary user.
        clubSecretaryRepository.findByClub_Id(clubId).ifPresent(clubSecretaryRepository::delete);
        clubSecretaryRepository.findByUser_RegNumber(user.getRegNumber()).ifPresent(clubSecretaryRepository::delete);

        ClubSecretary cs = new ClubSecretary();
        cs.setClub(club);
        cs.setUser(user);
        cs.setCreatedAt(Instant.now());
        clubSecretaryRepository.save(cs);
    }

    private ClubResponseDto toDto(Club club) {
        String sec = clubSecretaryRepository.findByClub_Id(club.getId())
                .map(cs -> cs.getUser().getRegNumber())
                .orElse(null);
        return ClubResponseDto.builder()
                .id(club.getId())
                .clubName(club.getClubName())
                .bgImageUrl(uploadUrlMapper.toPublicUrl(club.getBgImagePath()))
                .vision(club.getVision())
                .mission(club.getMission())
                .description(club.getDescription())
                .executiveBoardJson(club.getExecutiveBoardJson())
                .secretaryRegNumber(sec)
                .build();
    }

    private String normalizeJsonOrNull(String json) {
        if (json == null) return null;
        String t = json.trim();
        if (t.isBlank()) return null;
        // Keep it as-is; we don't validate JSON here to avoid extra dependencies.
        // Frontend/admin UI should provide a valid JSON string (e.g. {"president":"Harshana"}).
        return t;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
