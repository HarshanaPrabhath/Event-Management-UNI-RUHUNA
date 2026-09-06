package com.management.event.repository;

import com.management.event.entity.GeneralResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralResourceRepository extends JpaRepository<GeneralResource, Long> {
}
