package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.ApproverDto;
import com.management.event.dto.ApproverSummaryResponseDto;
import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.dto.LetterRejectRequestDto;
import com.management.event.dto.LetterToApproveResponseDto;
import com.management.event.dto.SenderSummaryResponseDto;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.StepStatus;
import com.management.event.entity.User;
import com.management.event.entity.WorkflowStep;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.entity.Place;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.UserRepository;
import com.management.event.repository.WorkflowStepRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LetterService {

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUser authenticatedUser;
    private final WorkflowStepRepository workflowStepRepository;
    private final PlaceRepository placeRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<LetterToApproveResponseDto> getMyLetters() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        return letterRepository.findByUserRegNumberOrderByIdDesc(currentUser.getRegNumber())
                .stream()
                .map(letter -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letter.getId());
                    return buildLetterToApproveResponse(letter, steps);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LetterToApproveResponseDto> getApprovedByMe() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> approvedSteps = workflowStepRepository
                .findByUserRegNumberAndStatusOrderByLetterIdDesc(currentUser.getRegNumber(), StepStatus.APPROVED);

        if (approvedSteps.isEmpty()) {
            throw new ApiException("You have not approved any letters yet");
        }

        return approvedSteps.stream()
                .map(step -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(step.getLetter().getId());
                    return buildLetterToApproveResponse(step.getLetter(), steps);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LetterToApproveResponseDto> getRejectedByMe() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> rejectedSteps = workflowStepRepository
                .findByUserRegNumberAndStatusOrderByLetterIdDesc(currentUser.getRegNumber(), StepStatus.REJECTED);

        if (rejectedSteps.isEmpty()) {
            throw new ApiException("You have not rejected any letters yet");
        }

        return rejectedSteps.stream()
                .map(step -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(step.getLetter().getId());
                    return buildLetterToApproveResponse(step.getLetter(), steps);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LetterToApproveResponseDto> getLettersToApprove() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> pendingSteps = workflowStepRepository
                .findByUserRegNumberAndStatusOrderByLetterIdDesc(currentUser.getRegNumber(), StepStatus.CURRENT);

        if (pendingSteps.isEmpty()) {
            throw new ApiException("You have no letters pending your approval");
        }

        return pendingSteps.stream()
                .map(step -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(step.getLetter().getId());
                    return buildLetterToApproveResponse(step.getLetter(), steps);
                })
                .toList();
    }

    @Transactional
    public void placeLetter(@Valid LetterPlaceRequestDto letterPlaceRequestDto) {
        if (letterPlaceRequestDto.getLetterPdf() == null || letterPlaceRequestDto.getLetterPdf().isEmpty()) {
            throw new ApiException("Letter PDF is required");
        }

        if (letterPlaceRequestDto.getApprovers() == null || letterPlaceRequestDto.getApprovers().isEmpty()) {
            throw new ApiException("At least one approver is required");
        }

        User requester = authenticatedUser.getAuthenticatedUser();


        List<User> approvers = resolveApprovers(letterPlaceRequestDto.getApprovers());

        List<User> finalApprovers = new ArrayList<>();


        if (StringUtils.hasText(letterPlaceRequestDto.getPlaceName())) {

            Place place = placeRepository.findByPlaceName(letterPlaceRequestDto.getPlaceName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Place", "name", letterPlaceRequestDto.getPlaceName()));

            if (place.getResponsiblePerson() == null) {
                throw new ApiException("Selected place has no responsible person");
            }

            User placeResponsiblePerson = place.getResponsiblePerson();

            // Add place responsible person FIRST
            finalApprovers.add(placeResponsiblePerson);

            // Add manual approvers (avoid duplicate)
            for (User approver : approvers) {
                if (!approver.getRegNumber().equals(placeResponsiblePerson.getRegNumber())) {
                    finalApprovers.add(approver);
                }
            }

        } else {
            finalApprovers = approvers;
        }


        if (finalApprovers.isEmpty()) {
            throw new ApiException("At least one approver is required");
        }

        Letter letter = new Letter();
        letter.setUser(requester);
        letter.setTitle(letterPlaceRequestDto.getEventName());
        letter.setEventDate(letterPlaceRequestDto.getEventDate());
        letter.setEventTime(letterPlaceRequestDto.getEventTime());
        letter.setEventPlace(letterPlaceRequestDto.getPlaceName());
        letter.setDescription(letterPlaceRequestDto.getDescription());
        letter.setPdfPath(storePdf(letterPlaceRequestDto.getLetterPdf()));
        letter.setGlobalStatus(LetterStatus.PENDING);

        Letter savedLetter = letterRepository.save(letter);

        List<WorkflowStep> steps = new ArrayList<>();

        for (int i = 0; i < finalApprovers.size(); i++) {
            WorkflowStep step = new WorkflowStep();
            step.setLetter(savedLetter);
            step.setUser(finalApprovers.get(i));
            step.setStepOrder(i + 1);
            step.setStatus(i == 0 ? StepStatus.CURRENT : StepStatus.WAITING);
            steps.add(step);
        }

        workflowStepRepository.saveAll(steps);
    }

    @Transactional
    public void rejectLetter(Long letterId, @Valid LetterRejectRequestDto request) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);
        if (steps.isEmpty()) {
            throw new ResourceNotFoundException("Letter", "id", letterId);
        }

        WorkflowStep currentStep = steps.stream()
                .filter(step -> step.getStatus() == StepStatus.CURRENT)
                .findFirst()
                .orElseThrow(() -> new ApiException("No pending workflow step found for this letter"));

        if (!currentStep.getUser().getRegNumber().equals(currentUser.getRegNumber())) {
            throw new ApiException("You are not the current approver for this letter");
        }

        currentStep.setStatus(StepStatus.REJECTED);
        currentStep.getLetter().setGlobalStatus(LetterStatus.REJECTED);
        currentStep.getLetter().setRejectionReason(request.getRejectionReason().trim());

        workflowStepRepository.save(currentStep);
        letterRepository.save(currentStep.getLetter());
    }

    @Transactional
    public void approveLetter(Long letterId) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);
        if (steps.isEmpty()) {
            throw new ResourceNotFoundException("Letter", "id", letterId);
        }

        WorkflowStep currentStep = steps.stream()
                .filter(step -> step.getStatus() == StepStatus.CURRENT)
                .findFirst()
                .orElseThrow(() -> new ApiException("No pending workflow step found for this letter"));

        if (!currentStep.getUser().getRegNumber().equals(currentUser.getRegNumber())) {
            throw new ApiException("You are not the current approver for this letter");
        }

        currentStep.setStatus(StepStatus.APPROVED);

        WorkflowStep nextStep = steps.stream()
                .filter(step -> step.getStepOrder().equals(currentStep.getStepOrder() + 1))
                .findFirst()
                .orElse(null);

        if (nextStep == null) {
            currentStep.getLetter().setGlobalStatus(LetterStatus.APPROVED);
        } else {
            nextStep.setStatus(StepStatus.CURRENT);
        }

        workflowStepRepository.saveAll(steps);
        letterRepository.save(currentStep.getLetter());
    }

    private LetterToApproveResponseDto buildLetterToApproveResponse(Letter letter, List<WorkflowStep> steps) {
        LetterToApproveResponseDto response = modelMapper.map(letter, LetterToApproveResponseDto.class);
        response.setLetterId(letter.getId());
        response.setGlobalStatus(letter.getGlobalStatus() != null ? letter.getGlobalStatus().name() : null);
        response.setRejectionReason(letter.getRejectionReason());
        response.setSender(SenderSummaryResponseDto.builder()
                .name(letter.getUser().getUserName())
                .regNumber(letter.getUser().getRegNumber())
                .build());

        WorkflowStep currentStep = steps.stream()
                .filter(step -> step.getStatus() == StepStatus.CURRENT)
                .findFirst()
                .orElse(null);

        if (currentStep != null) {
            response.setPreviousApprovers(steps.stream()
                    .filter(step -> step.getStepOrder() < currentStep.getStepOrder())
                    .map(this::mapApprover)
                    .toList());
            response.setCurrentApprover(mapApprover(currentStep));
            response.setNextApprovers(steps.stream()
                    .filter(step -> step.getStepOrder() > currentStep.getStepOrder())
                    .map(this::mapApprover)
                    .toList());
        } else {
            response.setPreviousApprovers(steps.stream().map(this::mapApprover).toList());
            response.setCurrentApprover(null);
            response.setNextApprovers(List.of());
        }

        return response;
    }

    private ApproverSummaryResponseDto mapApprover(WorkflowStep step) {
        return ApproverSummaryResponseDto.builder()
                .name(step.getUser().getUserName())
                .regNumber(step.getUser().getRegNumber())
                .stepOrder(step.getStepOrder())
                .status(step.getStatus().name())
                .build();
    }

    private List<User> resolveApprovers(List<ApproverDto> approverDtos) {
        List<ApproverDto> sortedApprovers = approverDtos.stream()
                .sorted(Comparator.comparingInt(ApproverDto::getOrder))
                .toList();

        Set<Integer> seenOrders = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        List<User> approvers = new ArrayList<>();

        for (int i = 0; i < sortedApprovers.size(); i++) {
            ApproverDto approverDto = sortedApprovers.get(i);
            String approverName = approverDto.getName().trim();

            if (approverDto.getOrder() != i + 1) {
                throw new ApiException("Approver order must start at 1 and be sequential");
            }
            if (!seenOrders.add(approverDto.getOrder())) {
                throw new ApiException("Duplicate approver order: " + approverDto.getOrder());
            }
            if (!seenNames.add(approverName.toLowerCase())) {
                throw new ApiException("Duplicate approver name: " + approverName);
            }

            User approver = userRepository.findByUserName(approverName)
                    .or(() -> userRepository.findByRegNumber(approverName))
                    .orElseThrow(() -> new ResourceNotFoundException("User", "name", approverName));

            approvers.add(approver);
        }

        return approvers;
    }

    private String storePdf(MultipartFile letterPdf) {
        String originalFilename = StringUtils.cleanPath(letterPdf.getOriginalFilename() == null ? "letter.pdf" : letterPdf.getOriginalFilename());
        String extension = ".pdf";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        Path uploadDirectory = Path.of("uploads", "letters");
        Path targetFile = uploadDirectory.resolve(UUID.randomUUID() + extension);

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream inputStream = letterPdf.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ApiException("Failed to store letter PDF");
        }

        return targetFile.toString().replace('\\', '/');
    }
}
