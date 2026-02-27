package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO retourné par l'étape 2 (Livraison) : aperçu de la commande sans rien sauvegarder en base.
 * Utilisé par POST /orders/preview pour afficher l'écran de confirmation avant que le salarié ne valide.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewDTO {

    private Integer totalArticles;          // Nombre d'articles (ex. "1 articles")
    private String deliveryOption;          // Nom de l'option (ex. "Hebdomadaire")
    private LocalDate firstDeliveryDate;    // Date de première livraison estimée (ex. 06/09/2025)
    private String deliveryAddress;         // Adresse de livraison principale (ex. "Dakar, Point E")
    private BigDecimal total;               // Montant total après coupon si appliqué (ex. 2 591 F CFA)
}
