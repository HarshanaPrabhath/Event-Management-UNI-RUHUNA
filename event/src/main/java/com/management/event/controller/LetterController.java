package com.management.event.controller;

import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.dto.LetterApproveRequestDto;
import com.management.event.dto.LetterRejectRequestDto;
import com.management.event.dto.LetterToApproveResponseDto;
import com.management.event.dto.SignLetterRequestDto;
import com.management.event.dto.SignLetterResponseDto;
import com.management.event.dto.SignApproveRequestDto;
import com.management.event.service.LetterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/letter")
public class LetterController {

    private final LetterService letterService;

    @GetMapping("/my")
    public ResponseEntity<List<LetterToApproveResponseDto>> getMyLetters() {
        return ResponseEntity.ok(letterService.getMyLetters());
    }

    @GetMapping("/rejected-by-me")
    public ResponseEntity<List<LetterToApproveResponseDto>> getRejectedByMe() {
        return ResponseEntity.ok(letterService.getRejectedByMe());
    }

    @GetMapping("/approved-by-me")
    public ResponseEntity<List<LetterToApproveResponseDto>> getApprovedByMe() {
        return ResponseEntity.ok(letterService.getApprovedByMe());
    }

    @GetMapping("/to-approve")
    public ResponseEntity<List<LetterToApproveResponseDto>> getLettersToApprove() {
        return ResponseEntity.ok(letterService.getLettersToApprove());
    }

    @PostMapping("/{letterId}/approve")
    public ResponseEntity<String> approveLetter(@PathVariable Long letterId,
                                                @RequestBody(required = false) LetterApproveRequestDto request) {
        letterService.approveLetter(letterId, request);
        return ResponseEntity.ok("Letter approved successfully");
    }

    @PostMapping("/{letterId}/reject")
    public ResponseEntity<String> rejectLetter(@PathVariable Long letterId, @Valid @RequestBody LetterRejectRequestDto request) {
        letterService.rejectLetter(letterId, request);
        return ResponseEntity.ok("Letter rejected successfully");
    }

    @PostMapping(value = "/place", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> placeLetter(@Valid @ModelAttribute LetterPlaceRequestDto letterPlaceRequestDto) {
        letterService.placeLetter(letterPlaceRequestDto);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/{letterId}/sign")
    public ResponseEntity<SignLetterResponseDto> signLetter(
            @PathVariable Long letterId,
            @Valid @RequestBody SignLetterRequestDto request
    ) {
        String signedPath = letterService.signLetter(letterId, request);
        return ResponseEntity.ok(new SignLetterResponseDto(letterId, signedPath));
    }

    // For steps that require a signature: stamp signature + approve + forward to next step.
    @PostMapping("/{letterId}/sign-approve")
    public ResponseEntity<SignLetterResponseDto> signAndApprove(
            @PathVariable Long letterId,
            @Valid @RequestBody(required = false) SignApproveRequestDto request
    ) {
        String signedPath = letterService.signAndApproveCurrentStep(letterId, request);
        return ResponseEntity.ok(new SignLetterResponseDto(letterId, signedPath));
    }
}
