package com.example.coopachat.controllers.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.*;
import com.example.coopachat.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            summary = "Inscription d'un utilisateur",
            description = "Permet d'inscrire un nouvel utilisateur dans le système, notamment les commerciaux et les responsables logistiques. " +
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
                loginRequest.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Connexion via Google",
            description = "Permet à un utilisateur de se connecter avec un compte Google (OAuth). " +
                    "Retourne un token JWT ou requiresOtp si admin."
    )
    @PostMapping("/login/google")
    public ResponseEntity<LoginResponseDTO> loginWithGoogle(@RequestBody @Valid GoogleLoginRequestDTO requestDTO) {
        LoginResponseDTO response = authService.authenticateWithGoogle(requestDTO.getIdToken());
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

    // ============================================================================
    // 🔐 ACTIVATION DE COMPTE
    // ============================================================================
    @Operation(
            summary = "Envoyer un code d'activation",
            description = "Envoie un code d'activation de 6 chiffres par email à un utilisateur. " +
                    "Le code expire dans 15 minutes."
    )
    @PostMapping("/send-activation-code")
    public ResponseEntity<String> sendActivationCode(@RequestBody @Valid SendActivationCodeRequestDTO requestDTO) {
        authService.sendActivationCode(requestDTO.getEmail());
        return ResponseEntity.ok("Code d'activation envoyé avec succès par email");
    }

    @Operation(
            summary = "Envoyer un code d'activation (mobile)",
            description = "Envoie un code d'activation pour le flux mobile (salarié/livreur)."
    )
    @PostMapping("/mobile/send-activation-code")
    public ResponseEntity<String> sendMobileActivationCode(@RequestBody @Valid RegisterMobileDTO requestDTO) {
        authService.sendMobileActivationCode(requestDTO);
        return ResponseEntity.ok("Code d'activation envoyé avec succès par email");
    }

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
            summary = "Renvoyer un code d'activation",
            description = "Renvoie un code d'activation par email. " +
                    "Un délai de 30 secondes doit être respecté entre chaque envoi."
    )
    @PostMapping("/resend-activation-code")
    public ResponseEntity<String> resendActivationCode(@RequestBody @Valid SendActivationCodeRequestDTO requestDTO) {
        authService.resendActivationCode(requestDTO.getEmail());
        return ResponseEntity.ok("Code d'activation renvoyé avec succès");
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
                    "Le token expire dans 15 minutes."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO requestDTO) {
        authService.generatePasswordResetToken(requestDTO.getEmail());
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
