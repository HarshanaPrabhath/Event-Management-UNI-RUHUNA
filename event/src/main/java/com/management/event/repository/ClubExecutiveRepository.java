package com.management.event.repository;

import com.management.event.entity.ClubExecutive;
import com.management.event.entity.ClubExecutiveRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubExecutiveRepository extends JpaRepository<ClubExecutive, Long> {
    Optional<ClubExecutive> findByClub_IdAndExecutiveRole(Long clubId, ClubExecutiveRole executiveRole);
    Optional<ClubExecutive> findByUser_RegNumberAndExecutiveRole(String regNumber, ClubExecutiveRole executiveRole);

    @Query("select ce from ClubExecutive ce join fetch ce.user join fetch ce.club where ce.executiveRole = :role")
    List<ClubExecutive> findAllWithUserAndClubByRole(@Param("role") ClubExecutiveRole role);
}

