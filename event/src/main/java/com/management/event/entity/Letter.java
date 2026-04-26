package com.management.event.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "letters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_reg_number", referencedColumnName = "reg_number", nullable = false)
    private User user;

    private String title;

    private LocalDate eventDate;

    private LocalTime eventTime;

    private String eventPlace;

    @Column(length = 2000)
    private String description;

    private String pdfPath;

    @Enumerated(EnumType.STRING)
    private LetterStatus globalStatus;

    @Column(length = 1000)
    private String rejectionReason;
}
