package com.example.coopachat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

//DTO pour la réponse de l'inscription d'un salarié par le commercial
public class RegisterEmployeeResponseDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String status; // "EN_ATTENTE_ACTIVATION"
    private String message;
}
