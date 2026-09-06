package com.management.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// What a letter asked for, at the time it was submitted. Snapshots the resource/department
// names (same convention as Letter#eventPlace) rather than a live FK, so the record stays
// accurate even if the admin later renames/removes the underlying PlaceResource.
@Entity
@Table(name = "letter_resource_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetterResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "letter_id", nullable = false)
    private Letter letter;

    @Column(nullable = false, length = 120)
    private String resourceName;

    // True for standalone equipment (not tied to a place) - its responsible person's approval
    // step must be signed, unlike a place-bound resource's (which just uses the venue TO).
    @Column(nullable = false)
    private boolean generalResource;

    // Snapshot of who was responsible for this resource at request time (the resource's own
    // override, or the place's responsible person) - used to rebuild workflow steps on resend
    // without re-resolving (the assignment may have changed since), and to display to reviewers.
    @Column(length = 50)
    private String responsiblePersonRegNumber;

    @Column(length = 100)
    private String responsiblePersonName;

    @Column(nullable = false)
    private Integer quantityRequested;

    // How many were available at this place when the request was made (for the TO's context).
    private Integer quantityAvailable;
}
