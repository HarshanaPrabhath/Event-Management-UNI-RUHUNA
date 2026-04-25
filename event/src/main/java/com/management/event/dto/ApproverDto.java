package com.management.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproverDto {

    @NotNull
    private Integer order;

    @NotBlank
    private String name;
}