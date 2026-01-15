package com.example.coopachat.repositories;

import com.example.coopachat.entities.SupplierOrder;
import com.example.coopachat.enums.SupplierOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * Repository pour l'entité CommandeFournisseur
 */
@Repository
public interface SupplierOrderRepository extends JpaRepository <SupplierOrder, Long>{

   /**
    * Vérifie si le numéro de cmd existe déjà
    */
    boolean existsByOrderNumber(String orderNumber);

   /**
    * Compte les commandes par statut
    *
    * @param supplierOrderStatus le statut de la commande
    * @return la valeur
    */
    long countByStatus(SupplierOrderStatus supplierOrderStatus);

     // ============================================================================
     // 🔍 MÉTHODES DE RECHERCHE ET FILTRES
     // ============================================================================

    /**
     * Récupère les commandes avec recherche par numéro de commande (pagination)
     */
    Page<SupplierOrder> findByOrderNumberContainingIgnoreCase(String orderNumber, Pageable pageable);

    // Requête pour rechercher les commandes (SupplierOrder) dont le numéro de commande
    // ou le nom d'un produit associé correspond au terme de recherche (:searchTerm).
    // - `items` = liste de tous les éléments (produits) d'une commande
    // - `item` = un élément (Item) de cette liste, parcouru individuellement par la jointure
    // - `LEFT JOIN` permet de garder les commandes même si elles n'ont aucun item
    // - `DISTINCT` évite les doublons si plusieurs so (surtout les numéros de cmd) correspondent au terme de recherche
    // - `LOWER()` convertit les chaînes en minuscules pour rendre la recherche insensible à la casse
    @Query("SELECT DISTINCT so FROM SupplierOrder so " +
            "LEFT JOIN so.items item " +
            "LEFT JOIN item.product p " +
            "WHERE LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<SupplierOrder> findByOrderNumberOrProductName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Récupère les commandes avec filtre par fournisseur (pagination)
     */
    Page<SupplierOrder> findBySupplierId(Long supplierId, Pageable pageable);
    /**
     * Récupère les commandes avec filtre par statut (pagination)
     */
    Page<SupplierOrder> findByStatus(SupplierOrderStatus status, Pageable pageable);

    /**
     * Récupère les commandes avec recherche et filtre par fournisseur
     *  Nécessite @Query car recherche par produit
     */
    @Query("SELECT DISTINCT so FROM SupplierOrder so " +
            "LEFT JOIN so.items item " +
            "LEFT JOIN item.product p " +
            "WHERE so.supplier.id = :supplierId " +
            "AND (LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<SupplierOrder> findByOrderNumberOrProductNameAndSupplierId(
            @Param("searchTerm") String searchTerm,
            @Param("supplierId") Long supplierId,
            Pageable pageable);


    /**
     * Récupère les commandes avec recherche et filtre par statut
     *  Nécessite @Query car recherche par produit
     */
    @Query("SELECT DISTINCT so FROM SupplierOrder so " +
            "LEFT JOIN so.items item " +
            "LEFT JOIN item.product p " +
            "WHERE so.status = :status " +
            "AND (LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<SupplierOrder> findByOrderNumberOrProductNameAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("status") SupplierOrderStatus status,
            Pageable pageable);

    /**
     * Récupère les commandes avec filtre par fournisseur et statut (pagination)
     */
    Page<SupplierOrder> findBySupplierIdAndStatus(Long supplierId, SupplierOrderStatus status, Pageable pageable);

    /**
     * Récupère les commandes avec recherche, filtre par fournisseur et statut
     * Nécessite @Query car recherche par produit
     */
    @Query("SELECT DISTINCT so FROM SupplierOrder so " +
            "LEFT JOIN so.items item " +
            "LEFT JOIN item.product p " +
            "WHERE so.supplier.id = :supplierId " +
            "AND so.status = :status " +
            "AND (LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<SupplierOrder> findByOrderNumberOrProductNameAndSupplierIdAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("supplierId") Long supplierId,
            @Param("status") SupplierOrderStatus status,
            Pageable pageable);


}