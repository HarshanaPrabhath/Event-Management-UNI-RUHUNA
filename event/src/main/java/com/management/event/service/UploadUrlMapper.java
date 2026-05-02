package com.management.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

/**
 * Keeps filesystem storage paths (used by the backend) separate from public URLs
 * (used by the frontend). All public URLs are served under /uploads/** by WebMvcConfigure.
 */
@Component
@RequiredArgsConstructor
public class UploadUrlMapper {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String toPublicUrl(String storedPath) {
        if (!StringUtils.hasText(storedPath)) return null;
        try {
            Path base = Path.of(uploadDir);
            Path p = Path.of(storedPath);

            String rel;
            if (base.isAbsolute() && p.isAbsolute() && p.startsWith(base)) {
                rel = base.relativize(p).toString();
            } else if (!p.isAbsolute()) {
                // Relative paths are treated as relative within the uploads base.
                rel = storedPath;
            } else {
                Path fileName = p.getFileName();
                rel = fileName == null ? storedPath : fileName.toString();
            }

            rel = rel.replace('\\', '/');
            while (rel.startsWith("/")) rel = rel.substring(1);
            // Some legacy rows may already include "uploads/" prefix.
            if (rel.startsWith("uploads/")) rel = rel.substring("uploads/".length());
            return "/uploads/" + rel;
        } catch (Exception ignored) {
            String leaf = storedPath.replace('\\', '/');
            int idx = leaf.lastIndexOf('/');
            String name = (idx >= 0) ? leaf.substring(idx + 1) : leaf;
            return "/uploads/" + name;
        }
    }

    public String toPublicUrlPreferSigned(String signedPath, String originalPath) {
        return toPublicUrl(firstNonBlank(signedPath, originalPath));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
