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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
