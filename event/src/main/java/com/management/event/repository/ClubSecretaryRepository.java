package com.management.event.repository;

import com.management.event.entity.ClubSecretary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClubSecretaryRepository extends JpaRepository<ClubSecretary, Long> {
    Optional<ClubSecretary> findByClub_Id(Long clubId);
    Optional<ClubSecretary> findByUser_RegNumber(String regNumber);

    @Query("select cs from ClubSecretary cs join fetch cs.user join fetch cs.club")
    List<ClubSecretary> findAllWithUserAndClub();
}
