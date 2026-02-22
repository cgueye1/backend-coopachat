package com.example.coopachat.dtos.claim;

import com.example.coopachat.enums.ClaimProblemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour l'ajout d'une réclamation (soumission par le salarié).
 */
@Data
@NoArgsConstructor
public class CreateClaimDTO {

    //ID du produit concerné (UN SEUL)
    @NotNull(message = "Le produit concerné est obligatoire")
    private Long orderItemId;

    //Nature du problème
    @NotNull(message = "La nature du problème est obligatoire")
    private ClaimProblemType problemType;

    // Commentaire explicatif
    private String comment;

    // Photos (optionnel)
    private List<String> photoUrls;
}
