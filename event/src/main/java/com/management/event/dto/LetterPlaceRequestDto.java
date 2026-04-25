package com.management.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class LetterPlaceRequestDto {

    @NotBlank
    private String eventName;

    private LocalDate eventDate;

    private LocalTime eventTime;

    private String eventPlace;

    private String description;

    private MultipartFile letterPdf;

    @Valid
    private List<ApproverDto> approvers;
}