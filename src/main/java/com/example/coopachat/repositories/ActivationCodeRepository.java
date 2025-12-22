package com.example.coopachat.repositories;

import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
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

    /** Vérifie si un code d'activation a été utilisé pour un email */
    @Query("SELECT COUNT(ac) > 0 FROM ActivationCode ac WHERE ac.email = :email AND ac.used = true ")
    Boolean hasUsedCode (String email);

    /** Recherche un token par code et type (non utilisé) */
    Optional<ActivationCode> findByCodeAndTypeAndUsedFalse(String code, CodeType type);
    
    /** Recherche un token valide (non utilisé et non expiré) par code et type */
    @Query("SELECT ac FROM ActivationCode ac WHERE ac.code = :code AND ac.type = :type AND ac.used = false AND ac.expiresAt > :now")
    Optional<ActivationCode> findValidTokenByCodeAndType(String code, CodeType type, LocalDateTime now);

    /** Recherche le dernier code envoyé pour un email et un type (trié par date de création décroissante) */
    @Query ("SELECT ac FROM ActivationCode ac WHERE ac.email= :email AND ac.type = :type ORDER BY ac.createdAt desc ")
    Optional <ActivationCode> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, CodeType type);

    // ============================================================================
    // 🗑️ NETTOYAGE
    // ============================================================================
    /** Supprime tous les codes expirés pour un utilisateur spécifique*/
     @Modifying(clearAutomatically = true)
     void deleteByEmail(String email);

     @Modifying(clearAutomatically = true)
     void deleteByEmailAndType(String email, CodeType type);

    /**
     * Supprime tous les codes expirés ET non utilisés
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivationCode ac WHERE ac.expiresAt < :now AND ac.used = false")
    void deleteExpiredCodes(LocalDateTime now);

    /**
     * Nettoyer les codes utilisés anciens
     * Supprime les codes utilisés créés il y a plus de 24 heures
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivationCode ac WHERE ac.used = true AND ac.createdAt < :oldDate")
    void deleteOldUsedCodes(LocalDateTime oldDate);

}
