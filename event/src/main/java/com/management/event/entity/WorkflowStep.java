package com.management.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "letter_id", nullable = false)
    private Letter letter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_reg_number", referencedColumnName = "reg_number", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    private String remarks;

    // When this step became CURRENT (nullable).
    private LocalDateTime assignedAt;

    // When this step was APPROVED/REJECTED (nullable).
    private LocalDateTime actedAt;
}
