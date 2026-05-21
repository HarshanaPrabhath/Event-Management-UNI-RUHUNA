package com.management.event.dto.club;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubResponseDto {
    private Long id;
    private String clubName;
    private String bgImageUrl;
    private String vision;
    private String mission;
    private String description;
    private String executiveBoardJson;
    private String secretaryRegNumber; // resolved from club_secretary if assigned
    private String seniorTreasurerRegNumber; // resolved from club_senior_treasurer if assigned
}
