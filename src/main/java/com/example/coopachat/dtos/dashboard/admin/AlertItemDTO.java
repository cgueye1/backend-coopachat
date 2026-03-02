package com.example.coopachat.dtos.dashboard.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour les alertes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertItemDTO {

    /** Type d'alerte : WARNING (livraisons en retard), DANGER (stocks critiques). */
    private String type;

    /** Message court (ex. "3 livraison(s) en retard", "1 stock(s) en critique"). */
    private String message;

    /** Détail ou instruction (ex. "Cliquez pour ouvrir le module concerné"). */
    private String detail;

    /** Module cible pour le clic : LIVRAISONS ou STOCKS (redirection vers la page concernée). */
    private String module;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate date;
}
