package com.management.event.repository;

import com.management.event.entity.ClubSecretary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubSecretaryRepository extends JpaRepository<ClubSecretary, Long> {
    Optional<ClubSecretary> findByClub_Id(Long clubId);
    Optional<ClubSecretary> findByUser_RegNumber(String regNumber);
}

