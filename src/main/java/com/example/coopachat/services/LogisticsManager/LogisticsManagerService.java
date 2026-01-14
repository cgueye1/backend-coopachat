package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.CreateSupplierOrderDTO;

/**
 * Interface pour le service de gestion des actions du Responsable Logistique
 */
public interface LogisticsManagerService {

    // ============================================================================
    // 🚚 GESTION DES LIVREURS
    // ============================================================================

    /**
     * Crée un nouveau livreur et envoie une invitation par email
     *
     * @param driverDTO Les informations du livreur à créer
     * @throws RuntimeException si l'email ou le téléphone existe déjà ou si une erreur survient
     */
    void createDriver(RegisterDriverRequestDTO driverDTO);

    // ============================================================================
    // 📦 GESTION DES COMMANDES FOURNISSEURS
    // ============================================================================

    /**
     *  Crée une nouvelle commande fournisseur avec un ou plusieurs produits
     *
     * @param createSupplierOrderDTO Les informations de la commande à créer (fournisseur, liste de produits, date prévue, notes)
     * @throws RuntimeException si le fournisseur n'existe pas, si un produit n'existe pas ou si une erreur survient
     */
    void createSupplierOrder (CreateSupplierOrderDTO createSupplierOrderDTO);
}


