package com.example.coopachat.dtos.dashboard.logisticsManager;

import com.example.coopachat.enums.DeliveryTourStatus;

/**
 * Projection pour le comptage par statut (tournées).
 * Utilisé par DeliveryTourRepository.countGroupByStatus().
 * <p>
 * Un record, c’est une petite classe qui ne fait que porter des données :
 * on a deux infos (status, count), et on peut les lire avec status() et count().
 */
public record StatusCountDTO(DeliveryTourStatus status, Long count) {}
