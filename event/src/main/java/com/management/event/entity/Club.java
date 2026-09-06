package com.management.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "club",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "club_name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, unique = true, length = 160)
    private String clubName;

    // Reg number of the user assigned as this club's secretary. Unique: a person can secretary
    // at most one club at a time.
    @Column(name = "secretary_reg_number", unique = true, length = 50)
    private String secretaryRegNumber;

    // Reg number of the user assigned as this club's senior treasurer. Unique: a person can be
    // senior treasurer of at most one club at a time.
    @Column(name = "senior_treasurer_reg_number", unique = true, length = 50)
    private String seniorTreasurerRegNumber;

    @Column(name = "bg_image_path", length = 500)
    private String bgImagePath;

    @Column(name = "vision", length = 2000)
    private String vision;

    @Column(name = "mission", length = 2000)
    private String mission;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    // Stored as a JSON string (key-value pairs like {"president":"Harshana"}).
    @Column(name = "executive_board_json", columnDefinition = "LONGTEXT")
    private String executiveBoardJson;

    // General club members, hand-entered by the secretary. Stored as a JSON array of
    // {"role": "...", "name": "..."} objects - the role is free text, not a system permission.
    @Column(name = "members_json", columnDefinition = "LONGTEXT")
    private String membersJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
