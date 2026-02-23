package com.example.coopachat.dtos.user;

import com.example.coopachat.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un élément du graphique "Utilisateurs par rôle" : rôle, libellé, effectif et part en %.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsByRoleItemDTO {

   //Rôle (ex. EMPLOYEE, COMMERCIAL). 
    private UserRole role;

    //Libellé affichable (ex. Salarié, Commercial). 
    private String roleLabel;

    //Nombre d'utilisateurs ayant ce rôle. 
    private long count;

    //Part en % du total (0–100). 
    private double percentage;
}
