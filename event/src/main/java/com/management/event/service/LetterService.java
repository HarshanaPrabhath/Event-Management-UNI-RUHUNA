package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.ApproverDto;
import com.management.event.dto.ApproverSummaryResponseDto;
import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.dto.LetterApproveRequestDto;
import com.management.event.dto.LetterRejectRequestDto;
import com.management.event.dto.LetterToApproveResponseDto;
import com.management.event.dto.ResourceRequestItemDto;
import com.management.event.dto.ResourceRequestResponseDto;
import com.management.event.dto.SenderSummaryResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.Club;
import com.management.event.entity.GeneralResource;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterResourceRequest;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.PlaceResource;
import com.management.event.entity.StepStatus;
import com.management.event.entity.User;
import com.management.event.entity.WorkflowStep;
import com.management.event.exception.ApiException;
import com.management.event.exception.ForbiddenException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.entity.Place;
import com.management.event.repository.ClubRepository;
import com.management.event.repository.GeneralResourceRepository;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.PlaceResourceRepository;
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
import java.time.LocalDateTime;
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
    private final PlaceResourceRepository placeResourceRepository;
    private final GeneralResourceRepository generalResourceRepository;
    private final ClubRepository clubRepository;
    private final CalendarEventService calendarEventService;
    private final ModelMapper modelMapper;
    private final PdfSigningService pdfSigningService;
    private final EmailNotificationService emailNotificationService;
    private final UploadUrlMapper uploadUrlMapper;

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
            return List.of();
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
            return List.of();
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
            return List.of();
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
        User secretary = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(secretary, AppRole.ROLE_SECRETARY)) {
            throw new ForbiddenException("Only a club secretary can place a letter");
        }
        Club club = resolveSecretaryClub(secretary);
        User seniorTreasurer = resolveSeniorTreasurer(club.getId());

        if (letterPlaceRequestDto.getLetterPdf() == null || letterPlaceRequestDto.getLetterPdf().isEmpty()) {
            throw new ApiException("Letter PDF is required");
        }
        boolean hasPlace = StringUtils.hasText(letterPlaceRequestDto.getPlaceName());
        List<ApproverDto> approverDtos = letterPlaceRequestDto.getApprovers() == null
                ? List.of()
                : letterPlaceRequestDto.getApprovers();
        // A venue-only chain (venue's responsible person -> senior treasurer) needs no manual
        // approvers; without a venue at least one manual approver is required.
        if (approverDtos.isEmpty() && !hasPlace) {
            throw new ApiException("At least one approver is required");
        }
        if (letterPlaceRequestDto.getEventTime() != null
                && letterPlaceRequestDto.getEventEndTime() != null
                && !letterPlaceRequestDto.getEventTime().isBefore(letterPlaceRequestDto.getEventEndTime())) {
            throw new ApiException("eventEndTime must be after eventTime");
        }

        List<User> manualApprovers = resolveApprovers(approverDtos);
        ResolvedResources placeResources = resolvePlaceResourceRequests(
                letterPlaceRequestDto.getPlaceName(), letterPlaceRequestDto.getResources()
        );
        ResolvedResources generalResources = resolveGeneralResourceRequests(letterPlaceRequestDto.getGeneralResources());
        ResolvedResources resolvedResources = mergeResolved(placeResources, generalResources);

        if (StringUtils.hasText(letterPlaceRequestDto.getPlaceName())
                && letterPlaceRequestDto.getEventDate() != null
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

        Letter letter = new Letter();
        letter.setUser(secretary);
        letter.setClub(club);
        letter.setTitle(letterPlaceRequestDto.getEventName());
        letter.setEventDate(letterPlaceRequestDto.getEventDate());
        letter.setEventTime(letterPlaceRequestDto.getEventTime());
        letter.setEventEndTime(letterPlaceRequestDto.getEventEndTime());
        letter.setEventPlace(letterPlaceRequestDto.getPlaceName());
        letter.setDescription(letterPlaceRequestDto.getDescription());
        letter.setPdfPath(storePdf(letterPlaceRequestDto.getLetterPdf()));
        letter.setGlobalStatus(LetterStatus.PENDING);
        attachResourceRequests(letter, resolvedResources);

        Letter savedLetter = letterRepository.save(letter);

        List<WorkflowStep> steps = buildAndSaveSteps(
                savedLetter, seniorTreasurer, manualApprovers,
                placeResources.responsiblePersons(), generalResources.responsiblePersons()
        );

        emailNotificationService.notifyApproverAssigned(savedLetter, steps.get(0).getUser());
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

        String rejectionText = trimToNull(firstNonBlank(request.getRejectionReason(), request.getRemarks()));

        Letter letter = currentStep.getLetter();
        currentStep.setStatus(StepStatus.REJECTED);
        currentStep.setRemarks(rejectionText);
        currentStep.setActedAt(LocalDateTime.now());
        letter.setRejectionReason(rejectionText);

        // Legacy letters (created before the club flow) keep the old terminal rejection.
        if (letter.getClub() == null) {
            letter.setGlobalStatus(LetterStatus.REJECTED);
            workflowStepRepository.saveAll(steps);
            letterRepository.save(letter);
            calendarEventService.deleteByLetterId(letterId);
            emailNotificationService.notifyRequesterRejected(letter, currentUser, rejectionText);
            return;
        }

        // The senior treasurer is identified by their club assignment, not by requiresSignature -
        // that flag is now also true for general-equipment approvers, who must sign too.
        WorkflowStep treasurerStep = findTreasurerStep(letter, steps);
        boolean seniorTreasurerRejected = isTreasurerStep(letter, currentStep);

        if (seniorTreasurerRejected || treasurerStep == null) {
            // Senior treasurer rejected -> return straight to the club secretary.
            letter.setGlobalStatus(LetterStatus.RETURNED_TO_SECRETARY);
            workflowStepRepository.saveAll(steps);
            letterRepository.save(letter);
            calendarEventService.deleteByLetterId(letterId);
            emailNotificationService.notifyRequesterRejected(letter, currentUser, rejectionText);
            return;
        }

        // Anyone else (TO ahead of the treasurer, or a downstream approver) rejected -> bounce
        // back to the senior treasurer with the reason.
        treasurerStep.setStatus(StepStatus.CURRENT);
        treasurerStep.setAssignedAt(LocalDateTime.now());
        treasurerStep.setActedAt(null);
        letter.setGlobalStatus(bookingHeld(steps) ? LetterStatus.PENDING_BOOKING : LetterStatus.PENDING);

        workflowStepRepository.saveAll(steps);
        letterRepository.save(letter);

        emailNotificationService.notifySeniorTreasurerBounce(letter, currentUser, rejectionText, treasurerStep.getUser());
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

        String remarks = trimToNull(request != null ? request.getRemarks() : null);
        advanceAfterApproval(steps, currentStep, currentUser, remarks);
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

        String signedPath = pdfSigningService.stampSignature(letter, currentUser, request);
        letterRepository.save(letter);
        return uploadUrlMapper.toPublicUrl(signedPath);
    }

    @Transactional
    public String signAndApproveCurrentStep(
            Long letterId,
            com.management.event.dto.SignApproveRequestDto request,
            MultipartFile signaturePng
    ) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        if (signaturePng == null || signaturePng.isEmpty()) {
            throw new ApiException("signature is required");
        }

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

        com.management.event.dto.SignLetterRequestDto effectiveReq =
                (request == null) ? null : request.getSignature();
        if (effectiveReq == null) effectiveReq = new com.management.event.dto.SignLetterRequestDto();

        Letter letter = currentStep.getLetter();
        String signedPath = pdfSigningService.stampSignature(letter, currentUser, effectiveReq, signaturePng);
        currentStep.setSignedAt(LocalDateTime.now());

        String remarks = trimToNull(request != null ? request.getRemarks() : null);
        advanceAfterApproval(steps, currentStep, currentUser, remarks);
        return uploadUrlMapper.toPublicUrl(signedPath);
    }

    /**
     * Senior treasurer decides to send a bounced letter back to the club secretary instead of
     * re-forwarding it down the chain.
     */
    @Transactional
    public void returnToSecretary(Long letterId, String remarks) {
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

        Letter letter = currentStep.getLetter();
        if (!isTreasurerStep(letter, currentStep)) {
            throw new ApiException("Only the senior treasurer step can return a letter to the secretary");
        }
        boolean bounced = steps.stream()
                .anyMatch(s -> !isTreasurerStep(letter, s) && s.getStatus() == StepStatus.REJECTED);
        if (!bounced) {
            throw new ApiException("This letter is not awaiting a re-forward decision. Use reject to send it back.");
        }

        String reason = trimToNull(remarks);
        currentStep.setStatus(StepStatus.REJECTED);
        currentStep.setRemarks(reason);
        currentStep.setActedAt(LocalDateTime.now());
        letter.setRejectionReason(reason);
        letter.setGlobalStatus(LetterStatus.RETURNED_TO_SECRETARY);

        workflowStepRepository.saveAll(steps);
        letterRepository.save(letter);
        calendarEventService.deleteByLetterId(letterId);

        emailNotificationService.notifyRequesterRejected(letter, currentUser, reason);
    }

    /**
     * Club secretary edits a returned letter and pushes it back into the flow from the start
     * (senior treasurer first). All prior signatures are cleared and re-collected.
     */
    @Transactional
    public void resendLetter(Long letterId, LetterPlaceRequestDto request) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter", "id", letterId));

        if (!letter.getUser().getRegNumber().equals(currentUser.getRegNumber())) {
            throw new ForbiddenException("Only the letter's secretary can resend it");
        }
        if (letter.getGlobalStatus() != LetterStatus.RETURNED_TO_SECRETARY) {
            throw new ApiException("Only a letter that was returned to the secretary can be resent");
        }
        if (letter.getClub() == null) {
            throw new ApiException("This letter is not linked to a club");
        }

        if (request != null) {
            if (StringUtils.hasText(request.getEventName())) letter.setTitle(request.getEventName().trim());
            if (request.getEventDate() != null) letter.setEventDate(request.getEventDate());
            if (request.getEventTime() != null) letter.setEventTime(request.getEventTime());
            if (request.getEventEndTime() != null) letter.setEventEndTime(request.getEventEndTime());
            if (request.getPlaceName() != null) {
                letter.setEventPlace(StringUtils.hasText(request.getPlaceName()) ? request.getPlaceName().trim() : null);
            }
            if (request.getDescription() != null) letter.setDescription(request.getDescription());
            if (request.getLetterPdf() != null && !request.getLetterPdf().isEmpty()) {
                letter.setPdfPath(storePdf(request.getLetterPdf()));
            }
        }

        if (letter.getEventTime() != null && letter.getEventEndTime() != null
                && !letter.getEventTime().isBefore(letter.getEventEndTime())) {
            throw new ApiException("eventEndTime must be after eventTime");
        }

        List<WorkflowStep> oldSteps = workflowStepRepository.findByLetterIdOrderByStepOrderAsc(letterId);

        List<User> manualApprovers;
        if (request != null && request.getApprovers() != null && !request.getApprovers().isEmpty()) {
            manualApprovers = resolveApprovers(request.getApprovers());
        } else {
            manualApprovers = oldSteps.stream()
                    .filter(s -> !s.isRequiresSignature() && !s.isCreatesBooking() && !s.isResourceApprover())
                    .sorted(Comparator.comparingInt(WorkflowStep::getStepOrder))
                    .map(WorkflowStep::getUser)
                    .toList();
        }
        // Venue-only chains (venue's responsible person -> senior treasurer) carry no manual approvers.
        if (manualApprovers.isEmpty() && !StringUtils.hasText(letter.getEventPlace())) {
            throw new ApiException("At least one approver is required");
        }

        List<User> placeResourceResponsiblePersons;
        List<User> generalResourceResponsiblePersons;
        if (request != null && (request.getResources() != null || request.getGeneralResources() != null)) {
            ResolvedResources placeResources = resolvePlaceResourceRequests(letter.getEventPlace(), request.getResources());
            ResolvedResources generalResources = resolveGeneralResourceRequests(request.getGeneralResources());
            attachResourceRequests(letter, mergeResolved(placeResources, generalResources));
            placeResourceResponsiblePersons = placeResources.responsiblePersons();
            generalResourceResponsiblePersons = generalResources.responsiblePersons();
        } else {
            placeResourceResponsiblePersons = placeResponsiblePersonsFromExistingRequests(letter);
            generalResourceResponsiblePersons = generalResponsiblePersonsFromExistingRequests(letter);
        }

        workflowStepRepository.deleteAll(oldSteps);
        workflowStepRepository.flush();

        User seniorTreasurer = resolveSeniorTreasurer(letter.getClub().getId());

        if (StringUtils.hasText(letter.getEventPlace())
                && letter.getEventDate() != null
                && letter.getEventTime() != null
                && letter.getEventEndTime() != null) {
            calendarEventService.assertSlotAvailableOrThrow(
                    letter.getEventPlace(), letter.getEventDate(), letter.getEventTime(), letter.getEventEndTime(), letterId
            );
        }

        letter.setSignedPdfPath(null);
        letter.setSignedByRegNumber(null);
        letter.setSignedAt(null);
        letter.setRejectionReason(null);
        letter.setApprovalNote(null);
        letter.setGlobalStatus(LetterStatus.PENDING);

        List<WorkflowStep> steps = buildAndSaveSteps(
                letter, seniorTreasurer, manualApprovers,
                placeResourceResponsiblePersons, generalResourceResponsiblePersons
        );
        letterRepository.save(letter);

        emailNotificationService.notifyApproverAssigned(letter, steps.get(0).getUser());
    }

    /**
     * Permanently close a letter. Allowed for the club secretary who owns it or the club's senior treasurer.
     */
    @Transactional
    public void cancelLetter(Long letterId, String remarks) {
        User currentUser = authenticatedUser.getAuthenticatedUser();

        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter", "id", letterId));

        boolean isOwner = letter.getUser().getRegNumber().equals(currentUser.getRegNumber());
        boolean isSeniorTreasurer = letter.getClub() != null
                && currentUser.getRegNumber().equals(letter.getClub().getSeniorTreasurerRegNumber());
        if (!isOwner && !isSeniorTreasurer) {
            throw new ForbiddenException("Only the club secretary or senior treasurer can cancel this letter");
        }
        if (letter.getGlobalStatus() == LetterStatus.APPROVED) {
            throw new ApiException("An approved letter cannot be cancelled");
        }
        if (letter.getGlobalStatus() == LetterStatus.CANCELLED) {
            return;
        }

        letter.setGlobalStatus(LetterStatus.CANCELLED);
        letter.setRejectionReason(trimToNull(remarks));
        letterRepository.save(letter);
        calendarEventService.deleteByLetterId(letterId);

        User notify = isOwner ? null : letter.getUser();
        if (notify != null) {
            emailNotificationService.notifyRequesterRejected(letter, currentUser, trimToNull(remarks));
        }
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private void advanceAfterApproval(List<WorkflowStep> steps, WorkflowStep currentStep, User actor, String remarks) {
        Letter letter = currentStep.getLetter();

        currentStep.setStatus(StepStatus.APPROVED);
        currentStep.setRemarks(remarks);
        currentStep.setActedAt(LocalDateTime.now());

        boolean bouncedReforward = isTreasurerStep(letter, currentStep)
                && steps.stream().anyMatch(s -> !isTreasurerStep(letter, s) && s.getStatus() == StepStatus.REJECTED);

        WorkflowStep nextStep;
        if (bouncedReforward) {
            nextStep = steps.stream()
                    .filter(s -> !isTreasurerStep(letter, s) && s.getStatus() == StepStatus.REJECTED)
                    .min(Comparator.comparingInt(WorkflowStep::getStepOrder))
                    .orElse(null);
            if (nextStep != null) {
                nextStep.setRemarks(null);
                nextStep.setActedAt(null);
                nextStep.setSignedAt(null);
            }
            // The bounce has been resolved by re-forwarding; clear the stale rejection reason.
            letter.setRejectionReason(null);
        } else {
            nextStep = steps.stream()
                    .filter(s -> s.getStepOrder().equals(currentStep.getStepOrder() + 1))
                    .findFirst()
                    .orElse(null);
        }

        if (currentStep.isCreatesBooking()) {
            calendarEventService.ensurePendingBookingForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.PENDING_BOOKING);
        }

        if (nextStep == null) {
            calendarEventService.markApprovedForLetterOrThrow(letter);
            letter.setGlobalStatus(LetterStatus.APPROVED);
            letter.setApprovalNote(remarks);
        } else {
            nextStep.setStatus(StepStatus.CURRENT);
            nextStep.setAssignedAt(LocalDateTime.now());
            if (bouncedReforward && !currentStep.isCreatesBooking()) {
                letter.setGlobalStatus(bookingHeld(steps) ? LetterStatus.PENDING_BOOKING : LetterStatus.PENDING);
            }
        }

        workflowStepRepository.saveAll(steps);
        letterRepository.save(letter);

        if (nextStep == null) {
            emailNotificationService.notifyRequesterApproved(letter, actor);
        } else {
            emailNotificationService.notifyApproverAssigned(letter, nextStep.getUser());
        }
    }

    /**
     * Builds the workflow: venue's responsible person, then the responsible TO for each requested
     * place-bound resource, then the responsible TO for each requested standalone equipment item,
     * then the senior treasurer, then the manually selected approvers.
     */
    private List<WorkflowStep> buildAndSaveSteps(
            Letter letter, User seniorTreasurer, List<User> manualApprovers,
            List<User> placeResourceResponsiblePersons, List<User> generalResourceResponsiblePersons
    ) {
        List<User> ordered = new ArrayList<>();

        String placeResponsibleReg = null;
        if (StringUtils.hasText(letter.getEventPlace())) {
            Place place = placeRepository.findByPlaceName(letter.getEventPlace().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Place", "name", letter.getEventPlace()));
            if (place.getResponsiblePerson() == null) {
                throw new ApiException("Selected place has no responsible person");
            }
            placeResponsibleReg = place.getResponsiblePerson().getRegNumber();
            if (!placeResponsibleReg.equals(seniorTreasurer.getRegNumber())) {
                // The place's responsible person (TO) clears the venue before the senior
                // treasurer signs off, so they lead the pipeline when a venue is requested.
                ordered.add(place.getResponsiblePerson());
            }
        }

        Set<String> resourceApproverRegs = new HashSet<>();
        // Standalone equipment's responsible person must sign for their approval, same as the
        // treasurer - unlike place-bound equipment and the venue TO, which stay plain approvals.
        Set<String> signatureRequiredRegs = new HashSet<>();
        for (User resourceResponsible : placeResourceResponsiblePersons) {
            boolean already = ordered.stream().anyMatch(u -> u.getRegNumber().equals(resourceResponsible.getRegNumber()))
                    || resourceResponsible.getRegNumber().equals(seniorTreasurer.getRegNumber());
            if (!already) {
                ordered.add(resourceResponsible);
                resourceApproverRegs.add(resourceResponsible.getRegNumber());
            }
        }
        for (User resourceResponsible : generalResourceResponsiblePersons) {
            boolean already = ordered.stream().anyMatch(u -> u.getRegNumber().equals(resourceResponsible.getRegNumber()))
                    || resourceResponsible.getRegNumber().equals(seniorTreasurer.getRegNumber());
            if (!already) {
                ordered.add(resourceResponsible);
                resourceApproverRegs.add(resourceResponsible.getRegNumber());
            }
            signatureRequiredRegs.add(resourceResponsible.getRegNumber());
        }

        ordered.add(seniorTreasurer);

        for (User approver : manualApprovers) {
            boolean already = ordered.stream().anyMatch(u -> u.getRegNumber().equals(approver.getRegNumber()));
            if (!already) {
                ordered.add(approver);
            }
        }

        if (ordered.size() < 2) {
            throw new ApiException("At least one approver is required in addition to the senior treasurer");
        }

        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            User user = ordered.get(i);
            boolean isPlaceResponsible = placeResponsibleReg != null && user.getRegNumber().equals(placeResponsibleReg);
            boolean isSeniorTreasurer = user.getRegNumber().equals(seniorTreasurer.getRegNumber());
            boolean isResourceApprover = resourceApproverRegs.contains(user.getRegNumber());
            boolean needsSignature = isSeniorTreasurer || signatureRequiredRegs.contains(user.getRegNumber());

            WorkflowStep step = new WorkflowStep();
            step.setLetter(letter);
            step.setUser(user);
            step.setStepOrder(i + 1);
            step.setStatus(i == 0 ? StepStatus.CURRENT : StepStatus.WAITING);
            if (i == 0) {
                step.setAssignedAt(LocalDateTime.now());
            }
            // The senior treasurer and any standalone-equipment TO sign; the venue TO and
            // place-bound-equipment TOs approve without a signature.
            step.setRequiresSignature(needsSignature);
            step.setCreatesBooking(isPlaceResponsible);
            step.setResourceApprover(isResourceApprover);
            steps.add(step);
        }

        workflowStepRepository.saveAll(steps);
        return steps;
    }

    // Identity-based checks for "is this the senior treasurer's step" - requiresSignature is no
    // longer a reliable proxy for that, since general-equipment approvers also sign now.
    private boolean isTreasurerStep(Letter letter, WorkflowStep step) {
        String treasurerReg = letter.getClub() != null ? letter.getClub().getSeniorTreasurerRegNumber() : null;
        return treasurerReg != null && treasurerReg.equals(step.getUser().getRegNumber());
    }

    private WorkflowStep findTreasurerStep(Letter letter, List<WorkflowStep> steps) {
        String treasurerReg = letter.getClub() != null ? letter.getClub().getSeniorTreasurerRegNumber() : null;
        if (treasurerReg == null) return null;
        return steps.stream()
                .filter(s -> treasurerReg.equals(s.getUser().getRegNumber()))
                .findFirst()
                .orElse(null);
    }

    private Club resolveSecretaryClub(User secretary) {
        return clubRepository.findBySecretaryRegNumber(secretary.getRegNumber())
                .orElseThrow(() -> new ForbiddenException("You are not assigned as secretary of any club"));
    }

    private User resolveSeniorTreasurer(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));
        if (club.getSeniorTreasurerRegNumber() == null) {
            throw new ApiException("Your club has no senior treasurer assigned. Contact the admin.");
        }
        return userRepository.findByRegNumber(club.getSeniorTreasurerRegNumber())
                .orElseThrow(() -> new ApiException("Your club has no senior treasurer assigned. Contact the admin."));
    }

    private static boolean bookingHeld(List<WorkflowStep> steps) {
        return steps.stream().anyMatch(s -> s.isCreatesBooking() && s.getStatus() == StepStatus.APPROVED);
    }

    private LetterToApproveResponseDto buildLetterToApproveResponse(Letter letter, List<WorkflowStep> steps, String currentUserRegNumber) {
        LetterToApproveResponseDto response = modelMapper.map(letter, LetterToApproveResponseDto.class);
        response.setLetterId(letter.getId());
        response.setGlobalStatus(letter.getGlobalStatus() != null ? letter.getGlobalStatus().name() : null);
        response.setRejectionReason(letter.getRejectionReason());
        response.setApprovalNote(letter.getApprovalNote());
        response.setPdfPath(uploadUrlMapper.toPublicUrlPreferSigned(letter.getSignedPdfPath(), letter.getPdfPath()));
        response.setSender(SenderSummaryResponseDto.builder()
                .name(letter.getUser().getUserName())
                .regNumber(letter.getUser().getRegNumber())
                .build());
        response.setCreatedAt(letter.getCreatedAt());
        response.setUpdatedAt(letter.getUpdatedAt());
        response.setResourceRequests(letter.getResourceRequests().stream()
                .map(r -> ResourceRequestResponseDto.builder()
                        .resourceName(r.getResourceName())
                        .responsiblePersonName(r.getResponsiblePersonName())
                        .quantityRequested(r.getQuantityRequested())
                        .quantityAvailable(r.getQuantityAvailable())
                        .build())
                .toList());

        if (letter.getClub() != null) {
            response.setClubId(letter.getClub().getId());
            response.setClubName(letter.getClub().getClubName());
        }

        LetterStatus status = letter.getGlobalStatus();
        WorkflowStep lastRejectedStep = steps.stream()
                .filter(s -> s.getStatus() == StepStatus.REJECTED && s.getActedAt() != null)
                .max(Comparator.comparing(WorkflowStep::getActedAt))
                .orElse(null);
        if (lastRejectedStep != null && (status == LetterStatus.REJECTED
                || status == LetterStatus.RETURNED_TO_SECRETARY
                || status == LetterStatus.CANCELLED)) {
            response.setFinalDecisionAt(lastRejectedStep.getActedAt());
        } else if (letter.getGlobalStatus() == LetterStatus.APPROVED) {
            LocalDateTime maxApprovedAt = steps.stream()
                    .filter(s -> s.getStatus() == StepStatus.APPROVED && s.getActedAt() != null)
                    .map(WorkflowStep::getActedAt)
                    .max(LocalDateTime::compareTo)
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
            // Split by actual status, not raw stepOrder: a re-forward after a bounce can move
            // CURRENT back to a step whose order is lower than an already-APPROVED step (when the
            // place-responsible person precedes the senior treasurer in the chain).
            response.setPreviousApprovers(steps.stream()
                    .filter(step -> step.getStatus() == StepStatus.APPROVED || step.getStatus() == StepStatus.REJECTED)
                    .map(this::mapApprover)
                    .toList());
            response.setCurrentApprover(mapApprover(currentStep));
            response.setNextApprovers(steps.stream()
                    .filter(step -> step.getStatus() == StepStatus.WAITING)
                    .map(this::mapApprover)
                    .toList());
        } else {
            response.setPreviousApprovers(steps.stream().map(this::mapApprover).toList());
            response.setCurrentApprover(null);
            response.setNextApprovers(List.of());
        }

        // Bounced letters sit on the senior treasurer while another step is REJECTED. Identified
        // by club assignment, not requiresSignature - general-equipment approvers sign too now.
        boolean bounced = currentStep != null && isTreasurerStep(letter, currentStep)
                && steps.stream().anyMatch(s -> !isTreasurerStep(letter, s) && s.getStatus() == StepStatus.REJECTED);
        boolean isCurrentUserStep = currentStep != null && currentUserRegNumber != null
                && currentUserRegNumber.equals(currentStep.getUser().getRegNumber());
        boolean returned = letter.getGlobalStatus() == LetterStatus.RETURNED_TO_SECRETARY;
        boolean owner = currentUserRegNumber != null && currentUserRegNumber.equals(letter.getUser().getRegNumber());
        boolean closed = letter.getGlobalStatus() == LetterStatus.APPROVED
                || letter.getGlobalStatus() == LetterStatus.CANCELLED
                || letter.getGlobalStatus() == LetterStatus.REJECTED;

        response.setReturnStage(bounced ? "SENIOR_TREASURER" : (returned ? "SECRETARY" : null));
        response.setCanReforward(bounced && isCurrentUserStep);
        response.setCanReturnToSecretary(bounced && isCurrentUserStep);
        response.setCanResend(returned && owner);
        response.setCanCancel(owner && !closed);

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

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isBlank() ? null : t;
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

    private record ResolvedResources(List<LetterResourceRequest> requests, List<User> responsiblePersons) {
    }

    // Resolves the secretary's picked place-bound equipment into snapshotted request rows plus
    // the distinct set of TOs that must additionally approve the letter. This equipment always
    // routes to the place's own responsible person - it has no responsible person of its own.
    private ResolvedResources resolvePlaceResourceRequests(String placeName, List<ResourceRequestItemDto> items) {
        if (items == null || items.isEmpty()) {
            return new ResolvedResources(List.of(), List.of());
        }
        if (!StringUtils.hasText(placeName)) {
            throw new ApiException("A venue must be selected to request resources");
        }

        List<LetterResourceRequest> requests = new ArrayList<>();
        List<User> responsiblePersons = new ArrayList<>();

        for (ResourceRequestItemDto item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ApiException("Requested quantity must be greater than zero");
            }
            PlaceResource resource = placeResourceRepository.findById(item.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", item.getResourceId()));
            if (!resource.getPlace().getPlaceName().equalsIgnoreCase(placeName.trim())) {
                throw new ApiException("Resource \"" + resource.getName() + "\" does not belong to the selected place");
            }

            User responsible = resource.getPlace().getResponsiblePerson();
            addResolvedResource(requests, responsiblePersons, resource.getName(), responsible,
                    item.getQuantity(), resource.getQuantity(), false);
        }

        return new ResolvedResources(requests, responsiblePersons);
    }

    // Resolves standalone equipment (not tied to any place) - each one carries its own required
    // responsible person.
    private ResolvedResources resolveGeneralResourceRequests(List<ResourceRequestItemDto> items) {
        if (items == null || items.isEmpty()) {
            return new ResolvedResources(List.of(), List.of());
        }

        List<LetterResourceRequest> requests = new ArrayList<>();
        List<User> responsiblePersons = new ArrayList<>();

        for (ResourceRequestItemDto item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ApiException("Requested quantity must be greater than zero");
            }
            GeneralResource resource = generalResourceRepository.findById(item.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", item.getResourceId()));

            addResolvedResource(requests, responsiblePersons, resource.getName(), resource.getResponsiblePerson(),
                    item.getQuantity(), resource.getQuantity(), true);
        }

        return new ResolvedResources(requests, responsiblePersons);
    }

    private void addResolvedResource(
            List<LetterResourceRequest> requests, List<User> responsiblePersons,
            String resourceName, User responsible, Integer quantityRequested, Integer quantityAvailable,
            boolean isGeneralResource
    ) {
        LetterResourceRequest lrr = new LetterResourceRequest();
        lrr.setResourceName(resourceName);
        lrr.setGeneralResource(isGeneralResource);
        lrr.setResponsiblePersonRegNumber(responsible != null ? responsible.getRegNumber() : null);
        lrr.setResponsiblePersonName(responsible != null ? responsible.getUserName() : null);
        lrr.setQuantityRequested(quantityRequested);
        lrr.setQuantityAvailable(quantityAvailable);
        requests.add(lrr);

        if (responsible != null && responsiblePersons.stream()
                .noneMatch(u -> u.getRegNumber().equals(responsible.getRegNumber()))) {
            responsiblePersons.add(responsible);
        }
    }

    private ResolvedResources mergeResolved(ResolvedResources a, ResolvedResources b) {
        List<LetterResourceRequest> requests = new ArrayList<>(a.requests());
        requests.addAll(b.requests());

        List<User> responsiblePersons = new ArrayList<>(a.responsiblePersons());
        for (User u : b.responsiblePersons()) {
            if (responsiblePersons.stream().noneMatch(existing -> existing.getRegNumber().equals(u.getRegNumber()))) {
                responsiblePersons.add(u);
            }
        }
        return new ResolvedResources(requests, responsiblePersons);
    }

    private void attachResourceRequests(Letter letter, ResolvedResources resolved) {
        letter.getResourceRequests().clear();
        for (LetterResourceRequest lrr : resolved.requests()) {
            lrr.setLetter(letter);
            letter.getResourceRequests().add(lrr);
        }
    }

    // Used on resend when the secretary doesn't resubmit a resources list: re-resolve the TOs
    // from what's already snapshotted on the letter rather than dropping them silently. Split by
    // source so standalone-equipment approvers keep requiring a signature after the rebuild.
    private List<User> placeResponsiblePersonsFromExistingRequests(Letter letter) {
        return responsiblePersonsFromExistingRequests(letter, false);
    }

    private List<User> generalResponsiblePersonsFromExistingRequests(Letter letter) {
        return responsiblePersonsFromExistingRequests(letter, true);
    }

    private List<User> responsiblePersonsFromExistingRequests(Letter letter, boolean generalOnly) {
        List<String> regNumbers = letter.getResourceRequests().stream()
                .filter(r -> r.isGeneralResource() == generalOnly)
                .map(LetterResourceRequest::getResponsiblePersonRegNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (regNumbers.isEmpty()) return List.of();

        List<User> resolved = new ArrayList<>();
        for (String regNumber : regNumbers) {
            userRepository.findByRegNumber(regNumber).ifPresent(resolved::add);
        }
        return resolved;
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
}
