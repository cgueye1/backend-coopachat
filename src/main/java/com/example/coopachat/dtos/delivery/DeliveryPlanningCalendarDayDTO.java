package com.example.coopachat.dtos.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un jour du calendrier de planification (vue globale RL).
 * date : dd/MM/yyyy
 * pendingOrders : commandes EN_ATTENTE (non planifiées)
 * plannedOrders : commandes déjà planifiées (dans une tournée non annulée)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPlanningCalendarDayDTO {
    private String date;
    private long pendingOrders;
    private long plannedOrders;
}

