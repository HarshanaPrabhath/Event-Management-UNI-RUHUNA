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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(value = "/{letterId}/sign-approve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignLetterResponseDto> signAndApprove(
            @PathVariable Long letterId,
            @RequestPart("signature") MultipartFile signature,
            @RequestParam(value = "pageIndex", defaultValue = "0") int pageIndex,
            @RequestParam(value = "x", required = false) Float x,
            @RequestParam(value = "y", required = false) Float y,
            @RequestParam(value = "width", required = false) Float width,
            @RequestParam(value = "height", required = false) Float height,
            @RequestParam(value = "nx", required = false) Float nx,
            @RequestParam(value = "ny", required = false) Float ny,
            @RequestParam(value = "nw", required = false) Float nw,
            @RequestParam(value = "nh", required = false) Float nh,
            @RequestParam(value = "origin", defaultValue = "TOP_LEFT") String origin,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        SignLetterRequestDto signDto = new SignLetterRequestDto();
        signDto.setPageIndex(pageIndex);
        signDto.setX(x);
        signDto.setY(y);
        signDto.setWidth(width);
        signDto.setHeight(height);
        signDto.setNx(nx);
        signDto.setNy(ny);
        signDto.setNw(nw);
        signDto.setNh(nh);
        signDto.setOrigin(origin);

        SignApproveRequestDto request = new SignApproveRequestDto();
        request.setSignature(signDto);
        request.setRemarks(remarks);

        String signedPath = letterService.signAndApproveCurrentStep(letterId, request, signature);
        return ResponseEntity.ok(new SignLetterResponseDto(letterId, signedPath));
    }
}
