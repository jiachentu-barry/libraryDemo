package com.example.demo5.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByUsernameIgnoreCase(String username);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
