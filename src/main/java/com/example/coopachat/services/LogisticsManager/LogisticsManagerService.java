package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.EligibleOrderDTO;
import com.example.coopachat.dtos.order.OrderEmployeeListResponseDTO;
import com.example.coopachat.dtos.order.OrderItemDetailsDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.SupplierOrderStatus;
import com.example.coopachat.enums.TimeSlot;
import org.springframework.core.io.ByteArrayResource;
import jakarta.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * Récupère la liste des fournisseurs (id + nom)
     *
     * @return liste des fournisseurs actifs
     */
     List<SupplierListItemDTO> getAllSuppliers();

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

    /**
     * Modifie le statut d'une commande fournisseur
     *
     * @param id ID de la commande à modifier
     * @param updateSupplierOrderStatusDTO Le nouveau statut à appliquer
     * @throws RuntimeException si la commande n'existe pas ou si une erreur survient
     */
    void updateSupplierOrderStatus(Long id, UpdateSupplierOrderStatusDTO updateSupplierOrderStatusDTO);

    /**
     * Récupère les statistiques des commandes fournisseurs
     *
     * @return SupplierOrderStatsDTO contenant total, pending, delivered, cancelled
     * @throws RuntimeException si une erreur survient
     */
     SupplierOrderStatsDTO getSupplierOrderStats();

    /**
     * Exporte la liste des commandes fournisseurs en fichier Excel selon les filtres
     * Le service retourne les données brutes (ByteArrayResource).
     * Le controller ajoute les headers HTTP (Content-Disposition, Content-Type) pour le téléchargement et retourne ResponseEntity<Resource> (la réponse complète).
     *
     * @param search Terme de recherche (référence ou nom produit)
     * @param supplierId ID du fournisseur pour filtrer
     * @param status Statut pour filtrer
     * @return ByteArrayResource contenant le fichier Excel
     * @throws RuntimeException si une erreur survient lors de la génération
     */
     ByteArrayResource exportSupplierOrders (String search ,Long supplierId, SupplierOrderStatus status );

    /**
     * Récupère la liste paginée du suivi des stocks avec recherche et filtres
     *
     * @param page Numéro de la page (0-indexed)
     * @param size Taille de la page
     * @param search Terme de recherche (référence ou produit)
     * @param categoryId ID de la catégorie pour filtrer
     * @param status Statut actif/inactif pour filtrer
     * @return ProductStockListResponseDTO contenant la liste paginée des stocks
     * @throws RuntimeException si une erreur survient
     */
    ProductStockListResponseDTO getStockList(int page, int size, String search, Long categoryId, Boolean status);

    /**
     * Exporte la liste du suivi des stocks en fichier Excel selon les filtres
     * Le service retourne les données brutes (ByteArrayResource).
     * Le controller ajoute les headers HTTP (Content-Disposition, Content-Type) pour le téléchargement et retourne ResponseEntity<Resource> (la réponse complète).
     *
     * @param search Terme de recherche (référence ou produit) - optionnel
     * @param categoryId ID de la catégorie pour filtrer - optionnel
     * @param status Statut actif/inactif pour filtrer - optionnel
     * @return ByteArrayResource contenant le fichier Excel
     * @throws RuntimeException si une erreur survient lors de la génération
     */
    ByteArrayResource exportStockList(String search, Long categoryId, Boolean status);

    /**
     * Augmente le stock d'un produit
     *
     * @param productId ID du produit
     * @param quantity Quantité à ajouter (doit être positive)
     * @throws RuntimeException si le produit n'existe pas ou si une erreur survient
     */
    void increaseStock(Long productId, Integer quantity);

    /**
     * Diminue le stock d'un produit
     *
     * @param productId ID du produit
     * @param quantity Quantité à retirer (doit être positive)
     * @throws RuntimeException si le produit n'existe pas, si le stock est insuffisant ou si une erreur survient
     */
    void decreaseStock(Long productId, Integer quantity);

    /**
     * Met à jour le seuil minimum de stock d'un produit
     *
     * @param productId ID du produit
     * @param minThreshold Nouveau seuil minimum (>= 0)
     * @throws RuntimeException si le produit n'existe pas ou si une erreur survient
     */
    void updateMinThreshold(Long productId, Integer minThreshold);

    /**
     * Met à jour le seuil minimum en appliquant un pourcentage sur le seuil actuel
     *
     * @param productId ID du produit
     * @param percent Pourcentage à appliquer (ex: 10 pour +10%)
     * @throws RuntimeException si le produit n'existe pas ou si une erreur survient
     */
    void updateMinThresholdByPercent(Long productId, Integer percent);

    /**
     * Récupère les statistiques du suivi des stocks
     *
     * @return StockStatsDTO contenant total, sous-seuil et ruptures
     * @throws RuntimeException si une erreur survient
     */
    StockStatsDTO getStockStats();

    /**
     * Récupère la liste paginée des produits en alerte de stock (stock < seuil)
     *
     * @param page Numéro de la page (0-indexed)
     * @param size Taille de la page
     * @param search Terme de recherche (référence ou produit) - optionnel
     * @param categoryId ID de la catégorie pour filtrer - optionnel
     * @return ProductStockListResponseDTO contenant la liste paginée des alertes
     * @throws RuntimeException si une erreur survient
     */
    ProductStockListResponseDTO getStockAlerts(int page, int size, String search, Long categoryId);

    /**
     * Exporte la liste des alertes de stock en fichier Excel
     * Le service retourne les données brutes (ByteArrayResource).
     * Le controller ajoute les headers HTTP (Content-Disposition, Content-Type) pour le téléchargement et retourne ResponseEntity<Resource> (la réponse complète).
     *
     * @param search Terme de recherche (référence ou produit) - optionnel
     * @param categoryId ID de la catégorie pour filtrer - optionnel
     * @return ByteArrayResource contenant le fichier Excel
     * @throws RuntimeException si une erreur survient lors de la génération
     */
    ByteArrayResource exportStockAlerts(String search, Long categoryId);

    // ============================================================================
    // 📦 GESTION DES COMMANDES SALARIÉS
    // ============================================================================
    /**
     * Récupère la liste paginée des commandes salariés avec recherche et filtres
     *
     * @param page Numéro de la page (0- par défaut)
     * @param size Taille de la page (nombre d'éléments par page)
     * @param search Terme de recherche (numéro de commande ou nom salarié) - optionnel
     * @param status Statut de la commande pour filtrer - optionnel
     * @return  OrderEmployeeListResponseDTO contenant la liste paginée des commandes et les métadonnées
     * @throws RuntimeException si une erreur survient
     */
     OrderEmployeeListResponseDTO getAllEmployeeOrders(int page, int size, String search, OrderStatus status);

    /**
     * Récupère les détails complets d'une commande d'un salarié
     *
     * @param id ID de la commande à récupérer
     * @return OrderItemDetailsDTO contenant toutes les informations de la commande et ses produits
     * @throws RuntimeException si la commande n'existe pas ou si une erreur survient
     */
     OrderItemDetailsDTO getOrderItemDetailById(Long id);


    /**
     * Exporte la liste des commandes salariés en fichier Excel
     *
     * @param search Terme de recherche (numéro commande ou nom salarié) - optionnel
     * @param status Statut de la commande pour filtrer - optionnel
     * @return ByteArrayResource contenant le fichier Excel
     * @throws RuntimeException si une erreur survient lors de la génération
     */
    ByteArrayResource exportEmployeeOrders(String search, OrderStatus status);

    // ============================================================================
   // 🚚 GESTION DES TOURNÉES DE LIVRAISON
   // ============================================================================

    /**
     * Récupère la liste des zones de livraison disponibles
     */
    List<ZoneOptionDTO> getAvailableZones();

    /**
     * Récupère la liste des commandes éligibles pour une tournée
     * @param deliveryDate Date de livraison (obligatoire)
     * @param timeSlot Créneau horaire (obligatoire)
     * @return Liste des commandes disponibles
     */
    List <EligibleOrderDTO> getEligibleOrders (LocalDate deliveryDate, TimeSlot timeSlot );


    /**
     * Récupère la liste des chauffeurs disponibles selon les filtres
     * @param deliveryDate Date de livraison
     * @param timeSlot Créneau horaire
     * @param deliveryZone Zone de livraison
     * @return Liste des chauffeurs disponibles
     */
    List<AvailableDriverDTO> getAvailableDrivers(LocalDate deliveryDate, TimeSlot timeSlot, String deliveryZone);

    /**
     * Crée une nouvelle tournée de livraison
     * @param dto Informations de la tournée à créer
     */
    void createDeliveryTour(CreateDeliveryTourDTO dto);

    /**
     * Récupère les détails d'une tournée de livraison
     * @param tourId ID de la tournée
     * @return DTO avec les détails de la tournée
     */
    DeliveryTourDetailsDTO getDeliveryTourDetails(Long tourId);

    /**
     * Récupère la liste paginée des tournées avec filtres
     * @param page Numéro de page (0-indexed)
     * @param size Taille de la page
     * @param tourNumber Filtre par numéro de tournée (optionnel)
     * @param status Filtre par statut de tournée (optionnel)
     * @return Réponse paginée avec la liste des tournées
     */
    DeliveryTourListResponseDTO getAllDeliveryTours(int page, int size, String tourNumber, DeliveryTourStatus status);

    /**
     * Met à jour une tournée de livraison
     * @param tourId ID de la tournée
     * @param dto Données de mise à jour
     */
    void updateDeliveryTour(Long tourId, UpdateDeliveryTourDTO dto);

    /**
     * Confirme une tournée de livraison
     * Change le statut de PLANIFIEE à CONFIRMEE
     * Envoie une notification au chauffeur
     *
     * @param tourId ID de la tournée à confirmer
     * @throws IllegalStateException si tournée n'est pas en statut PLANIFIEE
     * @throws ValidationException si tournée sans chauffeur ou sans commandes
     */
    void confirmDeliveryTour(Long tourId);



}


