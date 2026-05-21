package com.management.event.repository;

import com.management.event.entity.ClubSeniorTreasurer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClubSeniorTreasurerRepository extends JpaRepository<ClubSeniorTreasurer, Long> {
    Optional<ClubSeniorTreasurer> findByClub_Id(Long clubId);
    Optional<ClubSeniorTreasurer> findByUser_RegNumber(String regNumber);

    @Query("select cst from ClubSeniorTreasurer cst join fetch cst.user join fetch cst.club")
    List<ClubSeniorTreasurer> findAllWithUserAndClub();
}

