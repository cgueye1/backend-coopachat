package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.enums.SupplierType;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.CancelDeliveryTourDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.claim.ClaimDetailDTO;
import com.example.coopachat.dtos.claim.ClaimListResponseDTO;
import com.example.coopachat.dtos.claim.ClaimStatsDTO;
import com.example.coopachat.dtos.claim.RejectClaimDTO;
import com.example.coopachat.dtos.claim.ValidateClaimDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.CommandesParJourDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.RLDashboardKpisDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.TauxRetoursParJourDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatutTourneesDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.EligibleOrderDTO;
import com.example.coopachat.dtos.order.EligibleOrderLotDTO;
import com.example.coopachat.dtos.order.EmployeeOrderStatsDTO;
import com.example.coopachat.dtos.order.OrderEmployeeListResponseDTO;
import com.example.coopachat.dtos.order.OrderItemDetailsDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.enums.ClaimStatus;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.SupplierOrderStatus;
import org.springframework.core.io.ByteArrayResource;
import jakarta.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface pour le service de gestion des actions du Responsable Logistique
 */
public interface LogisticsManagerService {

    // ============================================================================
    // 🚚 GESTION DES FOURNISSEURS
    // ============================================================================

    /**
     * Récupère la liste des fournisseurs (id + nom) avec filtres
     *
     * @param categoryId ID de la catégorie pour filtrer (optionnel)
     * @param type Type de fournisseur pour filtrer (optionnel)
     * @return liste des fournisseurs actifs filtrés
     */
     List<SupplierListItemDTO> getAllSuppliers(Long categoryId, SupplierType type);

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
     * Récupère les statistiques pour la page Gestion des commandes : total commandes (hors ANNULEE), EN ATTENTE, EN RETARD, EN COURS, VALIDÉES, LIVRÉES ce mois.
     *
     * @return EmployeeOrderStatsDTO (totalCommandes, enAttente, enRetard, enCours, validees, livreesCeMois)
     */
    EmployeeOrderStatsDTO getEmployeeOrderStats();

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

    /**
     * Replanifier une commande en échec de livraison : passage en EN_ATTENTE, retrait de la tournée, notification salarié.
     * Réservé au RL. La commande doit être en statut ECHEC_LIVRAISON.
     */
    void replanOrder(Long orderId);

    /**
     * Annuler définitivement une commande après échec de livraison : ANNULEE, réintégration stock, notification salarié.
     * Réservé au RL. La commande doit être en statut ECHEC_LIVRAISON. Action irréversible.
     */
    void cancelOrderAfterFailure(Long orderId);

    // ============================================================================
   // 🚚 GESTION DES TOURNÉES DE LIVRAISON
   // ============================================================================

    /**
     * Récupère la liste des commandes éligibles pour une tournée (par date uniquement).
     * @param deliveryDate Date de livraison (obligatoire)
     * @return Liste des commandes disponibles
     */
    List<EligibleOrderDTO> getEligibleOrders(LocalDate deliveryDate);

    /**
     * Commandes éligibles groupées par proximité GPS (lots).
     * @param deliveryDate Date de livraison
     * @param lotSize Nombre max de commandes par lot
     * @return Liste de lots (chaque lot : zoneLabel, orderCount, orders)
     */
    List <EligibleOrderLotDTO> getGroupedEligibleOrders (LocalDate deliveryDate, int lotSize);

    /**
     * Nombre de commandes éligibles pour la planification à la date donnée (même règles que {@link #getGroupedEligibleOrders}).
     */
    long countEligibleOrdersForPlanning(LocalDate deliveryDate);

    /**
     * Chauffeurs actifs. Si {@code deliveryDate} est renseigné, exclut ceux qui ont déjà une tournée
     * ASSIGNEE ou EN_COURS à cette date ; {@code excludeTourId} permet d'ignorer une tournée (édition).
     */
    List<AvailableDriverDTO> getAvailableDrivers(LocalDate deliveryDate, Long excludeTourId);

    /**
     * Vue calendrier (mois) pour la planification : compte par jour des commandes en attente vs déjà planifiées,
     * plus {@code totalOverdueGlobal} (retards sur toutes les dates, pas limité au mois affiché).
     * @param year année (ex: 2026)
     * @param month mois (1-12)
     */
    com.example.coopachat.dtos.delivery.DeliveryPlanningCalendarResponseDTO getDeliveryPlanningCalendar(int year, int month);

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
     * Retire une commande d'une tournée (la commande redevient EN_ATTENTE sans tournée).
     * @param tourId ID de la tournée
     * @param orderId ID de la commande
     */
    void removeOrderFromTour(Long tourId, Long orderId);

    /**
     * Annule une tournée de livraison
     * @param tourId ID de la tournée
     * @param dto Motif d'annulation
     */
    void cancelDeliveryTour(Long tourId, CancelDeliveryTourDTO dto);

    /**
     * Exporte la liste des tournées en fichier Excel
     * @param tourNumber Filtre par numéro de tournée (optionnel)
     * @param status Filtre par statut de tournée (optionnel)
     * @return ByteArrayResource contenant le fichier Excel
     */
    ByteArrayResource exportDeliveryTours(String tourNumber, DeliveryTourStatus status);

    /**
     * Récupère les statistiques des tournées par statut
     * @return Statistiques des tournées
     */
    DeliveryTourStatsDTO getDeliveryTourStats();

    // ============================================================================
    // GESTION DES RÉCLAMATIONS
    // ============================================================================

    /**
     * Statistiques des retours (Total, Validés, Rejetés, Réintégrés, Montant remboursé).
     */
    ClaimStatsDTO getClaimStats();

    /**
     * Liste paginée des réclamations avec recherche et filtre par statut.
     *
     * @param page   numéro de page (0-based)
     * @param size   nombre d'éléments par page
     * @param search recherche par référence commande ou nom client (optionnel)
     * @param status filtre par statut (optionnel)
     * @return liste paginée des réclamations
     */
    ClaimListResponseDTO getClaims(int page, int size, String search, ClaimStatus status);

    /**
     * Export Excel de toutes les réclamations correspondant aux mêmes filtres que la liste (recherche, statut).
     */
    ByteArrayResource exportClaims(String search, ClaimStatus status);

    /**
     * Détails d'une réclamation par son id.
     *
     * @param id id de la réclamation
     * @return DTO avec les détails complets
     * @throws RuntimeException si la réclamation n'existe pas
     */
    ClaimDetailDTO getClaimById(Long id);

    /**
     * Valider une réclamation : réintégration au stock (quantité du produit remise en stock) ou remboursement (montant enregistré).
     *
     * @param id  id de la réclamation
     * @param dto type de décision (REINTEGRATION / REMBOURSEMENT) et montant si remboursement
     */
    void validateClaim(Long id, ValidateClaimDTO dto);

    /**
     * Rejeter une réclamation avec un motif.
     *
     * @param id  id de la réclamation
     * @param dto motif du rejet (obligatoire)
     */
    void rejectClaim(Long id, RejectClaimDTO dto);

    // ----------- Tableau de bord RL -----------

    /** KPIs : commandes en attente, en retard, tournées actives, livrées ce mois. */
    RLDashboardKpisDTO getDashboardKpis();

    /** Graphique "Statut tournées" : effectif par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE). */
    StatutTourneesDTO getStatutTournees();

    /** Graphique "Commandes par jour" : 7 derniers jours (date, nbCommandes). */
    List<CommandesParJourDTO> getCommandesParJour();

    /** Graphique "Taux de retours (%)" : 7 derniers jours (date, tauxPercent). Taux = réclamations créées ce jour / commandes ce jour × 100. */
    List<TauxRetoursParJourDTO> getTauxRetoursParJour();

    /**
     * Graphique tableau de bord RL (7 jours) : même sémantique que {@link #getLivraisonsParJour()}
     * (date prévue, livrées à la date, retard EN_ATTENTE).
     */
    List<LivraisonParJourDTO> getCommandesVsLivraisons();

    /** Donut "Stocks - État global" : normal, sous seuil, critique. */
    StockEtatGlobalDTO getStockEtatGlobal();

    /** Graphique « Livraisons 7 jours » : pour chaque jour (date, nbPrevues, nbLivreesALaDate, nbRetard). */
    List<LivraisonParJourDTO> getLivraisonsParJour();

    /**
     * Top 5 produits les plus commandés avec taux d'utilisation en % (pour le graphique "Produits les plus fréquents" - Gestion des commandes).
     * Même logique que le catalogue admin : 30 derniers jours, usagePercent = (quantité produit / total) × 100.
     */
    List<TopProductUsageDTO> getTop5ProductUsage();

}


