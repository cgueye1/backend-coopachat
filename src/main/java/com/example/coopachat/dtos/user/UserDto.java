package com.example.coopachat.dtos.user;

import com.example.coopachat.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//DTO pour l'objet utilisateur
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private String companyCommercial;  // Optionnel, seulement pour Commercial

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean isActive;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /** Nom du fichier photo de profil (ex. uuid.jpg). URL d'affichage : /api/files/{profilePhotoUrl}. */
    private String profilePhotoUrl;
}