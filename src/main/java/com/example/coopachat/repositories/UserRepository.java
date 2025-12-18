package com.example.coopachat.repositories;

import com.example.coopachat.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    // ============================================================================
    // 🔍 RECHERCHES
    // ============================================================================

    /** Vérifie si un email existe déjà dans la base de données */
    Boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    /** Vérifie si un téléphone existe déjà dans la base de données */
    Boolean existsByPhone(String phone);


}
