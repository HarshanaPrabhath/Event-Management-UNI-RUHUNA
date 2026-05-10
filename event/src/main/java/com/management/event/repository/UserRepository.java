package com.management.event.repository;

import com.management.event.entity.AppRole;
import com.management.event.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserName(String username);

    boolean existsByEmail(String email);

    boolean existsByRegNumber(String regNumber);

    Optional<User> findByRegNumber(String regNumber);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName NOT IN :excludedRoles")
    List<User> findUsersExcludingRoles(@Param("excludedRoles") List<AppRole> excludedRoles);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName = :role")
    List<User> findByRoleName(@Param("role") AppRole role);
}
