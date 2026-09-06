package com.management.event.repository;

import com.management.event.entity.PlaceResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceResourceRepository extends JpaRepository<PlaceResource, Long> {
    List<PlaceResource> findByPlace_PlaceId(Long placeId);
    void deleteByPlace_PlaceId(Long placeId);
}
