package com.management.event.repository;

import com.management.event.entity.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findByUserRegNumberOrderByIdDesc(String regNumber);
}
