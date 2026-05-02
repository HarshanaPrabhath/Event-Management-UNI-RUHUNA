package com.management.event.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "reg_number"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    @Id
    @NotEmpty
    @Column(name = "reg_number", unique = true, nullable = false, updatable = false)
    private String regNumber;

    @NotEmpty
    @Size(max = 30)
    @Column(name = "user_name", nullable = false)
    private String userName;

    @NotEmpty
    @Size(max = 50)
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @NotEmpty
    @Column(name = "password", nullable = false)
    private String password;

    // Stored as a path under /uploads (e.g. uploads/signatures/REG-123.png)
    @Column(name = "signature_image_path", length = 500)
    private String signatureImagePath;

    @ToString.Exclude
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_reg_number", referencedColumnName = "reg_number"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
