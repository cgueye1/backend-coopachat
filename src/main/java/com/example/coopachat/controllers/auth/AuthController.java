package com.example.coopachat.controllers.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.LoginRequestDTO;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.services.auth.AuthService;
import com.example.coopachat.services.auth.JwtServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
