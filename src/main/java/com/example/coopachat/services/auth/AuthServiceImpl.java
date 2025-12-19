package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.repositories.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implémentation du service d'authentification
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;

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

        if(userDto.getRole() == UserRole.COMMERCIAL){
          if(userDto.getCompanyCommercial() == null || userDto.getCompanyCommercial().isEmpty()){
              throw new ValidationException("L'entreprise est obligatoire pour un commercial");
          }
        }

        // Mapping : convertir le DTO en entité
        Users user = convertToEntity(userDto);
        

        // Sauvegarde : enregistrer l'utilisateur en base de données
        userRepository.save(user);
    }

    @Override
    public LoginResponseDTO authenticateCredentialsUser(String email, String password) {

        // Vérifier si l'utilisateur existe dans la base de données
        Users user = getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier les mots de passe
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Vérifier si le compte est actif
        if (!user.getIsActive()){
            throw new RuntimeException("Votre compte n'est pas actif");
        }

        //Générer le token
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId() );

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