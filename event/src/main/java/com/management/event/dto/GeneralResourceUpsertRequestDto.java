package com.management.event.dto;

import lombok.Data;

@Data
public class GeneralResourceUpsertRequestDto {
    private String name;
    private Integer quantity;
    private String responsiblePersonRegNumber;
}
