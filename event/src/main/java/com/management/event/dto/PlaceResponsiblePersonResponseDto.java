package com.management.event.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceResponsiblePersonResponseDto {

    private Long placeId;
    private String placeName;
    private String responsiblePersonRegNumber;
    private String responsiblePersonName;
    private String message;
}
