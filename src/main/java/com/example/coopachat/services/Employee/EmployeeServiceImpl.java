package com.example.coopachat.services.Employee;


import com.example.coopachat.entities.Users;
import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.repositories.ActivationCodeRepository;
import com.example.coopachat.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * Implémentation du service de gestion des salariés
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final PasswordEncoder passwordEncoder;

    // ============================================================================
    // 🔐 ACTIVATION D'UN COMPTE SALARIE
    // ============================================================================

    /**
     * Active le compte d'un salarié et crée son mot de passe via le token d'invitation
     */
    @Override
    @Transactional
    public void activateEmployeeAccount(String token, String newPassword, String confirmPassword) {
        // Vérifier que les deux mots de passe sont identiques
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Récupérer le token depuis la base de données (type EMPLOYEE_INVITATION)
        ActivationCode activationCode = activationCodeRepository.findByCodeAndTypeAndUsedFalse(token, CodeType.EMPLOYEE_INVITATION)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        // Vérifier que le token n'est pas expiré
        if (activationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        // Récupérer l'email depuis le token
        String email = activationCode.getEmail();

        // Récupérer l'utilisateur associé au token
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Encoder et sauvegarder le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));

        // Activer le compte
        user.setIsActive(true);

        userRepository.save(user);

        // Marquer le token comme utilisé pour éviter la réutilisation
        activationCode.setUsed(true);
        activationCodeRepository.save(activationCode);

        log.info("Compte salarié activé avec succès pour: {}", email);
    }
}