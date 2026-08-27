package com.management.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "club_executive",
        uniqueConstraints = {
                // exactly one executive per role per club (e.g. 1 secretary per club, 1 senior treasurer per club)
                @UniqueConstraint(columnNames = {"club_id", "executive_role"}),
                // one club assignment per role per user (a user can't be secretary for 2 clubs, etc.)
                @UniqueConstraint(columnNames = {"user_reg_number", "executive_role"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubExecutive {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "executive_role", nullable = false, length = 30)
    private ClubExecutiveRole executiveRole;

    private Instant createdAt = Instant.now();
}

