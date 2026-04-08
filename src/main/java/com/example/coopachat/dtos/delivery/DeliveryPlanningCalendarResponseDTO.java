package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse calendrier planification : jours du mois + total des retards (toutes dates, pas seulement le mois affiché).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPlanningCalendarResponseDTO {
    private List<DeliveryPlanningCalendarDayDTO> days;
    /**
     * Nombre total de commandes EN_ATTENTE, non planifiées, date de livraison prévue &lt; aujourd'hui,
     * employé actif — même périmètre que la planification (hors filtre « mois du calendrier »).
     */
    private long totalOverdueGlobal;
}
