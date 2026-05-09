package com.management.event.controller;

import com.management.event.dto.club.ClubResponseDto;
import com.management.event.dto.club.SecretaryClubUpdateRequestDto;
import com.management.event.service.ClubSecretaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/club")
public class SecretaryClubController {

    private final ClubSecretaryService clubSecretaryService;

    @GetMapping
    public ResponseEntity<ClubResponseDto> getMyClub() {
        return ResponseEntity.ok(clubSecretaryService.getMyClub());
    }

    @PutMapping
    public ResponseEntity<ClubResponseDto> updateMyClub(@RequestBody SecretaryClubUpdateRequestDto req) {
        return ResponseEntity.ok(clubSecretaryService.updateMyClub(req));
    }

    @PostMapping(value = "/bg-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClubResponseDto> uploadBgImage(@RequestParam("bgImage") MultipartFile bgImage) {
        return ResponseEntity.ok(clubSecretaryService.uploadMyClubBgImage(bgImage));
    }
}
