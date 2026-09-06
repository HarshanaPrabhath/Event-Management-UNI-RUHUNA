package com.management.event.controller;

import com.management.event.dto.club.ClubResponseDto;
import com.management.event.entity.Club;
import com.management.event.entity.User;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.UserRepository;
import com.management.event.service.UploadUrlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/clubs", "/api/public/clubs"})
public class PublicClubController {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final UploadUrlMapper uploadUrlMapper;

    @GetMapping
    public ResponseEntity<List<ClubResponseDto>> list() {
        List<ClubResponseDto> out = clubRepository.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{clubId:\\d+}")
    public ResponseEntity<ClubResponseDto> getById(@PathVariable Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        return ResponseEntity.ok(toDto(club));
    }

    @GetMapping("/by-name/{clubName}")
    public ResponseEntity<ClubResponseDto> getByName(@PathVariable String clubName) {
        Club club = clubRepository.findByClubName(clubName)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "clubName", clubName));
        return ResponseEntity.ok(toDto(club));
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
