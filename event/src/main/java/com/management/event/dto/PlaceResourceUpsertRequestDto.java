package com.management.event.dto;

import lombok.Data;

@Data
public class PlaceResourceUpsertRequestDto {
    private String name;
    private Integer quantity;
}
