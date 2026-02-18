package com.example.coopachat.dtos.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Corps de la requête POST "Noter le livreur" (commande livrée).
 * - rating : obligatoire, 1 à 5.
 * - tags : optionnel (ex. Ponctuel, Professionnel, Sympathique).
 * - comment : optionnel, max 500 caractères.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReviewDTO {

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "Note minimum : 1")
    @Max(value = 5, message = "Note maximum : 5")
    private Integer rating;

    private List<String> tags;

    @Size(max = 500, message = "Commentaire trop long (max 500 caractères)")
    private String comment;
}
