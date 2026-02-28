package com.example.coopachat.dtos.employees;

import com.example.coopachat.enums.DeliveryMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour créer ou mettre à jour une adresse.
 * Type (Domicile/Bureau/Autre) + adresse formatée Google + lat/long.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    /** Id de l'adresse **/
    private Long id;

    @NotNull(message = "Le type d'adresse est obligatoire")
    private DeliveryMode deliveryMode;

    @NotBlank(message = "L'adresse est obligatoire")
    private String formattedAddress;

    @NotNull(message = "La latitude est obligatoire")
    private BigDecimal latitude;

    @NotNull(message = "La longitude est obligatoire")
    private BigDecimal longitude;

    private boolean primary = false;
}