package com.management.event.controller;

import com.management.event.dto.WorkflowActionRequestDto;
import com.management.event.dto.WorkflowLetterResponseDto;
import com.management.event.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/my")
    public ResponseEntity<List<WorkflowLetterResponseDto>> getMyWorkflows() {
        return ResponseEntity.ok(workflowService.getMyWorkflows());
    }

    @GetMapping("/letters/{letterId}")
    public ResponseEntity<WorkflowLetterResponseDto> getWorkflowByLetter(@PathVariable Long letterId) {
        return ResponseEntity.ok(workflowService.getWorkflowByLetterId(letterId));
    }

    @PatchMapping("/letters/{letterId}/action")
    public ResponseEntity<WorkflowLetterResponseDto> actOnWorkflow(
            @PathVariable Long letterId,
            @Valid @RequestBody WorkflowActionRequestDto request
    ) {
        return ResponseEntity.ok(workflowService.actOnWorkflow(letterId, request));
    }
}
