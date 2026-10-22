package com.management.event.repository;

import com.management.event.entity.StepStatus;
import com.management.event.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByUserRegNumberAndStatusOrderByLetterIdDesc(String regNumber, StepStatus status);

    // Full history across every resend round, oldest round/step first - for display.
    List<WorkflowStep> findByLetterIdOrderByResendRoundAscStepOrderAsc(Long letterId);

    // Only the live round's steps - for the approval state machine (current/next/treasurer lookups).
    List<WorkflowStep> findByLetterIdAndResendRoundOrderByStepOrderAsc(Long letterId, int resendRound);

    Optional<WorkflowStep> findByLetterIdAndStatus(Long letterId, StepStatus status);
}
