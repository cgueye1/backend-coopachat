package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO de réponse pour le frontend avec les détails des zones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryZoneResponseDTO {

    private Long id;

    // Pour l'affichage dans l'app mobile : liste des zones de livraison du livreur
    private Set<ZoneDetail> zones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ZoneDetail {
        private Long id;
        private String zoneName;
        private String description;
        private Boolean active;
    }
}