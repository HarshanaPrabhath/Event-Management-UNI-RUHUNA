package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralResourceResponseDto {
    private Long id;
    private String name;
    private Integer quantity;
    private String responsiblePersonRegNumber;
    private String responsiblePersonName;
}
