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
    private String membersJson;
    private String secretaryRegNumber;
    private String secretaryName;
    private String seniorTreasurerRegNumber;
    private String seniorTreasurerName;
}
