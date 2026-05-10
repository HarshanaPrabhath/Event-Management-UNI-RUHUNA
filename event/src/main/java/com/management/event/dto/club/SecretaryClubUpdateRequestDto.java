package com.management.event.dto.club;

import lombok.Data;

@Data
public class SecretaryClubUpdateRequestDto {
    private String vision; // nullable
    private String mission; // nullable
    private String description;
    private String executiveBoardJson; // JSON string
}

