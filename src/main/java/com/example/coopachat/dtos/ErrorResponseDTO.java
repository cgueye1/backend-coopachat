package com.example.coopachat.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

//DTO pour une réponse d'erreur
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {

    private String message;
    private List<String> errors;
    private LocalDateTime timestamp;
    private Integer status;
}