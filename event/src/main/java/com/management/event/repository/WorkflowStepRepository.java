package com.management.event.repository;

import com.management.event.entity.StepStatus;
import com.management.event.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByUserUserIdOrderByLetterIdDescStepOrderAsc(Integer userId);

    List<WorkflowStep> findByLetterIdOrderByStepOrderAsc(Long letterId);

    Optional<WorkflowStep> findByLetterIdAndStatus(Long letterId, StepStatus status);
}
