package com.management.event.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "club_senior_treasurer",
        uniqueConstraints = {
                // exactly one senior treasurer per club
                @UniqueConstraint(columnNames = "club_id"),
                // and one club per senior treasurer user
                @UniqueConstraint(columnNames = "user_reg_number")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubSeniorTreasurer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // User PK is reg_number
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_reg_number", referencedColumnName = "reg_number", nullable = false)
    private User user;

    private Instant createdAt = Instant.now();
}

