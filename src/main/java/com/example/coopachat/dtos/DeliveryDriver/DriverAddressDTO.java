package com.example.coopachat.dtos.DeliveryDriver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Adresse du livreur : uniquement formattedAddress + lat/long (pas de mode ni isPrimary).
 * Le mobile remplit via Google Places puis envoie au back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverAddressDTO {

    @NotBlank(message = "L'adresse est obligatoire")
    private String formattedAddress;

    @NotNull(message = "La latitude est obligatoire")
    private BigDecimal latitude;

    @NotNull(message = "La longitude est obligatoire")
    private BigDecimal longitude;
}
