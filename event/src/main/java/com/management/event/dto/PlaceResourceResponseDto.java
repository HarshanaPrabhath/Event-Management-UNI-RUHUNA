package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceResourceResponseDto {
    private Long id;
    private String name;
    private Integer quantity;

    // Always the place's own responsible person - shown here for convenience.
    private String responsiblePersonRegNumber;
    private String responsiblePersonName;
}
