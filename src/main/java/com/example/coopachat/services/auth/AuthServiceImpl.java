package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.entities.Users;
import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.repositories.ActivationCodeRepository;
import com.example.coopachat.repositories.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation du service d'authentification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

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

    // ============================================================================
    // 👤 GESTION DES UTILISATEURS
    // ============================================================================

    /**
     * Ajoute un nouvel utilisateur dans le système
     */
    @Override
    public void addUser(UserDto userDto) {

        // Validation : vérifier si l'email existe déjà
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        // Validation : vérifier si le téléphone existe déjà
        if (userRepository.existsByPhone(userDto.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException("Ce téléphone est déjà utilisé");
        }

        if (userDto.getRole() == UserRole.COMMERCIAL) {
            if (userDto.getCompanyCommercial() == null || userDto.getCompanyCommercial().isEmpty()) {
                throw new ValidationException("L'entreprise est obligatoire pour un commercial");
            }
        }

        // Mapping : convertir le DTO en entité
        Users user = convertToEntity(userDto);

        // Sauvegarde : enregistrer l'utilisateur en base de données
        userRepository.save(user);
    }

    // ============================================================================
    // 🔐 AUTHENTIFICATION
    // ============================================================================

    @Override
    public LoginResponseDTO authenticateCredentialsUser(String email, String password) {

        // Vérifier si l'utilisateur existe dans la base de données
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier les mots de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            throw new RuntimeException("Votre compte n'est pas actif");
        }

        // Générer le token
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return new LoginResponseDTO(accessToken, user.getEmail(), user.getRole().getLabel(), user.getId());
    }

    @Override
    public LoginResponseDTO authenticateAdminWithOtp(String email, String password) {

        // Vérifier si l'utilisateur existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier que c'est un administrateur
        if (user.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Accès réservé aux administrateurs");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            throw new RuntimeException("Votre compte n'est pas actif");
        }

        // Générer et stocker le code OTP
        String otpCode = activationCodeService.generateAndStoreCode(email);

        // Envoyer le code OTP par email
        emailService.sendOtpCode(email, otpCode, user.getFirstName());

        // Retourner la réponse avec requiresOtp = true
        return new LoginResponseDTO(email, true);
    }

    /**
     * Vérifie le code OTP et génère le token JWT pour un administrateur
     */
    @Override
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
        return new LoginResponseDTO(accessToken, user.getEmail(), user.getRole().getLabel(), user.getId());
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
    public void generatePasswordResetToken(String email) {

        // Vérifier si l'email existe
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            throw new RuntimeException("Votre compte n'est pas actif");
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

        // Envoyer l'email avec le lien de réinitialisation
        emailService.sendPasswordResetLink(email, resetToken, user.getFirstName());
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
        return user;
    }

    private Optional<Users> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}