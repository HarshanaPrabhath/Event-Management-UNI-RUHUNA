package com.management.event.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowStepResponseDto {
    private Long id;
    private Integer approverId;
    private String approverName;
    private String approverRegNumber;
    private Integer stepOrder;
    private String status;
    private String remarks;
}
