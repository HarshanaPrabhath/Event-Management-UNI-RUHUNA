package com.management.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceRequestItemDto {
    @NotNull
    private Long resourceId;

    @NotNull
    private Integer quantity;
}
