package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.CreateSupplierOrderDTO;
import com.example.coopachat.dtos.supplierOrders.UpdateSupplierOrderDTO;

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

    /**
     * Modifie une commande fournisseur existante
     * Seules les commandes avec le statut "En attente" peuvent être modifiées
     *
     * @param id ID de la commande à modifier
     * @param updateSupplierOrderDTO  Les informations à mettre à jour (tous les champs sont optionnels)
     * @throws RuntimeException si la commande n'existe pas, si le statut ne permet pas la modification, si un produit n'existe pas ou si une erreur survient
     */
    void updateSupplierOrder (Long id , UpdateSupplierOrderDTO updateSupplierOrderDTO);
}


