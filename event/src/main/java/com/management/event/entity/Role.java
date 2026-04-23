package com.management.event.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "role_id")
    private Integer roleId;

    @NotNull
    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    @Column(length = 20,unique = true)
    private AppRole roleName;

    public Role(AppRole appRole) {
        this.roleName = appRole;
    }
}
