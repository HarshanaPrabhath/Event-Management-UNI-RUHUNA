package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.SignatureUploadResponseDto;
import com.management.event.entity.User;
import com.management.event.service.SignatureService;
import com.management.event.service.UploadUrlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/signature")
public class SignatureController {

    private final AuthenticatedUser authenticatedUser;
    private final SignatureService signatureService;
    private final UploadUrlMapper uploadUrlMapper;

    @GetMapping("/me")
    public ResponseEntity<SignatureUploadResponseDto> getMySignature() {
        User user = authenticatedUser.getAuthenticatedUser();
        return ResponseEntity.ok(new SignatureUploadResponseDto(uploadUrlMapper.toPublicUrl(user.getSignatureImagePath())));
    }

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignatureUploadResponseDto> uploadMySignature(
            @RequestParam("signature") MultipartFile signature
    ) {
        User user = authenticatedUser.getAuthenticatedUser();
        String path = signatureService.uploadSignature(user, signature);
        return ResponseEntity.ok(new SignatureUploadResponseDto(uploadUrlMapper.toPublicUrl(path)));
    }
}
