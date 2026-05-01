package com.management.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class LetterPlaceRequestDto {

    @NotBlank
    private String eventName;

    @NotNull(message = "eventDate is required")
    private LocalDate eventDate;

    @NotNull(message = "eventTime is required")
    private LocalTime eventTime;

    @NotNull(message = "eventEndTime is required")
    private LocalTime eventEndTime;

    private String eventPlace;

    private String placeName;

    private String description;

    private MultipartFile letterPdf;

    @Valid
    private List<ApproverDto> approvers;
}
