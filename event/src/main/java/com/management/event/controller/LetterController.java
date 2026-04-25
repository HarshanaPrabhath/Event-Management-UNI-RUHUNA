package com.management.event.controller;

import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.service.LetterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/letter")
public class LetterController {

    private final LetterService letterService;

    @PostMapping(value = "/place", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> placeLetter(@Valid @ModelAttribute LetterPlaceRequestDto letterPlaceRequestDto) {
        letterService.placeLetter(letterPlaceRequestDto);
        return ResponseEntity.ok("Success");
    }
}