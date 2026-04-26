package com.management.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LetterRejectRequestDto {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
