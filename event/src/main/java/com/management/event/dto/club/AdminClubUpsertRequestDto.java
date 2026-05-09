package com.management.event.dto.club;

import lombok.Data;

@Data
public class AdminClubUpsertRequestDto {
    private String clubName;
    // Admin registers the club by assigning its secretary user account.
    private String secretaryRegNumber;

    // NOTE: remaining details are meant to be managed by the club secretary via /api/me/club.
    // These are kept optional for backward compatibility if you already sent them from frontend;
    // admin create/update may ignore them depending on your workflow.
    private String vision;
    private String mission;
    private String description;
    private String executiveBoardJson;
}
