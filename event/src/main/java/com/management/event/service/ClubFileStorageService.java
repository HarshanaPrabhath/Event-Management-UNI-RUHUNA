package com.management.event.service;

import com.management.event.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
public class ClubFileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Stores a club background image as a PNG under uploads/clubs/{clubId}/
     * Returns the stored filesystem path string.
     */
    public String storeClubBgImage(Long clubId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("bgImage is required");
        }

        BufferedImage img;
        try (InputStream in = image.getInputStream()) {
            img = ImageIO.read(in);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read image");
        }
        if (img == null) {
            throw new BadRequestException("Unsupported image type. Upload a PNG/JPG image");
        }

        Path base = Path.of(uploadDir).toAbsolutePath().normalize();
        Path dir = base.resolve(Path.of("clubs", String.valueOf(clubId))).normalize();
        if (!dir.startsWith(base)) {
            throw new BadRequestException("Invalid upload path");
        }

        String filename = "bg-" + UUID.randomUUID() + ".png";
        Path target = dir.resolve(filename);
        try {
            Files.createDirectories(dir);
            ImageIO.write(img, "png", target.toFile());
        } catch (IOException e) {
            throw new BadRequestException("Failed to store image");
        }
        return target.toString().replace('\\', '/');
    }
}

