package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.LetterApproveRequestDto;
import com.management.event.dto.LetterRejectRequestDto;
import com.management.event.dto.WorkflowActionRequestDto;
import com.management.event.dto.WorkflowLetterResponseDto;
import com.management.event.dto.WorkflowStepResponseDto;
import com.management.event.entity.Letter;
import com.management.event.entity.StepStatus;
import com.management.event.entity.User;
import com.management.event.entity.WorkflowStep;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.WorkflowStepRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Thin wrapper over {@link LetterService} so the PATCH /api/workflows endpoint and the
 * /api/letter/* endpoints share one implementation of the approval state machine.
 */
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowStepRepository workflowStepRepository;
    private final AuthenticatedUser authenticatedUser;
    private final LetterService letterService;
    private final UploadUrlMapper uploadUrlMapper;

    @Transactional
    public WorkflowLetterResponseDto actOnWorkflow(Long letterId, @Valid WorkflowActionRequestDto request) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        String action = request.getAction() == null ? "" : request.getAction().trim().toLowerCase();
        switch (action) {
            case "approve" -> {
                LetterApproveRequestDto dto = new LetterApproveRequestDto();
                dto.setRemarks(request.getRemarks());
                letterService.approveLetter(letterId, dto);
            }
            case "reject" -> {
                LetterRejectRequestDto dto = new LetterRejectRequestDto();
                dto.setRemarks(request.getRemarks());
                letterService.rejectLetter(letterId, dto);
            }
            case "return", "return_to_secretary", "return-to-secretary" ->
                    letterService.returnToSecretary(letterId, request.getRemarks());
            default -> throw new ApiException("Invalid workflow action. Use approve, reject or return");
        }

        List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);
        if (steps.isEmpty()) {
            throw new ResourceNotFoundException("Workflow", "letterId", letterId);
        }
        return buildWorkflowResponse(steps.get(0).getLetter(), steps, currentUser.getRegNumber());
    }

    private WorkflowLetterResponseDto buildWorkflowResponse(Letter letter,
                                                            List<WorkflowStep> steps,
                                                            String currentUserRegNumber) {
        List<WorkflowStepResponseDto> stepResponses = steps.stream()
                .map(this::mapStep)
                .toList();

        WorkflowStepResponseDto currentStep = stepResponses.stream()
                .filter(step -> StepStatus.CURRENT.name().equals(step.getStatus()))
                .findFirst()
                .orElse(null);

        WorkflowStepResponseDto myStep = stepResponses.stream()
                .filter(step -> step.getApproverRegNumber().equals(currentUserRegNumber))
                .findFirst()
                .orElse(null);

        return WorkflowLetterResponseDto.builder()
                .letterId(letter.getId())
                .title(letter.getTitle())
                .eventDate(letter.getEventDate())
                .eventTime(letter.getEventTime())
                .eventPlace(letter.getEventPlace())
                .description(letter.getDescription())
                .globalStatus(letter.getGlobalStatus() != null ? letter.getGlobalStatus().name() : null)
                .pdfPath(uploadUrlMapper.toPublicUrlPreferSigned(letter.getSignedPdfPath(), letter.getPdfPath()))
                .requesterName(letter.getUser().getUserName())
                .requesterRegNumber(letter.getUser().getRegNumber())
                .currentStep(currentStep)
                .myStep(myStep)
                .steps(stepResponses)
                .build();
    }

    private WorkflowStepResponseDto mapStep(WorkflowStep step) {
        return WorkflowStepResponseDto.builder()
                .id(step.getId())
                .approverName(step.getUser().getUserName())
                .approverRegNumber(step.getUser().getRegNumber())
                .stepOrder(step.getStepOrder())
                .status(step.getStatus().name())
                .remarks(step.getRemarks())
                .build();
    }
}
