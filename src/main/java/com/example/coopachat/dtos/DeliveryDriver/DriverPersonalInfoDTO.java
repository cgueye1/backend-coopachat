package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;

//DTO pour retourner les infos personnelles du livreur
@AllArgsConstructor
public class DriverPersonalInfoDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
}
