package com.management.event.controller;

import com.management.event.dto.club.ClubResponseDto;
import com.management.event.entity.Club;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.ClubSecretaryRepository;
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
@RequestMapping("/api/public/clubs")
public class PublicClubController {

    private final ClubRepository clubRepository;
    private final ClubSecretaryRepository clubSecretaryRepository;
    private final UploadUrlMapper uploadUrlMapper;

    @GetMapping
    public ResponseEntity<List<ClubResponseDto>> list() {
        List<ClubResponseDto> out = clubRepository.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{clubName}")
    public ResponseEntity<ClubResponseDto> get(@PathVariable String clubName) {
        Club club = clubRepository.findByClubName(clubName)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "clubName", clubName));
        return ResponseEntity.ok(toDto(club));
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
}
