package com.management.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowActionRequestDto {
    @NotBlank
    private String action;

    private String remarks;
}
