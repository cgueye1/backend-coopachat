package com.example.coopachat.services.auth;

import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.repositories.ActivationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Implémentation du service de gestion des codes d'activation
 * Gère la génération, le stockage, la validation et le nettoyage des codes d'activation
 */
@Service
@RequiredArgsConstructor
@Slf4j// permet de logger les messages 
public class ActivationCodeServiceImpl implements ActivationCodeService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final ActivationCodeRepository activationCodeRepository;

    // ============================================================================
    // ⏱️ CONFIGURATION
    // ============================================================================

    private static final int CODE_LENGTH = 6;
    @Value("${activation.code.expiration.minutes:15}")
    private int expirationMinutes;  // 15 minutes


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
     * Génère et stocke un code d'activation pour un email
     */
    @Override
    @Transactional
    public String generateAndStoreCode(String email){

        // Supprimer les anciens codes de cet email (pour éviter les doublons)
        activationCodeRepository.deleteByEmail(email);

        // Générer un nouveau code
        String code = generateActivationCode();

        // Calculer la date d'expiration (15 minutes à partir de maintenant)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Créer et sauvegarder le code d'activation
        ActivationCode activationCode = new ActivationCode();
        activationCode.setCode(code);
        activationCode.setExpiresAt(expiresAt);
        activationCode.setEmail(email);
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
    public boolean verifyActivationCode(String email, String code){

        //Rechercher un code valide (non utilisé et non expiré)
        Optional<ActivationCode> activationCodeOpt = activationCodeRepository.findValidCode(
                email,
                code,
                LocalDateTime.now()
        );
        return activationCodeOpt.isPresent();
    }

    /**
     * Vérifie si un code d'activation a été utilisé pour un email (utile pour la création d'un mot de passe )
     */
    @Override
    public boolean hasUsedActivationCode(String email) {
        Boolean hasUsed= activationCodeRepository.hasUsedCode(email);
        return hasUsed;
    }


    /**
     * Marque un code d'activation comme utilisé
     */
    @Override
    @Transactional
    public void markCodeAsUsed(String email, String code){
        Optional <ActivationCode> activationCodeOpt = activationCodeRepository.findByEmailAndCode(email, code);
        if (activationCodeOpt.isPresent()) {
            activationCodeOpt.get().setUsed(true);
            activationCodeRepository.save(activationCodeOpt.get());
            log.info("Code d'activation marqué comme utilisé pour l'email: {}", email);
        }
    }

    // ============================================================================
    // 🗑️ NETTOYAGE DES CODES EXPIRÉS ET NON UTILISÉS
    // ============================================================================
    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures à la minute 0 on exécute la méthode (Seconde 0, Minute 0, * tous les heures, * tous les jours, * tous les mois, * tous les jours de la semaine ) ,  marque une méthode à exécuter automatiquement
     public void cleanupExpiredCodes(){
        LocalDateTime now = LocalDateTime.now();
        activationCodeRepository.deleteExpiredCodes(now);
        log.info("Nettoyage des codes d'activation expirés effectué");
    }

    // ============================================================================
    // 🗑️ NETTOYAGE DES CODES UTILISÉS ANCIENS
    // ============================================================================
    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Tous les jours à minuit
    public void cleanupOldUsedCodes() {
        // Supprimer les codes utilisés créés il y a plus de 24 heures
        LocalDateTime oldDate = LocalDateTime.now().minusHours(24);
        activationCodeRepository.deleteOldUsedCodes(oldDate);
        log.info("Nettoyage des codes d'activation utilisés anciens effectué");
    }

}
