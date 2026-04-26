package com.management.event.repository;

import com.management.event.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserName(String username);

    boolean existsByEmail(String email);

    boolean existsByRegNumber(String regNumber);

    Optional<User> findByRegNumber(String regNumber);
}
