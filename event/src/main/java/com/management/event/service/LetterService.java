package com.management.event.service;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.ApproverDto;
import com.management.event.dto.LetterPlaceRequestDto;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.StepStatus;
import com.management.event.entity.User;
import com.management.event.entity.WorkflowStep;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.LetterRepository;
import com.management.event.repository.UserRepository;
import com.management.event.repository.WorkflowStepRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        Letter letter = new Letter();
        letter.setUser(requester);
        letter.setTitle(letterPlaceRequestDto.getEventName());
        letter.setEventDate(letterPlaceRequestDto.getEventDate());
        letter.setEventTime(letterPlaceRequestDto.getEventTime());
        letter.setEventPlace(letterPlaceRequestDto.getEventPlace());
        letter.setDescription(letterPlaceRequestDto.getDescription());
        letter.setPdfPath(storePdf(letterPlaceRequestDto.getLetterPdf()));
        letter.setGlobalStatus(LetterStatus.PENDING);

        Letter savedLetter = letterRepository.save(letter);

        List<WorkflowStep> steps = new ArrayList<>();

        for (int i = 0; i < approvers.size(); i++) {
            WorkflowStep step = new WorkflowStep();
            step.setLetter(savedLetter);
            step.setUser(approvers.get(i));
            step.setStepOrder(i + 1);
            step.setStatus(i == 0 ? StepStatus.CURRENT : StepStatus.WAITING);
            steps.add(step);
        }

        workflowStepRepository.saveAll(steps);
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
