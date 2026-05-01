package com.management.event.repository;

import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findByUserRegNumberOrderByIdDesc(String regNumber);

    Optional<Letter> findByTitleAndEventDateAndEventTimeAndEventPlace(
            String title,
            LocalDate eventDate,
            LocalTime eventTime,
            String eventPlace
    );

    List<Letter> findByGlobalStatusAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
            LetterStatus globalStatus,
            LocalDate from,
            LocalDate to
    );

    List<Letter> findByGlobalStatusOrderByEventDateAscEventTimeAsc(LetterStatus globalStatus);

    List<Letter> findByGlobalStatusInOrderByEventDateAscEventTimeAsc(Set<LetterStatus> statuses);

    List<Letter> findByGlobalStatusInAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
            Set<LetterStatus> statuses,
            LocalDate from,
            LocalDate to
    );
}
