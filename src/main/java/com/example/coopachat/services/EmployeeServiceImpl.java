package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Users;
import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.ActivationCodeRepository;
import com.example.coopachat.repositories.CompanyRepository;
import com.example.coopachat.repositories.EmployeeRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implémentation du service de gestion des salariés
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${activation.code.expiration.minutes:15}")
    private int expirationMinutes;

    @Override
    public void addEmployee(CreateEmployeeDTO createEmployeeDTO) {

        // Récupérer le commercial connecté
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        
        String commercialEmail = authentication.getName();
        Users commercial = userRepository.findByEmail(commercialEmail)
                .orElseThrow(() -> new RuntimeException("Commercial introuvable"));

        // Vérifier que l'email n'existe pas déjà
        if (userRepository.existsByEmail(createEmployeeDTO.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Vérifier que le téléphone n'existe pas déjà
        if (userRepository.existsByPhone(createEmployeeDTO.getPhone())) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        // Récupérer l'entreprise associée
        Company company = companyRepository.findById(createEmployeeDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        // Créer l'utilisateur (salarié)
        Users newUser = new Users();
        newUser.setEmail(createEmployeeDTO.getEmail());
        newUser.setFirstName(createEmployeeDTO.getFirstName());
        newUser.setLastName(createEmployeeDTO.getLastName());
        newUser.setPhone(createEmployeeDTO.getPhone());
        newUser.setIsActive(false);
        newUser.setRole(UserRole.EMPLOYEE);

        Users savedUser = userRepository.save(newUser);

        // Créer l'employé
        Employee newEmployee = new Employee();
        newEmployee.setCompany(company);
        newEmployee.setAddress(createEmployeeDTO.getAddress());
        newEmployee.setUser(savedUser);
        newEmployee.setCreatedBy(commercial);

        employeeRepository.save(newEmployee);

        // Générer un token UUID unique pour l'invitation
        String invitationToken = UUID.randomUUID().toString();

        // Calculer la date d'expiration (15 minutes par défaut)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Supprimer les anciens tokens d'invitation pour cet email
        activationCodeRepository.deleteByEmailAndType(createEmployeeDTO.getEmail(), CodeType.EMPLOYEE_INVITATION);

        // Créer et sauvegarder le token d'invitation
        ActivationCode activationCode = new ActivationCode();
        activationCode.setEmail(createEmployeeDTO.getEmail());
        activationCode.setCode(invitationToken);
        activationCode.setType(CodeType.EMPLOYEE_INVITATION);
        activationCode.setExpiresAt(expiresAt);
        activationCode.setUsed(false);

        activationCodeRepository.save(activationCode);

        // Envoyer l'email d'invitation avec le lien d'activation
        String commercialFullName = commercial.getFirstName() + " " + commercial.getLastName();
        emailService.sendEmployeeInvitation(createEmployeeDTO.getEmail(), invitationToken,createEmployeeDTO.getFirstName(), commercialFullName, company.getName());
    }

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
