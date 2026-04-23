package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserDto;
import com.example.coopachat.dtos.user.UpdateMyProfileRequestDTO;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.dtos.auth.ProfileUpdateResponseDTO;
import org.springframework.web.multipart.MultipartFile;
import com.example.coopachat.dtos.auth.RegisterMobileDTO;
import com.example.coopachat.enums.PasswordResetChannel;
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
     * @param email L'email de l'utilisateur (optionnel si phone renseigné)
     * @param phone Le téléphone de l'utilisateur (optionnel si email renseigné)
     * @param password Le mot de passe
     * @return LoginResponseDTO avec tokens JWT si succès
     * @throws RuntimeException si l'authentification échoue
     */
    LoginResponseDTO authenticateCredentialsUser(String email, String phone, String password);


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
     * Crée le mot de passe et active le compte d'un utilisateur en utilisant le token fourni dans l'URL
     *
     * @param email L'email de l'utilisateur
     * @param token Le token d'activation
     * @param password Le mot de passe à définir
     * @param confirmPassword  la confirmation
     * @throws RuntimeException si l'utilisateur n'existe pas, si le code n'est pas valide, ou si les mots de passe ne correspondent pas
     */
    void setPassword(String email, String token, String password, String confirmPassword);

    /**
     * Renvoie un lien d'activation à un utilisateur dont le compte n'est pas encore actif.
     * Le type de lien (web ou mobile) dépend automatiquement du rôle de l'utilisateur.
     *
     * @param email L'email de l'utilisateur
     * @throws RuntimeException si l'utilisateur n'existe pas ou si le compte est déjà actif
     */
    void resendActivationLink(String email);

    /**
     * Génère un token de réinitialisation de mot de passe et l'envoie par email
     *
     * @param email L'email de l'utilisateur qui demande la réinitialisation
     * @param channel Canal du lien dans l'email (web ou mobile)
     * @throws RuntimeException si l'utilisateur n'existe pas ou si le compte n'est pas actif
     */
    void generatePasswordResetToken(String email, PasswordResetChannel channel);


    /**
     * Réinitialise le mot de passe d'un utilisateur avec un token valide
     *
     * @param token Le token de réinitialisation reçu par email
     * @param newPassword Le nouveau mot de passe à définir
     * @param confirmPassword La confirmation du nouveau mot de passe
     * @throws RuntimeException si le token est invalide, expiré, si les mots de passe ne correspondent pas, ou si l'utilisateur n'existe pas
     */
    void resetPassword(String token, String newPassword, String confirmPassword);



    /**
     * Déconnecte un utilisateur en invalidant son token JWT
     *
     * @param token Le token JWT à invalider
     * @throws RuntimeException si le token est invalide
     */
    void logout (String token);

    /**
     * Retourne le profil de l'utilisateur connecté (tous rôles).
     *
     * @return UserDetailsDTO avec les infos de l'utilisateur connecté
     * @throws RuntimeException si non authentifié
     */
    UserDetailsDTO getCurrentUserProfile();

    /**
     * Met à jour le profil de l'utilisateur connecté (prénom, nom, email, téléphone).
     * Réservé au commercial et au responsable logistique. Retourne un nouveau JWT.
     */
    ProfileUpdateResponseDTO updateMyProfile(UpdateMyProfileRequestDTO dto);

    /**
     * Met à jour la photo de profil de l'utilisateur connecté.
     * Réservé au commercial et au responsable logistique (mêmes règles que pour les autres rôles via AdminService).
     */
    void updateMyProfilePhoto(MultipartFile file);

    /**
     * Supprime la photo de profil de l'utilisateur connecté (commercial / responsable logistique).
     */
    void removeMyProfilePhoto();

}