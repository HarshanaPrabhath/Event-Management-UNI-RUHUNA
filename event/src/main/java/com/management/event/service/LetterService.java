package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.ApproverDto;
import com.management.event.dto.ApproverSummaryResponseDto;
import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.dto.LetterApproveRequestDto;
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
import com.management.event.service.CalendarEventService;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.UserRepository;
import com.management.event.repository.WorkflowStepRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
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
    private final CalendarEventService calendarEventService;
    private final ModelMapper modelMapper;
    private final PdfSigningService pdfSigningService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<LetterToApproveResponseDto> getMyLetters() {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        return letterRepository.findByUserRegNumberOrderByIdDesc(currentUser.getRegNumber())
                .stream()
                .map(letter -> {
                    List<WorkflowStep> steps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letter.getId());
                    return buildLetterToApproveResponse(letter, steps, currentUser.getRegNumber());
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
                    return buildLetterToApproveResponse(step.getLetter(), steps, currentUser.getRegNumber());
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
                    return buildLetterToApproveResponse(step.getLetter(), steps, currentUser.getRegNumber());
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
                    return buildLetterToApproveResponse(step.getLetter(), steps, currentUser.getRegNumber());
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
        if (letterPlaceRequestDto.getEventTime() != null
                && letterPlaceRequestDto.getEventEndTime() != null
                && !letterPlaceRequestDto.getEventTime().isBefore(letterPlaceRequestDto.getEventEndTime())) {
            throw new ApiException("eventEndTime must be after eventTime");
        }

        User requester = authenticatedUser.getAuthenticatedUser();


        List<User> approvers = resolveApprovers(letterPlaceRequestDto.getApprovers());

        List<User> finalApprovers = new ArrayList<>();


        if (StringUtils.hasText(letterPlaceRequestDto.getPlaceName())) {
            // Block placing a letter if the slot is already booked (approved or pending booking).
            if (letterPlaceRequestDto.getEventDate() != null
                    && letterPlaceRequestDto.getEventTime() != null
                    && letterPlaceRequestDto.getEventEndTime() != null) {
                calendarEventService.assertSlotAvailableOrThrow(
                        letterPlaceRequestDto.getPlaceName(),
                        letterPlaceRequestDto.getEventDate(),
                        letterPlaceRequestDto.getEventTime(),
                        letterPlaceRequestDto.getEventEndTime(),
                        null
                );
            }

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
        letter.setEventEndTime(letterPlaceRequestDto.getEventEndTime());
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
            if (i == 0) {
                step.setAssignedAt(java.time.LocalDateTime.now());
            }
            // If a place is selected, step 1 is the place responsible person (approve/forward only, no signature).
            // Everyone else must sign+approve.
            boolean isPlaceResponsibleFirstStep = (i == 0) && StringUtils.hasText(savedLetter.getEventPlace());
            step.setRequiresSignature(!isPlaceResponsibleFirstStep);
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

        String rejectionText = firstNonBlank(request.getRejectionReason(), request.getRemarks());
        if (rejectionText != null) {
            rejectionText = rejectionText.trim();
            if (rejectionText.isBlank()) {
                rejectionText = null;
            }
        }

        currentStep.setStatus(StepStatus.REJECTED);
        currentStep.getLetter().setGlobalStatus(LetterStatus.REJECTED);
        currentStep.setRemarks(rejectionText);
        currentStep.getLetter().setRejectionReason(rejectionText);
        currentStep.setActedAt(java.time.LocalDateTime.now());

        workflowStepRepository.save(currentStep);
        letterRepository.save(currentStep.getLetter());
        // If the place had a pending booking, remove it when the letter is rejected.
        calendarEventService.deleteByLetterId(letterId);
    }

    @Transactional
    public void approveLetter(Long letterId, LetterApproveRequestDto request) {
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

        if (currentStep.isRequiresSignature()) {
            throw new ApiException("This approval requires a signature. Use the sign+approve endpoint.");
        }

        Letter letter = currentStep.getLetter();

        String remarks = request != null ? request.getRemarks() : null;
        if (remarks != null) {
            remarks = remarks.trim();
            if (remarks.isBlank()) {
                remarks = null;
            }
        }

        currentStep.setStatus(StepStatus.APPROVED);
        currentStep.setRemarks(remarks);
        currentStep.setActedAt(java.time.LocalDateTime.now());

        WorkflowStep nextStep = steps.stream()
                .filter(step -> step.getStepOrder().equals(currentStep.getStepOrder() + 1))
                .findFirst()
                .orElse(null);

        // If this is the place responsible person's approval (step 1 when a place was selected),
        // create a PENDING_BOOKING calendar reservation and update global status.
        boolean isPlaceResponsibleApproval = currentStep.getStepOrder() == 1 && StringUtils.hasText(letter.getEventPlace());
        if (isPlaceResponsibleApproval) {
            calendarEventService.ensurePendingBookingForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.PENDING_BOOKING);
        }

        if (nextStep == null) {
            // Final approval: mark letter approved and save/upgrade calendar event to APPROVED.
            calendarEventService.markApprovedForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.APPROVED);
            letter.setApprovalNote(remarks);
        } else {
            nextStep.setStatus(StepStatus.CURRENT);
            nextStep.setAssignedAt(java.time.LocalDateTime.now());
        }

        workflowStepRepository.saveAll(steps);
        letterRepository.save(letter);
    }

    private LetterToApproveResponseDto buildLetterToApproveResponse(Letter letter, List<WorkflowStep> steps, String currentUserRegNumber) {
        LetterToApproveResponseDto response = modelMapper.map(letter, LetterToApproveResponseDto.class);
        response.setLetterId(letter.getId());
        response.setGlobalStatus(letter.getGlobalStatus() != null ? letter.getGlobalStatus().name() : null);
        response.setRejectionReason(letter.getRejectionReason());
        response.setApprovalNote(letter.getApprovalNote());
        response.setSender(SenderSummaryResponseDto.builder()
                .name(letter.getUser().getUserName())
                .regNumber(letter.getUser().getRegNumber())
                .build());
        response.setCreatedAt(letter.getCreatedAt());
        response.setUpdatedAt(letter.getUpdatedAt());

        // Convenience fields for UI.
        WorkflowStep rejectedStep = steps.stream().filter(s -> s.getStatus() == StepStatus.REJECTED).findFirst().orElse(null);
        if (rejectedStep != null) {
            response.setFinalDecisionAt(rejectedStep.getActedAt());
        } else if (letter.getGlobalStatus() == LetterStatus.APPROVED) {
            java.time.LocalDateTime maxApprovedAt = steps.stream()
                    .filter(s -> s.getStatus() == StepStatus.APPROVED && s.getActedAt() != null)
                    .map(WorkflowStep::getActedAt)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);
            response.setFinalDecisionAt(maxApprovedAt);
        } else {
            response.setFinalDecisionAt(null);
        }

        WorkflowStep myStep = (currentUserRegNumber == null) ? null : steps.stream()
                .filter(s -> s.getUser() != null && currentUserRegNumber.equals(s.getUser().getRegNumber()))
                .findFirst()
                .orElse(null);
        response.setMyAction(myStep == null ? null : mapApprover(myStep));

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
                .remarks(step.getRemarks())
                .assignedAt(step.getAssignedAt())
                .actedAt(step.getActedAt())
                .build();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
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

        Path uploadDirectory = Path.of(uploadDir);
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

    @Transactional
    public String signLetter(Long letterId, com.management.event.dto.SignLetterRequestDto request) {
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
        if (!currentStep.isRequiresSignature()) {
            throw new ApiException("This step does not require a signature");
        }

        Letter letter = currentStep.getLetter();

        // Stamps and updates the letter fields; persist the result.
        String signedPath = pdfSigningService.stampSignature(letter, currentUser, request);
        letterRepository.save(letter);
        return signedPath;
    }

    @Transactional
    public String signAndApproveCurrentStep(
            Long letterId,
            com.management.event.dto.SignApproveRequestDto request
    ) {
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
        if (!currentStep.isRequiresSignature()) {
            throw new ApiException("This step does not require a signature. Use approve endpoint.");
        }

        // If client didn't send coordinates, try to use predefined step placement.
        com.management.event.dto.SignLetterRequestDto effectiveReq =
                (request == null) ? null : request.getSignature();
        if (effectiveReq == null) effectiveReq = new com.management.event.dto.SignLetterRequestDto();

        boolean hasCoords = effectiveReq.getX() != null && effectiveReq.getY() != null
                && effectiveReq.getWidth() != null && effectiveReq.getHeight() != null;
        boolean hasNorm = effectiveReq.getNx() != null && effectiveReq.getNy() != null
                && effectiveReq.getNw() != null && effectiveReq.getNh() != null;

//        if (!hasCoords && !hasNorm) {
//            if (currentStep.getSignaturePageIndex() == null
//                    || currentStep.getSignatureX() == null
//                    || currentStep.getSignatureY() == null
//                    || currentStep.getSignatureWidth() == null
//                    || currentStep.getSignatureHeight() == null) {
//                throw new ApiException("Signature coordinates are required for this step");
//            }
//            com.management.event.dto.SignLetterRequestDto r = new com.management.event.dto.SignLetterRequestDto();
//            r.setPageIndex(currentStep.getSignaturePageIndex());
//            r.setOrigin(currentStep.getSignatureOrigin() == null ? "TOP_LEFT" : currentStep.getSignatureOrigin());
//            r.setX(currentStep.getSignatureX());
//            r.setY(currentStep.getSignatureY());
//            r.setWidth(currentStep.getSignatureWidth());
//            r.setHeight(currentStep.getSignatureHeight());
//            effectiveReq = r;
//        }

        Letter letter = currentStep.getLetter();
        String signedPath = pdfSigningService.stampSignature(letter, currentUser, effectiveReq);
        currentStep.setSignedAt(java.time.LocalDateTime.now());

        String remarks = request != null ? request.getRemarks() : null;
        if (remarks != null) {
            remarks = remarks.trim();
            if (remarks.isBlank()) {
                remarks = null;
            }
        }

        currentStep.setStatus(StepStatus.APPROVED);
        currentStep.setRemarks(remarks);
        currentStep.setActedAt(java.time.LocalDateTime.now());

        WorkflowStep nextStep = steps.stream()
                .filter(step -> step.getStepOrder().equals(currentStep.getStepOrder() + 1))
                .findFirst()
                .orElse(null);

        // Place-responsible step never calls this, but keep behavior consistent if misconfigured.
        boolean isPlaceResponsibleApproval = currentStep.getStepOrder() == 1 && StringUtils.hasText(letter.getEventPlace());
        if (isPlaceResponsibleApproval) {
            calendarEventService.ensurePendingBookingForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.PENDING_BOOKING);
        }

        if (nextStep == null) {
            calendarEventService.markApprovedForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.APPROVED);
            letter.setApprovalNote(remarks);
        } else {
            nextStep.setStatus(StepStatus.CURRENT);
            nextStep.setAssignedAt(java.time.LocalDateTime.now());
        }

        workflowStepRepository.saveAll(steps);
        letterRepository.save(letter);
        return signedPath;
    }
}
