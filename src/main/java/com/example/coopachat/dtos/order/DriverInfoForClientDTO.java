package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Infos livreur exposées au client uniquement quand la commande est en cours de livraison (EN_COURS / ARRIVE).
 * Permet d'afficher nom, téléphone (bouton Appeler) et photo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfoForClientDTO {
    private String name;
    private String phone;
    /**
     * Satisfaction moyenne : somme des rating (1 à 5) ÷ nombre d'avis.
     * Null si le livreur n'a encore aucun avis (DriverAvis).
     */
    private Double satisfactionMoyenne;
    /** URL de la photo du livreur (null si non renseignée). */
    private String photo;
}
