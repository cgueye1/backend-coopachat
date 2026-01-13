package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.DriverRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des actions du Responsable Logistique
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsManagerServiceImpl implements LogisticsManagerService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;

    // ============================================================================
    // 🚚CRÉER UN LIVREUR
    // ============================================================================

     @Override
    @Transactional
    public void createDriver(RegisterDriverRequestDTO driverDTO) {

        // Vérifier que l'email n'existe pas déjà
        if (userRepository.existsByEmail(driverDTO.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Vérifier que le téléphone n'existe pas déjà
        if (userRepository.existsByPhone(driverDTO.getPhone())) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        // Récupérer le Responsable Logistique connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users logisticsManager = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

        // Vérifier que l'utilisateur connecté est bien un Responsable Logistique
        if (logisticsManager.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un Responsable Logistique peut créer un livreur");
        }

        // Créer l'utilisateur (livreur)
        Users user = new Users();
        user.setFirstName(driverDTO.getFirstName());
        user.setLastName(driverDTO.getLastName());
        user.setEmail(driverDTO.getEmail());
        user.setPhone(driverDTO.getPhone());
        user.setRole(UserRole.DELIVERY_DRIVER);
        user.setIsActive(false);

        Users savedUser = userRepository.save(user);

        // Créer le livreur
        Driver newDriver = new Driver();
        newDriver.setUser(savedUser);
        newDriver.setCreatedBy(logisticsManager);

        driverRepository.save(newDriver);

        // Créer et sauvegarder le code d'activation
        String codeActivation = activationCodeService.generateAndStoreCodeMobile(driverDTO.getEmail());

        // Envoyer l'email d'invitation avec le code d'activation
        emailService.sendDriverActivationCode(driverDTO.getEmail(), codeActivation, driverDTO.getFirstName());

        log.info("Livreur créé avec succès par le Responsable Logistique: {}", logisticsManager.getEmail());
    }
}

