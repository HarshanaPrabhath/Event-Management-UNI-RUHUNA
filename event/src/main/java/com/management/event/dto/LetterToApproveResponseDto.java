package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterToApproveResponseDto {
    private Long letterId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalTime eventEndTime;
    private String eventPlace;
    private String globalStatus;
    private String pdfPath;
    private String rejectionReason;
    private String approvalNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finalDecisionAt;

    // Club this letter belongs to (new club flow).
    private Long clubId;
    private String clubName;

    // Where a rejected letter is currently parked: "SENIOR_TREASURER" (bounced, awaiting re-forward
    // decision), "SECRETARY" (returned for revision), or null.
    private String returnStage;

    // UI hints for the authenticated user viewing this letter.
    private boolean canReforward;
    private boolean canReturnToSecretary;
    private boolean canResend;
    private boolean canCancel;

    private List<ResourceRequestResponseDto> resourceRequests;

    private SenderSummaryResponseDto sender;
    private List<ApproverSummaryResponseDto> previousApprovers;
    private ApproverSummaryResponseDto currentApprover;
    private List<ApproverSummaryResponseDto> nextApprovers;

    // Convenience: the authenticated user's workflow step for this letter (if any).
    private ApproverSummaryResponseDto myAction;
}
