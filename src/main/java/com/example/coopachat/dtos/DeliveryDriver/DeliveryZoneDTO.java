package com.example.coopachat.dtos.DeliveryDriver;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZoneDTO {

    private Long id;

    @NotEmpty(message = "Veuillez sélectionner au moins une zone")
    private Set<Long> zoneIds; // ← IDs des zones de référence
}