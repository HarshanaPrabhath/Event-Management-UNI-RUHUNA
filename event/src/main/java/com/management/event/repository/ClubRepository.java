package com.management.event.repository;

import com.management.event.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByClubName(String clubName);
    boolean existsByClubName(String clubName);
    Optional<Club> findBySecretaryRegNumber(String secretaryRegNumber);
    Optional<Club> findBySeniorTreasurerRegNumber(String seniorTreasurerRegNumber);
}

