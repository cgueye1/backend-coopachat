package com.example.coopachat.repositories;

import com.example.coopachat.entities.auth.ActivationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ActivationCodeRepository extends JpaRepository<ActivationCode, Long> {

    // ============================================================================
    // 🔍 RECHERCHES
    // ============================================================================

    /** Recherche un code d'activation par email et code */
    Optional<ActivationCode> findByEmailAndCode(String email, String code);

    /** Recherche un code d'activation valide (non utilisé et non expiré) */
    @Query("SELECT ac FROM ActivationCode ac WHERE ac.email = :email AND ac.code = :code AND ac.used = false AND ac.expiresAt > :now")
    Optional<ActivationCode> findValidCode(String email, String code, LocalDateTime now);


    // ============================================================================
    // 🗑️ NETTOYAGE
    // ============================================================================
    /** Supprime tous les codes expirés pour un utilisateur spécifique*/
     @Modifying(clearAutomatically = true)
     void deleteByEmail(String email);

    /** Supprime tous les codes expirés */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivationCode ac WHERE ac.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);

}
