package com.platform.orchestrator.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if user exists by username.
     */
    boolean existsByUsername(String username);

    /**
     * Check if user exists by email.
     */
    boolean existsByEmail(String email);
}
