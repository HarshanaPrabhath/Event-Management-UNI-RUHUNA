package com.management.event.dto.club;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubSeniorTreasurerResponseDto {
    private String regNumber;
    private String userName;
    private String email;

    private Long clubId;
    private String clubName;
}

