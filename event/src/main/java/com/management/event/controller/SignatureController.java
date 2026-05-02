package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.SignatureUploadResponseDto;
import com.management.event.entity.User;
import com.management.event.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/signature")
public class SignatureController {

    private final AuthenticatedUser authenticatedUser;
    private final SignatureService signatureService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private String toPublicUploadUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        try {
            Path base = Path.of(uploadDir);
            Path p = Path.of(storedPath);
            String rel;
            if (base.isAbsolute() && p.isAbsolute() && p.startsWith(base)) {
                rel = base.relativize(p).toString();
            } else {
                Path fileName = p.getFileName();
                rel = fileName == null ? storedPath : fileName.toString();
            }
            rel = rel.replace('\\', '/');
            while (rel.startsWith("/")) rel = rel.substring(1);
            return "/uploads/" + rel;
        } catch (Exception ignored) {
            String leaf = storedPath.replace('\\', '/');
            int idx = leaf.lastIndexOf('/');
            String name = (idx >= 0) ? leaf.substring(idx + 1) : leaf;
            return "/uploads/" + name;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<SignatureUploadResponseDto> getMySignature() {
        User user = authenticatedUser.getAuthenticatedUser();
        return ResponseEntity.ok(new SignatureUploadResponseDto(toPublicUploadUrl(user.getSignatureImagePath())));
    }

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignatureUploadResponseDto> uploadMySignature(
            @RequestParam("signature") MultipartFile signature
    ) {
        User user = authenticatedUser.getAuthenticatedUser();
        String path = signatureService.uploadSignature(user, signature);
        return ResponseEntity.ok(new SignatureUploadResponseDto(toPublicUploadUrl(path)));
    }
}
