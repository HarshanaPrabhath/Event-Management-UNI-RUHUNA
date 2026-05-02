package com.management.event.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SignLetterRequestDto {

    // 0-based page index.
    @Min(0)
    private int pageIndex = 0;

    // Coordinates in PDF points. If origin = TOP_LEFT, y is measured from top of the page.
    private Float x;
    private Float y;
    private Float width;
    private Float height;

    // Normalized coordinates (0..1) relative to page size.
    // If provided, these will be used when x/y/width/height are null.
    private Float nx;
    private Float ny;
    private Float nw;
    private Float nh;

    // "BOTTOM_LEFT" (PDF) or "TOP_LEFT" (browser).
    private String origin = "TOP_LEFT";
}

