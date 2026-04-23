package com.example.coopachat.services.auth;

import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.repositories.ActivationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Implémentation du service de gestion des codes d'activation
 * Gère la génération, le stockage, la validation et le nettoyage des codes d'activation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationCodeServiceImpl implements ActivationCodeService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final ActivationCodeRepository activationCodeRepository;

    // ============================================================================
    // ⏱️ CONFIGURATION
    // ============================================================================

    private static final int CODE_LENGTH = 6;
    private static final int CODE_LENGTH_MOBILE = 4;

    @Value("${activation.code.expiration.minutes:15}")
    private int expirationMinutes;  // 15 minutes

    @Value("${activation.code.resend.cooldown.seconds:30}")
    private int resendCooldownSeconds;  // 30 secondes par défaut


    // ============================================================================
    // 🔢 GÉNÉRATION DE CODES
    // ============================================================================

    /**
     * Génère un code d'activation à 6 chiffres
     */
    @Override
    public String generateActivationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(); //permet de construire le code d'activation en concaténant les chiffres générés aléatoirement
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();//retourne le code d'activation sous forme de chaîne de caractères
    }

    /**
     * Génère un code d'activation à 4 chiffres (pour mobile)
     */
    @Override
    public String generateActivationCodeMoblie() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(); //permet de construire le code d'activation en concaténant les chiffres générés aléatoirement
        for (int i = 0; i < CODE_LENGTH_MOBILE; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();//retourne le code d'activation sous forme de chaîne de caractères
    }

    /**
     * Génère et stocke un code d'activation pour un email
     */
    @Override
    @Transactional
    public String generateAndStoreCode(String email) {

        // Supprimer les anciens codes de cet email (pour éviter les doublons)
        activationCodeRepository.deleteByEmail(email);
        activationCodeRepository.flush();
        log.info("Codes supprimés pour {}", email);

        // Générer un nouveau code
        String code = generateActivationCode();

        // Calculer la date d'expiration (15 minutes à partir de maintenant)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Créer et sauvegarder le code d'activation
        ActivationCode activationCode = new ActivationCode();
        activationCode.setCode(code);
        activationCode.setExpiresAt(expiresAt);
        activationCode.setEmail(email);
        activationCode.setType(CodeType.ACTIVATION);
        activationCode.setUsed(false);
        activationCodeRepository.save(activationCode);

        return code;
    }

    /**
     * Génère et stocke un code d'activation pour un email par mobile
     */
    @Override
    @Transactional
    public String generateAndStoreCodeMobile(String email) {

        // Supprimer les anciens codes de cet email (pour éviter les doublons)
        activationCodeRepository.deleteByEmail(email);

        // Générer un nouveau code
        String code = generateActivationCodeMoblie();

        // Calculer la date d'expiration (15 minutes à partir de maintenant)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Créer et sauvegarder le code d'activation
        ActivationCode activationCode = new ActivationCode();
        activationCode.setCode(code);
        activationCode.setExpiresAt(expiresAt);
        activationCode.setEmail(email);
        activationCode.setType(CodeType.ACTIVATION);
        activationCode.setUsed(false);
        activationCodeRepository.save(activationCode);

        return code;
    }

    // ============================================================================
    // ✅ VALIDATION DE CODES
    // ============================================================================

    /**
     * Valide un code d'activation pour un email
     */
    @Override
    public boolean verifyActivationCode(String email, String code) {

        //Rechercher un code valide (non utilisé et non expiré)
        Optional<ActivationCode> activationCodeOpt = activationCodeRepository.findValidCode(
                email,
                code,
                LocalDateTime.now()
        );
        return activationCodeOpt.isPresent();
    }

    @Override
    @Transactional
    public void markCodeAsUsed(String email, String code) {
        Optional<ActivationCode> activationCodeOpt = activationCodeRepository.findByEmailAndCode(email, code);
        if (activationCodeOpt.isPresent()) {
            activationCodeOpt.get().setUsed(true);
            activationCodeRepository.save(activationCodeOpt.get());
            log.info("Code marqué comme utilisé pour l'email: {}", email);
        }
    }

    // ============================================================================
    // ⏱️ GESTION DU COOLDOWN
    // ============================================================================

    /**
     * Calcule le temps restant (en secondes) avant de pouvoir renvoyer un code
     */
    @Override
    public long getRemainingCooldownSecond(String email, CodeType type) {
        try{
            // Récupérer le dernier code envoyé
            Optional<ActivationCode> lastCodeOpt = activationCodeRepository
                    .findTopByEmailAndTypeOrderByCreatedAtDesc(email, type);

            if (lastCodeOpt.isEmpty()) {
                return 0;
            }

            ActivationCode lastCode = lastCodeOpt.get();
            // Calculer le temps écoulé depuis le dernier envoi (en secondes)
            long secondsSinceLastCode = Duration
                    .between(lastCode.getCreatedAt(), LocalDateTime.now())
                    .getSeconds();

            // Si le délai minimum n'est pas encore écoulé, retourner le temps restant
            if (secondsSinceLastCode < resendCooldownSeconds) {
                return resendCooldownSeconds - secondsSinceLastCode;
            }

            // On peut renvoyer immédiatement
            return 0;

        } catch (Exception e) {
            log.error("Erreur lors du calcul du cooldown pour {}: {}", email, e.getMessage());
            // En cas d'erreur, retourner un délai par défaut
            return resendCooldownSeconds;
        }
    }


    // ============================================================================
    // 🗑️ NETTOYAGE DES CODES
    // ============================================================================

    /**
     * Nettoie les codes d'activation expirés et non utilisés
     * Exécuté automatiquement toutes les heures à la minute 0
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures à la minute 0
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        activationCodeRepository.deleteExpiredCodes(now);
        log.info("Nettoyage des codes d'activation expirés effectué");
    }

    /**
     * Nettoie les codes d'activation utilisés anciens (plus de 24 heures)
     * Exécuté automatiquement tous les jours à minuit
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Tous les jours à minuit
    public void cleanupOldUsedCodes() {
        // Supprimer les codes utilisés créés il y a plus de 24 heures
        LocalDateTime oldDate = LocalDateTime.now().minusHours(24);
        activationCodeRepository.deleteOldUsedCodes(oldDate);
        log.info("Nettoyage des anciens codes d'activation utilisés  effectué");
    }


    
}
