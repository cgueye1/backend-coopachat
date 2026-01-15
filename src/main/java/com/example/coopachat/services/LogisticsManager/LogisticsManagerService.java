package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.CreateSupplierOrderDTO;
import com.example.coopachat.dtos.supplierOrders.SupplierOrderDetailsDTO;
import com.example.coopachat.dtos.supplierOrders.SupplierOrderListResponseDTO;
import com.example.coopachat.dtos.supplierOrders.UpdateSupplierOrderDTO;
import com.example.coopachat.enums.SupplierOrderStatus;

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

    /**
     * Récupère les détails complets d'une commande fournisseur
     *
     * @param id ID de la commande à récupérer
     * @return SupplierOrderDetailsDTO contenant toutes les informations de la commande et ses produits
     * @throws RuntimeException si la commande n'existe pas ou si une erreur survient
     */
    SupplierOrderDetailsDTO getSupplierOrderById(Long id);

    /**
     *  Récupère la liste paginée des commandes fournisseurs avec recherche et filtres
     *
     * @param page Numéro de la page (0-indexed)
     * @param size Taille de la page (nombre d'éléments par page)
     * @param search Terme de recherche (numéro de commande ou nom de produit) - optionnel
     * @param supplierId ID du fournisseur pour filtrer - optionnel
     * @param status Statut de la commande pour filtrer - optionnel
     * @return SupplierOrderListResponseDTO contenant la liste paginée des commandes et les métadonnées
     * @throws RuntimeException si une erreur survient
     */
    SupplierOrderListResponseDTO getAllSupplierOrders(int page, int size, String search, Long supplierId, SupplierOrderStatus status);
}


