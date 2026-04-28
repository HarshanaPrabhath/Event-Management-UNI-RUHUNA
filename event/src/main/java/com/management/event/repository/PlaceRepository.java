package com.management.event.repository;


import com.management.event.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAll();

    Optional<Place> findByPlaceName(String placeName);
}