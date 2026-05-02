package com.management.event.service;

import com.management.event.entity.User;
import com.management.event.exception.ApiException;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public String uploadSignature(User user, MultipartFile signatureImage) {
        if (signatureImage == null || signatureImage.isEmpty()) {
            throw new ApiException("Signature image is required");
        }

        BufferedImage img;
        try (InputStream in = signatureImage.getInputStream()) {
            img = ImageIO.read(in);
        } catch (IOException e) {
            throw new ApiException("Failed to read signature image");
        }

        if (img == null) {
            throw new ApiException("Unsupported image type. Upload a PNG/JPG image");
        }

        Path uploadDirectory = Path.of(uploadDir, "signatures");
        String fileName = user.getRegNumber().replaceAll("[^a-zA-Z0-9._-]", "_")
                + "-" + UUID.randomUUID() + ".png";
        Path targetFile = uploadDirectory.resolve(fileName);

        try {
            Files.createDirectories(uploadDirectory);
            ImageIO.write(img, "png", targetFile.toFile());
        } catch (IOException e) {
            throw new ApiException("Failed to store signature image");
        }

        String savedPath = targetFile.toString().replace('\\', '/');
        user.setSignatureImagePath(savedPath);
        userRepository.save(user);
        return savedPath;
    }
}

