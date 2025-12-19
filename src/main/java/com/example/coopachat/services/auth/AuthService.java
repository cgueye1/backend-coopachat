package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;

/**
 * Interface pour le service d'authentification
 */
public interface AuthService {

    /**
     * Ajoute un nouvel utilisateur dans le système
     *
     * @param userDto Les informations de l'utilisateur à créer
     * @throws EmailAlreadyExistsException si l'email est déjà utilisé
     * @throws PhoneAlreadyExistsException si le téléphone est déjà utilisé
     */
    void addUser(UserDto userDto);

    /**
     * Authentifie un utilisateur avec email et mot de passe (étape 1)
     *
     * @param email L'email de l'utilisateur
     * @param password Le mot de passe
     * @return LoginResponseDTO avec tokens JWT si succès
     * @throws RuntimeException si l'authentification échoue
     */
    LoginResponseDTO authenticateCredentialsUser(String email, String password);

    /**
     * Authentifie un administrateur avec email et mot de passe, puis envoie un code OTP
     *
     * @param email L'email de l'administrateur
     * @param password Le mot de passe
     * @return LoginResponseDTO avec requiresOtp = true et message "Code OTP envoyé par email"
     * @throws RuntimeException si l'authentification échoue ou si l'utilisateur n'est pas administrateur
     */
    LoginResponseDTO authenticateAdminWithOtp(String email, String password);

    /**
     * Vérifie le code OTP et génère le token JWT pour un administrateur
     *
     * @param email L'email de l'administrateur
     * @param otp   Le code OTP à 6 chiffres
     * @return LoginResponseDTO avec accessToken JWT si succès
     * @throws RuntimeException si l'utilisateur n'existe pas, n'est pas administrateur, ou si le code OTP est invalide
     */
    LoginResponseDTO verifyOtpAndGenerateToken(String email, String otp);

    /**
     * Envoie un code d'activation par email à un utilisateur
     *
     * @param email L'email de l'utilisateur
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    void sendActivationCode(String email);

    /**
     * Vérifie un code d'activation pour un utilisateur
     *
     * @param email L'email de l'utilisateur
     * @param code  Le code d'activation à 6 chiffres
     * @throws RuntimeException si l'utilisateur n'existe pas ou si le code est invalide
     */
    void verifyActivationCode(String email , String code);

    /**
     * Crée le mot de passe et active le compte d'un utilisateur
     *
     * @param email L'email de l'utilisateur
     * @param password Le mot de passe à définir
     * @param confirmPassword  la confirmation
     * @throws RuntimeException si l'utilisateur n'existe pas, si le code n'a pas été vérifié, ou si les mots de passe ne correspondent pas
     */
    void setPassword(String email, String password, String confirmPassword);


}