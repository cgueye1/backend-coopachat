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
@AllArgsConstructor
public class CreateClaimDTO {

    /** IDs des articles de la commande concernés (vide = réclamation sur toute la commande). */
    private List<Long> orderItemIds = new ArrayList<>();

    /** Nature du problème. */
    @NotNull(message = "La nature du problème est obligatoire")
    private ClaimProblemType problemType;

    /** Commentaire de la réclamation (optionnel). */
    @Size(max = 2000, message = "Commentaire trop long (max 2000 caractères)")
    private String comment;
}
