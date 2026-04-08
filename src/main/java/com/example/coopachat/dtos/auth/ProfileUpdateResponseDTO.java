package com.example.coopachat.dtos.auth;

import com.example.coopachat.dtos.user.UserDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse après mise à jour du profil : données à jour + nouveau JWT (le sujet du token suit l'email).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateResponseDTO {

    private UserDetailsDTO profile;

    /** Nouveau token à utiliser dans Authorization: Bearer … (notamment si l'email a changé). */
    private String accessToken;
}
