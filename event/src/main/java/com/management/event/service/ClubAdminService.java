package com.management.event.service;
import com.management.event.dto.club.AdminClubUpsertRequestDto;
import com.management.event.dto.club.ClubResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.ClubExecutive;
import com.management.event.entity.ClubExecutiveRole;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.ClubExecutiveRepository;
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
    private final ClubExecutiveRepository clubExecutiveRepository;
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
        if (!StringUtils.hasText(req.getSeniorTreasurerRegNumber())) throw new BadRequestException("seniorTreasurerRegNumber is required");
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

        assignExecutive(club.getId(), req.getSecretaryRegNumber().trim(), ClubExecutiveRole.SECRETARY);
        assignExecutive(club.getId(), req.getSeniorTreasurerRegNumber().trim(), ClubExecutiveRole.SENIOR_TREASURER);
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
                clubExecutiveRepository.findByClub_IdAndExecutiveRole(clubId, ClubExecutiveRole.SECRETARY)
                        .ifPresent(clubExecutiveRepository::delete);
            } else {
                assignExecutive(clubId, sec, ClubExecutiveRole.SECRETARY);
            }
        }
        if (req.getSeniorTreasurerRegNumber() != null) {
            // if empty => unassign, if non-empty => assign/replace
            String st = req.getSeniorTreasurerRegNumber().trim();
            if (st.isBlank()) {
                clubExecutiveRepository.findByClub_IdAndExecutiveRole(clubId, ClubExecutiveRole.SENIOR_TREASURER)
                        .ifPresent(clubExecutiveRepository::delete);
            } else {
                assignExecutive(clubId, st, ClubExecutiveRole.SENIOR_TREASURER);
            }
        }
        return toDto(club);
    }

    @Transactional
    public void delete(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        // Remove mappings first to avoid FK issues.
        clubExecutiveRepository.findByClub_IdAndExecutiveRole(clubId, ClubExecutiveRole.SECRETARY)
                .ifPresent(clubExecutiveRepository::delete);
        clubExecutiveRepository.findByClub_IdAndExecutiveRole(clubId, ClubExecutiveRole.SENIOR_TREASURER)
                .ifPresent(clubExecutiveRepository::delete);
        clubRepository.delete(club);
    }

    @Transactional
    public void assignExecutive(Long clubId, String regNumber, ClubExecutiveRole executiveRole) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        User user = userRepository.findByRegNumber(regNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", regNumber));

        // Ensure app-level role is present for the mapped executive role.
        AppRole appRole = switch (executiveRole) {
            case SECRETARY -> AppRole.ROLE_SECRETARY;
            case SENIOR_TREASURER -> AppRole.ROLE_SENIOR_TRESURER;
        };
        if (!RoleUtil.hasRole(user, appRole)) {
            Role role = roleRepository.findByRoleName(appRole)
                    .orElseThrow(() -> new BadRequestException(appRole.name() + " missing in role table"));
            user.getRoles().add(role);
            userRepository.save(user);
        }

        // Replace existing mapping for this club+role, and also enforce one club per role per user.
        clubExecutiveRepository.findByClub_IdAndExecutiveRole(clubId, executiveRole)
                .ifPresent(clubExecutiveRepository::delete);
        clubExecutiveRepository.findByUser_RegNumberAndExecutiveRole(user.getRegNumber(), executiveRole)
                .ifPresent(clubExecutiveRepository::delete);

        ClubExecutive ce = new ClubExecutive();
        ce.setClub(club);
        ce.setUser(user);
        ce.setExecutiveRole(executiveRole);
        ce.setCreatedAt(Instant.now());
        clubExecutiveRepository.save(ce);
    }

    private ClubResponseDto toDto(Club club) {
        String sec = clubExecutiveRepository.findByClub_IdAndExecutiveRole(club.getId(), ClubExecutiveRole.SECRETARY)
                .map(ce -> ce.getUser().getRegNumber())
                .orElse(null);
        String st = clubExecutiveRepository.findByClub_IdAndExecutiveRole(club.getId(), ClubExecutiveRole.SENIOR_TREASURER)
                .map(ce -> ce.getUser().getRegNumber())
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
                .seniorTreasurerRegNumber(st)
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
