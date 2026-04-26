package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.WorkflowActionRequestDto;
import com.management.event.dto.WorkflowLetterResponseDto;
import com.management.event.dto.WorkflowStepResponseDto;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.StepStatus;
import com.management.event.entity.User;
import com.management.event.entity.WorkflowStep;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.WorkflowStepRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowStepRepository workflowStepRepository;
    private final LetterRepository letterRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional(readOnly = true)
    public List<WorkflowLetterResponseDto> getMyWorkflows() {
        return getMyCurrentStepLetters();
    }

    @Transactional(readOnly = true)
    public List<WorkflowLetterResponseDto> getMyCurrentStepLetters() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> currentSteps = workflowStepRepository
                .findByUserRegNumberAndStatusOrderByLetterIdDesc(currentUser.getRegNumber(), StepStatus.CURRENT);

        return currentSteps
                .stream()
                .map(step -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(step.getLetter().getId());
                    return buildWorkflowResponse(step.getLetter(), steps, currentUser.getRegNumber());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowLetterResponseDto getWorkflowByLetterId(Long letterId) {
        User currentUser = authenticatedUser.getAuthenticatedUser();
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter", "id", letterId));
        List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);

        if (steps.isEmpty()) {
            throw new ResourceNotFoundException("Workflow", "letterId", letterId);
        }

        return buildWorkflowResponse(letter, steps, currentUser.getRegNumber());
    }

    @Transactional
    public WorkflowLetterResponseDto actOnWorkflow(Long letterId, @Valid WorkflowActionRequestDto request) {
        User currentUser = authenticatedUser.getAuthenticatedUser();
        List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);

        if (steps.isEmpty()) {
            throw new ResourceNotFoundException("Workflow", "letterId", letterId);
        }

        WorkflowStep currentStep = steps.stream()
                .filter(step -> step.getStatus() == StepStatus.CURRENT)
                .findFirst()
                .orElseThrow(() -> new ApiException("No current workflow step found"));

        if (!currentStep.getUser().getRegNumber().equals(currentUser.getRegNumber())) {
            throw new ApiException("You can only act on your current workflow step");
        }

        String action = request.getAction().trim().toLowerCase();
        switch (action) {
            case "approve" -> approveCurrentStep(steps, currentStep);
            case "reject" -> rejectCurrentStep(currentStep);
            default -> throw new ApiException("Invalid workflow action. Use approve or reject");
        }

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            currentStep.setRemarks(request.getRemarks().trim());
        }

        workflowStepRepository.saveAll(steps);
        letterRepository.save(currentStep.getLetter());

        return buildWorkflowResponse(currentStep.getLetter(), steps, currentUser.getRegNumber());
    }

    private void approveCurrentStep(List<WorkflowStep> steps, WorkflowStep currentStep) {
        currentStep.setStatus(StepStatus.APPROVED);

        WorkflowStep nextStep = steps.stream()
                .filter(step -> step.getStepOrder().equals(currentStep.getStepOrder() + 1))
                .findFirst()
                .orElse(null);

        if (nextStep == null) {
            currentStep.getLetter().setGlobalStatus(LetterStatus.APPROVED);
            return;
        }

        nextStep.setStatus(StepStatus.CURRENT);
        currentStep.getLetter().setGlobalStatus(LetterStatus.PENDING);
    }

    private void rejectCurrentStep(WorkflowStep currentStep) {
        currentStep.setStatus(StepStatus.REJECTED);
        currentStep.getLetter().setGlobalStatus(LetterStatus.REJECTED);
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
                .pdfPath(letter.getPdfPath())
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
