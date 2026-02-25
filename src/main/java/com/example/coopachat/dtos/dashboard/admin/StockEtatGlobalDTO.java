package com.example.coopachat.dtos.dashboard.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le donut "Stocks - État global" du dashboard admin.
 * Nombre de produits dans chaque état : Normal (stock > seuil), Sous seuil, Critique (rupture).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockEtatGlobalDTO {

    /** Produits avec stock strictement au-dessus du seuil minimum. */
    private long normal;

    /** Produits sous le seuil (stock > 0 et stock < seuil min). */
    private long sousSeuil;

    /** Produits en rupture (stock = 0). */
    private long critique;
}
