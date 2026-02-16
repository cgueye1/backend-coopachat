package com.example.coopachat.dtos.employees;

import com.example.coopachat.enums.DeliveryMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour créer ou mettre à jour une adresse (avec coordonnées GPS pour livraison).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private DeliveryMode deliveryMode;
    private String city;
    private String district;
    private String street;
    private boolean primary;

    /** Adresse formatée complète (ex. autocomplete Google / Yango) */
    private String formattedAddress;
    /** Latitude (rempli par géocodage ou autocomplete) */
    private BigDecimal latitude;
    /** Longitude (rempli par géocodage ou autocomplete) */
    private BigDecimal longitude;
}