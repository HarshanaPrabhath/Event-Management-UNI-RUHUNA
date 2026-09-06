package com.management.event.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceUpsertRequestDto {
    private String placeName;
    private String department;
    private Integer capacity;
    private String responsiblePersonRegNumber;
    // Full replacement list each update - simplest way to keep this in sync (see AdminPlaceService).
    private List<PlaceResourceUpsertRequestDto> resources;
}
