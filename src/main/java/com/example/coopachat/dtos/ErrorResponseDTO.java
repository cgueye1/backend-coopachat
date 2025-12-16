package com.example.coopachat.dtos;

import java.time.LocalDateTime;
import java.util.List;

//DTO pour une réponse d'erreur
public class ErrorResponseDTO {

    private String message;
    private List<String> errors;
    private LocalDateTime timestamp;
    private Integer status;
}