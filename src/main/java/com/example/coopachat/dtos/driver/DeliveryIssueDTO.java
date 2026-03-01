package com.example.coopachat.dtos.driver;

import com.example.coopachat.enums.DeliveryIssueReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour signaler un problème de livraison (livreur).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryIssueDTO {

    /**
     * Raison du problème (enum : Client absent, Adresse introuvable, etc.).
     */
    @NotNull(message = "La raison est obligatoire")
    private DeliveryIssueReason reason;

    /**
     * Commentaire optionnel du livreur.
     */
    private String comment;

    /**
     * URLs des photos (optionnel).
     */
    private List<String> photoUrls;
}
