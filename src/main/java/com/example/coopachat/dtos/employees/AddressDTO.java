package com.example.coopachat.dtos.employees;

import com.example.coopachat.enums.DeliveryMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer ou mettre à jour une adresse
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
}