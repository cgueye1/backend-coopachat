package com.example.coopachat.dtos.driver;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour signaler un problème de livraison (livreur).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryIssueDTO {

    /**
     * ID de la raison (référentiel admin : delivery-issue-reasons).
     */
    @NotNull(message = "La raison est obligatoire")
    private Long reasonId;

    /**
     * Commentaire optionnel du livreur.
     */
    private String comment;
}
