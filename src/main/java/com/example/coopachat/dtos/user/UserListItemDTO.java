package com.example.coopachat.dtos.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Élément de la liste des utilisateurs (admin). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListItemDTO {

    private Long id;
    private String reference;      // ex. US-2025-05
    private String firstName;
    private String lastName;
    private String email;
    private String roleLabel;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDateTime createdAt;

    private Boolean isActive;      // true = Actif, false = Inactif
}
