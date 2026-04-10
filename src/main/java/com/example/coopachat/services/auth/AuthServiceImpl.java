package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserDto;
import com.example.coopachat.dtos.user.UpdateMyProfileRequestDTO;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.dtos.auth.ProfileUpdateResponseDTO;
import com.example.coopachat.dtos.auth.RegisterMobileDTO;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Users;
import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.enums.PasswordResetChannel;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.exceptions.ResourceNotFoundException;
import com.example.coopachat.repositories.ActivationCodeRepository;
import com.example.coopachat.repositories.EmployeeRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.admin.AdminService;
import com.example.coopachat.services.user.UserReferenceGenerator;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implémentation du service d'authentification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    /** Aligné sur l’entité {@link com.example.coopachat.entities.Users} (@Email). */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** Aligné sur {@code Users.phone} : {@code ^[+]?[0-9\s\-\(\)]{8,25}$} */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{8,25}$");

    @Value("${activation.code.expiration.minutes:15}")
    private int expirationMinutes;  // 15 minutes


    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;
    private final ActivationCodeRepository activationCodeRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmployeeRepository employeeRepository;
    private final UserReferenceGenerator userReferenceGenerator;
    private final AdminService adminService;

    // ============================================================================
    // 👤 GESTION DES UTILISATEURS
    // ============================================================================

    /**
     * Ajoute un nouvel utilisateur dans le système
     */
    @Override
    public void addUser(UserDto userDto) {

        if (userDto.getRole() == UserRole.COMMERCIAL || userDto.getRole() == UserRole.LOGISTICS_MANAGER) {
            throw new ValidationException(
                    "Les comptes Commercial et Responsable logistique sont créés par un administrateur. "
                            + "Utilisez la page « Activer mon compte » avec l’adresse e-mail qui vous a été communiquée.");
        }

        // Validation : vérifier si l'email existe déjà
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        // Validation : vérifier si le téléphone existe déjà
        if (userRepository.existsByPhone(userDto.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException("Ce téléphone est déjà utilisé");
        }

        // Mapping : convertir le DTO en entité
        Users user = convertToEntity(userDto);
        user.setRefUser(userReferenceGenerator.generateUniqueRefUser());

        // Sauvegarde : enregistrer l'utilisateur en base de données
        userRepository.save(user);
    }

    // ============================================================================
    // 🔐 AUTHENTIFICATION
    // ============================================================================

    @Override
    public LoginResponseDTO authenticateCredentialsUser(String email, String phone, String password) {
        // Identifiant = email ou téléphone : on cherche d’abord par email, puis par téléphone
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        if (!hasEmail && !hasPhone) {
            throw new RuntimeException("Email ou téléphone requis");
        }

        // Récupérer l'utilisateur par email ou par téléphone (même logique qu'avec l'email)
        Users user = hasEmail
                ? getUserByEmail(email).orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"))
                : userRepository.findByPhone(phone).orElseThrow(() -> new RuntimeException("Téléphone ou mot de passe incorrect"));

        // Vérifier les mots de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException(hasEmail ? "Email ou mot de passe incorrect" : "Téléphone ou mot de passe incorrect");
        }

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            throw new RuntimeException("Votre compte n'est pas actif");
        }

        // Règle 3 : si salarié, vérifier que son entreprise est active
        if (user.getRole() == UserRole.EMPLOYEE) {
            Employee employee = employeeRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Salarié introuvable"));
            if (employee.getCompany() == null || !Boolean.TRUE.equals(employee.getCompany().getIsActive())) {
                throw new RuntimeException("Votre entreprise est inactive. Vous ne pouvez pas vous connecter.");
            }
        }

        // Si l'utilisateur est admin, déclencher l'OTP (on envoie sur l’email du compte)
        if (user.getRole() == UserRole.ADMINISTRATOR) {
            String otpCode = activationCodeService.generateAndStoreCode(user.getEmail());
            emailService.sendOtpCode(user.getEmail(), otpCode, user.getFirstName());
            return new LoginResponseDTO(user.getEmail(), true);
        }

        // Pour les autres rôles : connexion directe avec JWT
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return new LoginResponseDTO(
                accessToken,
                user.getEmail(),
                user.getRole().getLabel(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePhotoUrl()
        );
    }

    /**
     * Vérifie le code OTP et génère le token JWT pour un administrateur
     */
    @Override
    @Transactional
    public LoginResponseDTO verifyOtpAndGenerateToken(String email, String otp) {
        // Vérifier si l'utilisateur existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier que c'est un administrateur
        if (user.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Accès réservé aux administrateurs");
        }

        // Vérifier le code OTP
        boolean isValid = activationCodeService.verifyActivationCode(email, otp);

        if (!isValid) {
            throw new RuntimeException("Code OTP invalide ou expiré");
        }

        // Marquer le code OTP comme utilisé
        activationCodeService.markCodeAsUsed(email, otp);

        // Générer le token JWT
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        // Supprimer le code OTP utilisé (plus besoin de le garder)
        activationCodeRepository.deleteByEmail(email);

        // Retourner le token JWT avec les informations utilisateur
        return new LoginResponseDTO(
                accessToken,
                user.getEmail(),
                user.getRole().getLabel(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePhotoUrl()
        );
    }

    // ============================================================================
    // 🔐 ACTIVATION DE COMPTE
    // ============================================================================

    /**
     * Envoie un code d'activation par email à un utilisateur
     */
    @Override
    public void sendActivationCode(String email) {

        // Vérifier si l'utilisateur existe
        Users users = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Générer et stocker le code d'activation
        String code = activationCodeService.generateAndStoreCode(email);

        // Envoyer l'email avec le code
        emailService.sendActivationCode(email,code, users.getFirstName());

    }

    /**
     * Envoie un code d'activation par email pour le flux mobile (salarié/livreur).
     * Un code n'est envoyé que si le user n'a pas encore de mot de passe (première inscription).
     * Si un mot de passe existe déjà, on refuse pour éviter les abus du bouton.
     */
    @Override
    public void sendMobileActivationCode(RegisterMobileDTO requestDTO) {
        String email = requestDTO.getEmail();

        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            throw new RuntimeException("Vous êtes déjà inscrit. Connectez-vous avec votre mot de passe.");
        }

        if (user.getRole() == UserRole.EMPLOYEE) {
            Employee employee = employeeRepository.findByUserEmail(email)
                    .orElseThrow(() -> new RuntimeException("Employé introuvable pour cet email"));

            String code = activationCodeService.generateAndStoreCodeMobile(email);
            String commercialFullName = employee.getCreatedBy().getFirstName() + " " + employee.getCreatedBy().getLastName();

            emailService.sendEmployeeInvitation(
                    email,
                    code,
                    user.getFirstName(),
                    commercialFullName,
                    employee.getCompany().getName()
            );

            log.info("Code mobile envoyé au salarié {} ({})", user.getFirstName(), email);
            return;
        }
         //Cas livreur
        if (user.getRole() == UserRole.DELIVERY_DRIVER) {
            String code = activationCodeService.generateAndStoreCodeMobile(email);
            emailService.sendDriverActivationCode(email, code, user.getFirstName());
            log.info("Code mobile envoyé au livreur {} ({})", user.getFirstName(), email);
            return;
        }

        throw new RuntimeException("Ce rôle n'est pas éligible à l'activation mobile");
    }
    /**
     * Vérifie un code d'activation pour un utilisateur
     */
    @Override
    public void verifyActivationCode(String email, String code) {

        // Vérifier si l'utilisateur existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier le code d'activation
        boolean isValid = activationCodeService.verifyActivationCode(email, code);

        if(!isValid){
            throw new RuntimeException("Code d'activation invalide ou expiré");
        }
        // Sinon, Marquer le code comme utilisé
        activationCodeService.markCodeAsUsed(email, code);

    }

    /**
     * Crée le mot de passe et active le compte d'un utilisateur
     */
    @Override
    @Transactional
    public void setPassword(String email, String password, String confirmPassword) {

        // Vérifier si l'utilisateur existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier qu'un code d'activation a été vérifié
        if(!activationCodeService.hasUsedActivationCode(email)){
            throw new RuntimeException("Vous devez d'abord vérifier votre code d'activation");
        }

        // Vérifier que les mots de passe correspondent
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Hasher le mot de passe
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        // Activer le compte
        user.setIsActive(true);

        // Sauvegarder l'utilisateur
        userRepository.save(user);

        // Supprimer le code utilisé on en a plus besoin
        activationCodeRepository.deleteByEmail(email);

    }

    /**
     * Renvoie un code d'activation avec vérification du cooldown
     */
    @Override
    public void resendActivationCode(String email) {

        // Vérifier si l'utilisateur existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier le cooldown restant
        long remainingSeconds = activationCodeService.getRemainingCooldownSecond(email, CodeType.ACTIVATION);

        if (remainingSeconds > 0) {
            throw new RuntimeException("Veuillez attendre " + remainingSeconds + " secondes avant de renvoyer le code");
        }
        // Cooldown terminé, générer et envoyer un nouveau code
        if (user.getRole() == UserRole.EMPLOYEE) {
            Employee employee = employeeRepository.findByUserEmail(email)
                    .orElseThrow(() -> new RuntimeException("Employé introuvable pour cet email"));

            String code = activationCodeService.generateAndStoreCodeMobile(email);
            String commercialFullName = employee.getCreatedBy().getFirstName() + " " + employee.getCreatedBy().getLastName();

            emailService.sendEmployeeInvitation(
                    email,
                    code,
                    user.getFirstName(),
                    commercialFullName,
                    employee.getCompany().getName()
            );

            log.info("Code mobile envoyé au salarié {} ({})", user.getFirstName(), email);
            return;
        }
        //Cas livreur
        if (user.getRole() == UserRole.DELIVERY_DRIVER) {
            String code = activationCodeService.generateAndStoreCodeMobile(email);
            emailService.sendDriverActivationCode(email, code, user.getFirstName());
            log.info("Code mobile envoyé au livreur {} ({})", user.getFirstName(), email);
            return;
        }

        String code = activationCodeService.generateAndStoreCode(email);
        emailService.sendActivationCode(email, code, user.getFirstName());
    }

    // ============================================================================
    // 🔐 DÉCONNEXION
    // ============================================================================

    /**
     * Déconnecte un utilisateur en invalidant son token JWT
     */
    @Override
    @Transactional
    public void logout(String token) {
        // Vérifier que le token est valide avant de le blacklister
        if (token == null || token.isEmpty() || !jwtService.isTokenValid(token)) {
            throw new RuntimeException("Token invalide");
        }

        // Ajouter le token à la blacklist
        tokenBlacklistService.addToBlackList(token);

        log.info("Utilisateur déconnecté avec succès");
    }

    // ============================================================================
    // 🔑 RÉINITIALISATION DE MOT DE PASSE
    // ============================================================================

    /**
     * Démarre le processus de réinitialisation de mot de passe
     * Génère un token unique et envoie un email avec le lien
     */
    @Override
    @Transactional
    public void generatePasswordResetToken(String email, PasswordResetChannel channel) {

        // Vérifier si l'email existe (404 + message lisible pour l'utilisateur final)
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun compte n'est associé à cette adresse e-mail. Vérifiez l'orthographe ou inscrivez-vous si vous n'avez pas encore de compte."));

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            throw new RuntimeException(
                    "Votre compte n'est pas actif. La réinitialisation du mot de passe n'est pas disponible pour le moment. Contactez le support si vous avez besoin d'aide.");
        }

        // Supprimer les tokens existants pour cet email
        activationCodeRepository.deleteByEmailAndType(email, CodeType.PASSWORD_RESET);

        // Générer un token unique pour la réinitialisation
        String resetToken = UUID.randomUUID().toString();

        // Calculer la date d'expiration du token
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Créer et sauvegarder le token dans la base
        ActivationCode activationCode = new ActivationCode();
        activationCode.setType(CodeType.PASSWORD_RESET);
        activationCode.setUsed(false);
        activationCode.setExpiresAt(expiresAt);
        activationCode.setCode(resetToken);
        activationCode.setEmail(email);

        activationCodeRepository.save(activationCode);

        PasswordResetChannel resolvedChannel = channel != null ? channel : PasswordResetChannel.WEB;
        emailService.sendPasswordResetLink(email, resetToken, user.getFirstName(), resolvedChannel);
    }

    /**
     * Réinitialise le mot de passe avec le token fourni
     * Vérifie la validité du token et met à jour le mot de passe
     */
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {

        // Vérifier que les deux mots de passe sont identiques
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Récupérer le token depuis la base de données
        ActivationCode activationCode = activationCodeRepository.findByCodeAndTypeAndUsedFalse(token, CodeType.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        // Vérifier que le token n'est pas expiré
        if (activationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        // Récupérer l'utilisateur associé au token
        Users user = userRepository.findByEmail(activationCode.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marquer le token comme utilisé pour éviter la réutilisation
        activationCode.setUsed(true);
        activationCodeRepository.save(activationCode);

        log.info("Mot de passe réinitialisé pour: {}", user.getEmail());
    }

    // ============================================================================
    // 🔄 MAPPING DTO <-> ENTITY
    // ============================================================================

    /**
     * Convertit un DTO UserDto en entité Users
     *
     * @param userDto Le DTO à convertir
     * @return L'entité Users créée
     */
    private Users convertToEntity(UserDto userDto) {
        Users user = new Users();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhoneNumber());
        user.setRole(userDto.getRole());
        user.setCompanyCommercial(userDto.getCompanyCommercial());
        if (userDto.getProfilePhotoUrl() != null && !userDto.getProfilePhotoUrl().isBlank()) {
            user.setProfilePhotoUrl(userDto.getProfilePhotoUrl());
        }
        return user;
    }

    private Optional<Users> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDetailsDTO getCurrentUserProfile() {
        return toUserDetailsDto(getCurrentUser());
    }

    @Override
    @Transactional
    public ProfileUpdateResponseDTO updateMyProfile(UpdateMyProfileRequestDTO dto) {
        Users u = getCurrentUser();
        assertCommercialOrLogisticsManager(u);
        Long id = u.getId();

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            u.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            u.setLastName(dto.getLastName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ValidationException("L'email n'est pas valide");
            }
            if (Boolean.TRUE.equals(userRepository.existsByEmailAndIdNot(email, id))) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur");
            }
            u.setEmail(email);
        }
        if (dto.getPhoneNumber() != null) {
            String phone = dto.getPhoneNumber().trim();
            if (!phone.isEmpty()) {
                if (!PHONE_PATTERN.matcher(phone).matches()) {
                    throw new ValidationException("Le numéro de téléphone doit contenir entre 8 et 15 caractères");
                }
                if (Boolean.TRUE.equals(userRepository.existsByPhoneAndIdNot(phone, id))) {
                    throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre utilisateur");
                }
                u.setPhone(phone);
            }
        }

        userRepository.save(u);
        UserDetailsDTO profile = toUserDetailsDto(u);
        String accessToken = jwtService.generateToken(u.getEmail(), u.getRole().name(), u.getId());
        log.info("Profil mis à jour (commercial / RL) pour l'utilisateur id={}", u.getId());
        return new ProfileUpdateResponseDTO(profile, accessToken);
    }

    @Override
    @Transactional
    public void updateMyProfilePhoto(MultipartFile file) {
        Users u = getCurrentUser();
        assertCommercialOrLogisticsManager(u);
        adminService.updateProfilePhotoForCurrentUser(file);
    }

    @Override
    @Transactional
    public void removeMyProfilePhoto() {
        Users u = getCurrentUser();
        assertCommercialOrLogisticsManager(u);
        adminService.removeProfilePhotoForCurrentUser();
    }

    private static void assertCommercialOrLogisticsManager(Users u) {
        if (u.getRole() != UserRole.COMMERCIAL && u.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seuls le commercial et le responsable logistique peuvent modifier leur profil via cette API");
        }
    }

    private static UserDetailsDTO toUserDetailsDto(Users u) {
        UserDetailsDTO dto = new UserDetailsDTO();
        dto.setId(u.getId());
        dto.setRefUser(u.getRefUser());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setEmail(u.getEmail());
        dto.setPhoneNumber(u.getPhone());
        dto.setRole(u.getRole());
        dto.setRoleLabel(u.getRole() != null ? u.getRole().getLabel() : "");
        dto.setCompanyCommercial(u.getCompanyCommercial());
        dto.setIsActive(u.getIsActive());
        dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec email: " + userEmail));
    }
}