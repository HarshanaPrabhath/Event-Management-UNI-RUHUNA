package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequestResponseDto {
    private String resourceName;
    private String responsiblePersonName;
    private Integer quantityRequested;
    private Integer quantityAvailable;
}
