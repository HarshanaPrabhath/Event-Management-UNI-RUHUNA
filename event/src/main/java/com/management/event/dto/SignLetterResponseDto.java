package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignLetterResponseDto {
    private Long letterId;
    private String signedPdfPath;
}

