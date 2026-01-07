package com.example.coopachat.repositories;

import com.example.coopachat.entities.auth.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    /** Vérifie si un token est dans la blacklist */
    Optional<TokenBlacklist> findByToken(String token);

    /** Supprime tous les tokens expirés de la blacklist */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}