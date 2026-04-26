package com.management.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class WorkflowLetterResponseDto {
    private Long letterId;
    private String title;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String eventPlace;
    private String description;
    private String globalStatus;
    private String pdfPath;
    private String requesterName;
    private String requesterRegNumber;
    private WorkflowStepResponseDto currentStep;
    private WorkflowStepResponseDto myStep;
    private List<WorkflowStepResponseDto> steps;
}
