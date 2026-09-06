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

// Equipment that isn't tied to any specific place (e.g. a portable projector, a generator) and can
// be requested on any letter regardless of venue. Unlike PlaceResource, each one has its own
// required responsible person, since there's no place to inherit one from.
@Entity
@Table(name = "general_resource")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_person_reg_number", referencedColumnName = "reg_number", nullable = false)
    private User responsiblePerson;
}
