package com.example.coopachat.dtos.reference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple pour une entité de type "nom + description" (types de réclamation, raisons livraison, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceItemDTO {

    private Long id;
    private String name;
    private String description;
}
