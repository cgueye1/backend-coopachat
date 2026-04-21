package com.example.coopachat.controllers;

import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserDto;
import com.example.coopachat.dtos.user.UpdateMyProfileRequestDTO;
import com.example.coopachat.dtos.auth.*;
import com.example.coopachat.enums.PasswordResetChannel;
import com.example.coopachat.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur pour la gestion de l'authentification
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "API pour la gestion de l'authentification et des utilisateurs")
public class AuthController {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final AuthService authService;

    // ============================================================================
    // 👤 GESTION DES UTILISATEURS
    // ============================================================================

    @Operation(
            summary = "Inscription publique (rôles restreints)",
            description = "Création de compte via l'API publique : les rôles Commercial et Responsable logistique ne sont pas autorisés (création par administrateur uniquement). " +
                         "L'email et le téléphone doivent être uniques."
    )
    @PostMapping("/users")
    public ResponseEntity<String> addUser(@RequestBody @Valid UserDto userDto) {
        authService.addUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur créé avec succès");
    }

    // ============================================================================
    // 🔐 AUTHENTIFICATION
    // ============================================================================

    @Operation(
            summary = "Connexion d'un utilisateur",
            description = "Permet à un utilisateur de se connecter avec son email et son mot de passe. " +
                         "Retourne un token JWT pour les requêtes authentifiées."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.authenticateCredentialsUser(
                loginRequest.getEmail(),
                loginRequest.getPhone(),
                loginRequest.getPassword()
        );
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Déconnexion d'un utilisateur",
            description = "Invalide le token JWT de l'utilisateur et le déconnecte du système."
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam("token") String token) {
        authService.logout(token);
        return ResponseEntity.ok("Déconnexion réussie");
    }

    @Operation(
            summary = "Profil de l'utilisateur connecté",
            description = "Retourne les informations personnelles de l'utilisateur actuellement connecté (tous rôles)."
    )
    @GetMapping("/me")
    public ResponseEntity<UserDetailsDTO> getCurrentUserProfile() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }

    @Operation(
            summary = "Modifier mon profil (commercial / responsable logistique)",
            description = "Met à jour prénom, nom, email et/ou téléphone pour l'utilisateur connecté. " +
                    "Champs absents ou vides : inchangés. Un nouveau JWT est renvoyé (à utiliser si l'email a changé). " +
                    "Email et téléphone doivent rester uniques."
    )
    @PutMapping("/me")
    public ResponseEntity<ProfileUpdateResponseDTO> updateMyProfile(@RequestBody UpdateMyProfileRequestDTO body) {
        return ResponseEntity.ok(authService.updateMyProfile(body));
    }

    @Operation(
            summary = "Modifier ma photo de profil (commercial / responsable logistique)",
            description = "Upload multipart, partie 'file' (JPEG, PNG, GIF, WebP, max 5 Mo)."
    )
    @PutMapping(value = "/me/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateMyProfilePhoto(@RequestParam("file") MultipartFile file) {
        authService.updateMyProfilePhoto(file);
        return ResponseEntity.ok("Photo de profil mise à jour");
    }

    @Operation(
            summary = "Supprimer ma photo de profil (commercial / responsable logistique)",
            description = "Supprime le fichier stocké et remet profilePhotoUrl à null."
    )
    @DeleteMapping("/me/profile-photo")
    public ResponseEntity<String> removeMyProfilePhoto() {
        authService.removeMyProfilePhoto();
        return ResponseEntity.ok("Photo de profil supprimée");
    }

    // ============================================================================
    // 🔐 ACTIVATION DE COMPTE
    // ============================================================================


    @Operation(
            summary = "Vérifier un code d'activation",
            description = "Vérifie le code d'activation de 6 et 4 chiffres reçu par email. " +
                    "Le code doit être valide et non expiré."
    )
    @PostMapping("/verify-activation-code")
    public ResponseEntity<String> verifyActivationCode(@RequestBody @Valid VerifyActivationCodeRequestDTO requestDTO) {
        authService.verifyActivationCode(requestDTO.getEmail(), requestDTO.getCode());
        return ResponseEntity.ok("Code d'activation vérifié avec succès");
    }

    @Operation(
            summary = "Créer un mot de passe",
            description = "Crée le mot de passe d'un utilisateur après vérification du code d'activation. " +
                    "Active automatiquement le compte."
    )
    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(@RequestBody @Valid SetPasswordRequestDTO requestDTO) {
        authService.setPassword(requestDTO.getEmail(), requestDTO.getPassword(),requestDTO.getConfirmPassword());
        return ResponseEntity.ok("Mot de passe créé avec succès. Votre compte est maintenant actif.");
    }


    @Operation(
            summary = "Vérifier le code OTP administrateur",
            description = "Vérifie le code OTP reçu par email et génère le token JWT pour finaliser la connexion administrateur."
    )
    @PostMapping("/admin/verify-otp")
    public ResponseEntity<LoginResponseDTO> verifyOtp(@RequestBody @Valid VerifyActivationCodeRequestDTO requestDTO) {
        LoginResponseDTO response = authService.verifyOtpAndGenerateToken(
                requestDTO.getEmail(),
                requestDTO.getCode()
        );
        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // 🔑 RÉINITIALISATION DE MOT DE PASSE
    // ============================================================================

    @Operation(
            summary = "Demander la réinitialisation de mot de passe",
            description = "Génère un token de réinitialisation et l'envoie par email à l'utilisateur. " +
                    "Le token expire dans 15 minutes. " +
                    "Champ optionnel channel : WEB (défaut, lien navigateur) ou MOBILE (deep link application)."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO requestDTO) {
        PasswordResetChannel channel = requestDTO.getChannel() != null
                ? requestDTO.getChannel()
                : PasswordResetChannel.WEB;
        authService.generatePasswordResetToken(requestDTO.getEmail(), channel);
        return ResponseEntity.ok("Un lien de réinitialisation a été envoyé à votre adresse email");
    }

    @Operation(
            summary = "Réinitialiser le mot de passe",
            description = "Réinitialise le mot de passe d'un utilisateur avec un token valide reçu par email. " +
                    "Le token doit être valide et non expiré."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO requestDTO) {
        authService.resetPassword(
                requestDTO.getToken(),
                requestDTO.getNewPassword(),
                requestDTO.getConfirmPassword()
        );
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès");
    }
}
