package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverSummaryResponseDto {
    private String name;
    private String regNumber;
    private Integer stepOrder;
    private String status;
}
