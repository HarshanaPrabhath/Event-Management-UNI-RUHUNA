package com.management.event.service;
import com.management.event.dto.club.AdminClubUpsertRequestDto;
import com.management.event.dto.club.ClubResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
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
        club.setMembersJson(null);
        club.setCreatedAt(Instant.now());
        club.setUpdatedAt(Instant.now());
        club = clubRepository.save(club);

        assignSecretary(club, req.getSecretaryRegNumber().trim());
        assignSeniorTreasurer(club, req.getSeniorTreasurerRegNumber().trim());
        clubRepository.save(club);
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
        // Admin update only changes identity/assignment (clubName + secretary/senior treasurer).

        if (req.getSecretaryRegNumber() != null) {
            // if empty => unassign, if non-empty => assign/replace
            String sec = req.getSecretaryRegNumber().trim();
            if (sec.isBlank()) {
                club.setSecretaryRegNumber(null);
            } else {
                assignSecretary(club, sec);
            }
        }
        if (req.getSeniorTreasurerRegNumber() != null) {
            // if empty => unassign, if non-empty => assign/replace
            String st = req.getSeniorTreasurerRegNumber().trim();
            if (st.isBlank()) {
                club.setSeniorTreasurerRegNumber(null);
            } else {
                assignSeniorTreasurer(club, st);
            }
        }

        club.setUpdatedAt(Instant.now());
        clubRepository.save(club);
        return toDto(club);
    }

    @Transactional
    public void delete(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        clubRepository.delete(club);
    }

    private void assignSecretary(Club club, String regNumber) {
        User user = userRepository.findByRegNumber(regNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", regNumber));

        // Enforce one club per secretary: clear whichever other club currently holds this person.
        clubRepository.findBySecretaryRegNumber(regNumber)
                .filter(other -> !other.getId().equals(club.getId()))
                .ifPresent(other -> {
                    other.setSecretaryRegNumber(null);
                    clubRepository.save(other);
                });

        ensureRole(user, AppRole.ROLE_SECRETARY);
        club.setSecretaryRegNumber(regNumber);
    }

    private void assignSeniorTreasurer(Club club, String regNumber) {
        User user = userRepository.findByRegNumber(regNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", regNumber));

        // Enforce one club per senior treasurer: clear whichever other club currently holds this person.
        clubRepository.findBySeniorTreasurerRegNumber(regNumber)
                .filter(other -> !other.getId().equals(club.getId()))
                .ifPresent(other -> {
                    other.setSeniorTreasurerRegNumber(null);
                    clubRepository.save(other);
                });

        ensureRole(user, AppRole.ROLE_SENIOR_TRESURER);
        club.setSeniorTreasurerRegNumber(regNumber);
    }

    private void ensureRole(User user, AppRole appRole) {
        if (!RoleUtil.hasRole(user, appRole)) {
            Role role = roleRepository.findByRoleName(appRole)
                    .orElseThrow(() -> new BadRequestException(appRole.name() + " missing in role table"));
            user.getRoles().add(role);
            userRepository.save(user);
        }
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
}
