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

// A single equipment/resource line item that's part of a place (e.g. "Microphone" x2 in Lab11).
// Always approved by that place's own responsible person - it has no responsible person of its own.
@Entity
@Table(name = "place_resource")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer quantity;
}
