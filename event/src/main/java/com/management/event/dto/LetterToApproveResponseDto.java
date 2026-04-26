package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private String eventPlace;
    private String globalStatus;
    private String pdfPath;
    private String rejectionReason;
    private SenderSummaryResponseDto sender;
    private List<ApproverSummaryResponseDto> previousApprovers;
    private ApproverSummaryResponseDto currentApprover;
    private List<ApproverSummaryResponseDto> nextApprovers;
}
