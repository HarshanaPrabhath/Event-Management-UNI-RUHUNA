package com.management.event.repository;

import com.management.event.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
}
