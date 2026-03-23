package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


// DTO pour l'affichage et la modification des infos personnelles
@AllArgsConstructor
@Getter
public class DriverPersonalInfoDTO {

    @Setter
    private String firstName;      // Modifiable
    @Setter
    private String lastName;       // Modifiable
    @Setter
    private String phone;          // Modifiable

    private String email;          // Lecture seule (affiché mais ignoré à l'update)

    private String profilUrl;  //photo du livreur
}
