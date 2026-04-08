package com.example.coopachat.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mise à jour du profil par l'utilisateur connecté (Commercial ou Responsable logistique).
 * Seuls les champs non null et non vides sont appliqués.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMyProfileRequestDTO {

    private String firstName;
    private String lastName;

    /** Validé côté service lorsque renseigné (format email). */
    private String email;

    /** Validé côté service lorsque renseigné (format téléphone). */
    private String phoneNumber;
}
