package com.management.event.dto.club;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubSecretaryResponseDto {
    private String regNumber;
    private String userName;
    private String email;

    private Long clubId;
    private String clubName;

    private Instant assignedAt;
}

