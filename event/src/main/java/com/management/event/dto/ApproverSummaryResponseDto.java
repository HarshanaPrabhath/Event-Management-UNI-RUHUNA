package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverSummaryResponseDto {
    private String name;
    private String regNumber;
    private Integer stepOrder;
    // Which resend cycle this step belongs to (0 = original submission, 1 = first resend, ...) -
    // lets the UI group/label the complete flow by round.
    private Integer resendRound;
    private String status;
    private String remarks;
    private LocalDateTime assignedAt;
    private LocalDateTime actedAt;
}
