package com.management.event.dto;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class SignApproveRequestDto {
    @Valid
    private SignLetterRequestDto signature;

    // Optional approval remarks.
    private String remarks;
}

