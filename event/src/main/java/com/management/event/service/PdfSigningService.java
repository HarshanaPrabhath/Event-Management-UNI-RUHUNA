package com.management.event.service;

import com.management.event.dto.SignLetterRequestDto;
import com.management.event.entity.Letter;
import com.management.event.entity.User;
import com.management.event.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfSigningService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public String stampSignature(Letter letter, User signer, SignLetterRequestDto req) {

        // 🔹 Get source PDF
        String sourcePathStr = (letter.getSignedPdfPath() != null && !letter.getSignedPdfPath().isBlank())
                ? letter.getSignedPdfPath()
                : letter.getPdfPath();

        if (sourcePathStr == null || sourcePathStr.isBlank()) {
            throw new ApiException("Letter PDF path is missing");
        }

        if (signer.getSignatureImagePath() == null || signer.getSignatureImagePath().isBlank()) {
            throw new ApiException("You don't have a signature on file. Upload your signature first.");
        }

        Path pdfPath = Path.of(sourcePathStr);
        if (!Files.exists(pdfPath)) {
            throw new ApiException("Letter PDF not found: " + sourcePathStr);
        }

        Path signaturePath = Path.of(signer.getSignatureImagePath());
        if (!Files.exists(signaturePath)) {
            throw new ApiException("Signature image not found. Upload again.");
        }

        // 🔹 Load signature image
        BufferedImage signatureImg;
        try {
            signatureImg = ImageIO.read(signaturePath.toFile());
        } catch (IOException e) {
            throw new ApiException("Failed to read signature image");
        }

        if (signatureImg == null) {
            throw new ApiException("Signature image is invalid");
        }

        // 🔹 TEMP FILE
        Path tempPath = pdfPath.resolveSibling("temp-" + UUID.randomUUID() + ".pdf");

        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {

            if (req.getPageIndex() < 0 || req.getPageIndex() >= doc.getNumberOfPages()) {
                throw new ApiException("Invalid pageIndex");
            }

            PDPage page = doc.getPage(req.getPageIndex());
            PDRectangle media = page.getMediaBox();
            float pageW = media.getWidth();
            float pageH = media.getHeight();

            Placement placement = resolvePlacement(req, pageW, pageH);

            PDImageXObject imgX = LosslessFactory.createFromImage(doc, signatureImg);

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {

                cs.drawImage(imgX, placement.x, placement.y, placement.w, placement.h);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String signedByText = "Signed by: " + signer.getRegNumber();
                String dateText = "Date: " + LocalDateTime.now().format(formatter);

                float textX = placement.x + 6;
                float textY = placement.y - 12;

                // ── FIXED: use Standard14Fonts for both text blocks ──
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                cs.beginText();
                cs.setFont(font, 7);
                cs.newLineAtOffset(textX, textY);
                cs.showText(signedByText);
                cs.endText();

                cs.beginText();
                cs.setFont(font, 7);
                cs.newLineAtOffset(textX, textY - 10);
                cs.showText(dateText);
                cs.endText();
            }

            // 🔹 Save to TEMP
            doc.save(tempPath.toFile());

        } catch (IOException e) {
            throw new ApiException("Failed to stamp signature onto PDF");
        }

        // 🔹 Replace original
        try {
            Files.move(tempPath, pdfPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException("Failed to replace original PDF");
        }

        // 🔹 Update DB
        String saved = pdfPath.toString().replace('\\', '/');
        letter.setSignedPdfPath(saved);
        letter.setSignedByRegNumber(signer.getRegNumber());
        letter.setSignedAt(LocalDateTime.now());

        return saved;
    }

    // 🔹 Placement logic
    private static Placement resolvePlacement(SignLetterRequestDto req, float pageW, float pageH) {

        Float x = req.getX();
        Float y = req.getY();
        Float w = req.getWidth();
        Float h = req.getHeight();

        if ((x == null || y == null || w == null || h == null)
                && (req.getNx() != null && req.getNy() != null
                && req.getNw() != null && req.getNh() != null)) {

            x = req.getNx() * pageW;
            y = req.getNy() * pageH;
            w = req.getNw() * pageW;
            h = req.getNh() * pageH;
        }

        if (x == null || y == null || w == null || h == null) {
            throw new ApiException("Provide either x/y/width/height or nx/ny/nw/nh");
        }

        if (w <= 0 || h <= 0) {
            throw new ApiException("width/height must be > 0");
        }

        String origin = req.getOrigin() == null
                ? "TOP_LEFT"
                : req.getOrigin().trim().toUpperCase(Locale.ROOT);

        float outX = x;
        float outY = y;

        if ("TOP_LEFT".equals(origin)) {
            outY = pageH - y - h;
        } else if (!"BOTTOM_LEFT".equals(origin)) {
            throw new ApiException("origin must be TOP_LEFT or BOTTOM_LEFT");
        }

        outX = Math.max(0, Math.min(outX, pageW - w));
        outY = Math.max(0, Math.min(outY, pageH - h));

        return new Placement(outX, outY, w, h);
    }

    private record Placement(float x, float y, float w, float h) {}
}