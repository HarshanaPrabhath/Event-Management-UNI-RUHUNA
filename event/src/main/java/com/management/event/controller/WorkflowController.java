package com.management.event.controller;

import com.management.event.dto.WorkflowActionRequestDto;
import com.management.event.dto.WorkflowLetterResponseDto;
import com.management.event.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PatchMapping("/letters/{letterId}/action")
    public ResponseEntity<WorkflowLetterResponseDto> actOnWorkflow(
            @PathVariable Long letterId,
            @Valid @RequestBody WorkflowActionRequestDto request
    ) {
        return ResponseEntity.ok(workflowService.actOnWorkflow(letterId, request));
    }
}
