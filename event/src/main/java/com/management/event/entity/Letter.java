package com.management.event.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    // The club this letter belongs to. Nullable only for legacy rows created before the club flow.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    private String title;

    private LocalDate eventDate;

    private LocalTime eventTime;

    // End time for the event (required for conflict detection on new requests).
    private LocalTime eventEndTime;

    private String eventPlace;

    @Column(length = 2000)
    private String description;

    private String pdfPath;

    // Optional output path after stamping a signature.
    private String signedPdfPath;

    // The regNumber of the user who stamped the signature.
    private String signedByRegNumber;

    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    private LetterStatus globalStatus;

    @Column(length = 1000)
    private String rejectionReason;

    // Optional note from the final approver to be shown to the letter placer.
    @Column(length = 1000)
    private String approvalNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "letter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LetterResourceRequest> resourceRequests = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
