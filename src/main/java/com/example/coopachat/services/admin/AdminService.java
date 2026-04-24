package com.example.coopachat.services.admin;

import com.example.coopachat.enums.SupplierType;

import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.fee.CreateFeeDTO;
import com.example.coopachat.dtos.fee.FeeDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.categories.CategoryKpiDTO;
import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.categories.UpdateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.user.SaveUserDTO;
import com.example.coopachat.dtos.user.UpdateUserStatusDTO;
import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserListResponseDTO;
import com.example.coopachat.dtos.user.UserStatsByRoleItemDTO;
import com.example.coopachat.dtos.user.UserStatsByStatusItemDTO;
import com.example.coopachat.dtos.user.UserStatsDTO;
import com.example.coopachat.dtos.suppliers.*;
import com.example.coopachat.dtos.dashboard.admin.AdminAlertsDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminDashboardStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatutTourneesDTO;
import com.example.coopachat.dtos.reference.CreateReferenceItemDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.enums.UserRole;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface pour le service de gestion des actions de l'administrateur
 */
public interface AdminService {

    /**
     * Crée une nouvelle catégorie
     *
     * @param createCategoryDTO Les informations de la catégorie à créer
     * @throws RuntimeException si le nom de la catégorie existe déjà ou si une erreur survient
     */
    void createCategory(CreateCategoryDTO createCategoryDTO);

    /**
     * Récupère la liste des catégories (id + nom + icon).
     */
    List<CategoryListItemDTO> getAllCategories(String search);

    /**
     * KPI de la page catégories (total catégories, total produits, produits actifs).
     */
    CategoryKpiDTO getCategoryKpis();

    /**
     * Récupère une catégorie par son ID (id + nom + icon).
     */
    CategoryListItemDTO getCategoryById(Long id);

    /**
     * Met à jour une catégorie : seuls les champs non null du DTO sont modifiés.
     */
    void updateCategory(Long id, UpdateCategoryDTO dto);

    /**
     * Supprime une catégorie ainsi que tous les produits rattachés.
     */
    void deleteCategory(Long id);

    /**
     * Crée un nouveau produit
     *
     * @param createProductDTO Les informations du produit à créer
     * @throws RuntimeException si le nom du produit existe déjà, si la catégorie n'existe pas ou si une erreur survient
     */
    void createProduct(CreateProductDTO createProductDTO);

    /**
     * Récupère la liste paginée de tous les produits avec recherche et filtres
     *
     * @param page Numéro de la page (0-indexed)
     * @param size Taille de la page
     * @param search Terme de recherche (nom ou code produit)
     * @param categoryId ID de la catégorie pour filtrer
     * @param status Statut actif/inactif pour filtrer (true = actif, false = inactif)
     * @return Réponse paginée contenant la liste des produits
     */
    ProductListResponseDTO getAllProducts(int page, int size, String search, Long categoryId, Boolean status);

    /**
     * Récupère les détails d'un produit par son ID
     *
     * @param id ID du produit
     * @return DTO contenant les détails du produit
     * @throws RuntimeException si le produit n'existe pas ou si une erreur survient
     */
    ProductDetailsDTO getProductById(Long id);

    /**
     * Met à jour un produit existant
     *
     * @param id ID du produit à modifier
     * @param updateProductDTO Les informations à mettre à jour (tous les champs sont optionnels)
     * @throws RuntimeException si le produit n'existe pas, si le nom existe déjà, si la catégorie n'existe pas ou si une erreur survient
     */
    void updateProduct(Long id, UpdateProductDTO updateProductDTO);

    /**
     * Active ou désactive un produit
     *
     * @param id ID du produit à activer/désactiver
     * @param updateProductStatusDTO Le statut à appliquer (true = actif, false = inactif)
     * @throws RuntimeException si le produit n'existe pas ou si une erreur survient
     */
    void updateProductStatus(Long id, UpdateProductStatusDTO updateProductStatusDTO);

    /**
     * Exporte la liste des produits en fichier Excel selon les filtres.
     * Le service retourne les données brutes (ByteArrayResource).
     * Le controller ajoute les headers HTTP (Content-Disposition, Content-Type) pour le téléchargement et retourne ResponseEntity<Resource> (la réponse complète).
     *
     * @param search Terme de recherche (nom ou code produit)
     * @param categoryId ID de la catégorie pour filtrer
     * @param status Statut actif/inactif pour filtrer (true = actif, false = inactif)
     * @return ByteArrayResource contenant les données brutes du fichier Excel
     * @throws RuntimeException si une erreur survient lors de la génération du fichier
     */
    ByteArrayResource exportProducts(String search, Long categoryId, Boolean status);

    /**
     * Récupère les statistiques des produits
     *
     * @return  ProductStatsDTO  contenant total, activé, désactivé
     * @throws RuntimeException si une erreur survient
     */
    ProductStatsDTO getProductStats();

    /**
     * Récupère le top 5 des produits les plus commandés avec leur taux d'utilisation en %.
     * Chaque produit est retourné avec son nom et le pourcentage que représentent ses
     * quantités commandées par rapport au total des quantités sur la période (ex. 30 derniers jours).
     *
     * @return liste de TopProductUsageDTO (productName, usagePercent entre 0 et 100)
     */
    List<TopProductUsageDTO> getTop5ProductUsage();

    /**
     * Récupère la liste des fournisseurs (id + nom)
     *
     * @return Liste des fournisseurs
     */
    List<SupplierListItemDTO> getAllSuppliers();

    /**
     * Crée un nouveau fournisseur avec toutes les informations détaillées.
     *
     * @param dto Les informations du fournisseur à créer
     */
    void createSupplier(CreateSupplierDTO dto);

    /**
     * Liste paginée des fournisseurs avec recherche et filtres.
     */
    SupplierListResponseDTO getSuppliers(int page, int size, String search, Long categoryId, SupplierType type, Boolean status);

    /**
     * Récupère les détails d'un fournisseur par son ID.
     */
    SupplierDetailsDTO getSupplierById(Long id);

    /**
     * Met à jour les informations d'un fournisseur.
     */
    void updateSupplier(Long id, UpdateSupplierDTO dto);

    /**
     * Active ou désactive un fournisseur.
     */
    void updateSupplierStatus(Long id, UpdateSupplierStatusDTO dto);

    /**
     * Récupère les statistiques des fournisseurs (Total, Actifs, Inactifs).
     */
    SupplierStatsDTO getSupplierStats();

    /**
     * Crée une nouvelle option de livraison
     *
     * @param dto Les informations de l'option de livraison à créer
     * @throws RuntimeException si une option avec le même nom existe déjà ou si une erreur survient
     */
    void createDeliveryOption(DeliveryOptionDTO dto);

    /**
     * Récupère toutes les options de livraison
     *
     * @return Liste des options de livraison
     */
    List<DeliveryOptionDTO> getAllDeliveryOptions();

    // ============================================================================
    // 💰 FRAIS (paramétrables par l'admin)
    // ============================================================================

    List<FeeDTO> getAllFees();
    void createFee(CreateFeeDTO dto);

    /**
     * Crée un nouvel utilisateur "agent": Admin, Responsable logistique, Commercial ou Livreur.
     * Les salariés (EMPLOYEE) ne se créent pas ici — ils sont créés par les commerciaux.
     * Envoi d'un code d'activation par email  si nécessaire .
     *
     * @param dto Données du nouvel utilisateur (prénom, nom, email, téléphone, rôle)
     */
    void createUser(SaveUserDTO dto);

    /**
     * Liste paginée de tous les utilisateurs (tous rôles) avec filtres optionnels.
     *
     * @param page   numéro de page (0-based)
     * @param size   nombre d'éléments par page
     * @param search recherche sur prénom, nom ou email (optionnel)
     * @param role   filtre par rôle (optionnel)
     * @param status filtre par statut actif/inactif (optionnel, true = actif, false = inactif)
     */
    UserListResponseDTO getUsers(int page, int size, String search, UserRole role, Boolean status);

    /**
     * Export Excel de la liste des utilisateurs (mêmes filtres que {@link #getUsers} sans pagination).
     */
    ByteArrayResource exportUsers(String search, UserRole role, Boolean status);

    /**
     * Statistiques utilisateurs pour la page Gestion des utilisateurs (total, actifs, inactifs).
     */
    UserStatsDTO getUsersStats();

    /**
     * Statistiques utilisateurs par rôle (effectif et % par rôle) pour le graphique "Utilisateurs par rôle".
     */
    List<UserStatsByRoleItemDTO> getUsersStatsByRole();

    /**
     * Répartition des statuts (Actifs / Inactifs) pour le graphique "Répartition des statuts" (donut).
     */
    List<UserStatsByStatusItemDTO> getUsersStatsByStatus();

    /**
     * Voir détails d'un utilisateur par son id .
     */
    UserDetailsDTO getUserById(Long id);

    /**
     * Activer ou désactiver un utilisateur (toggle statut).
     */
    void updateUserStatus(Long id, UpdateUserStatusDTO dto);

    /**
     * Modifier les infos de base d'un utilisateur (prénom, nom, email, téléphone, rôle, companyCommercial).
     */
    void updateUser(Long id, SaveUserDTO dto);

    /**
     * Met à jour la photo de profil d'un utilisateur (upload du fichier puis mise à jour de profilePhotoUrl).
     * Réservé à l'admin ou à l'utilisateur lui-même selon l'endpoint appelé.
     */
    void updateUserProfilePhoto(Long userId, MultipartFile file);

    /**
     * Supprime la photo de profil d'un utilisateur (fichier MinIO + champ en base).
     * Réservé à l'administrateur.
     */
    void removeUserProfilePhoto(Long userId);

    /**
     * Met à jour la photo de profil de l'utilisateur connecté (salarié, livreur, admin).
     * Utilisé par PUT /api/users/me/profile-photo.
     */
    void updateProfilePhotoForCurrentUser(MultipartFile file);

    /**
     * Supprime la photo de profil de l'utilisateur authentifié (même effet que {@link #removeUserProfilePhoto(Long)} sans être admin).
     */
    void removeProfilePhotoForCurrentUser();

    // ============================================================================
    // 📊 DASHBOARD ADMIN
    // ============================================================================

    /**
     * Statistiques du tableau de bord admin (KPIs + paiements par statut).
     * Données globales, sans filtre de période.
     *
     * @param periode ignoré (conservé pour compatibilité signature)
     * @return DTO avec : commandes en attente, paiements échoués, réclamations ouvertes, liste paiements par statut
     */
    AdminDashboardStatsDTO getDashboardStats(String periode);

    /**
     * Alias historique : même résultat que {@link #getLivraisonsParJour()} (date prévue, livrées à la date, retard).
     * GET /admin/dashboard/commandes-vs-livraisons — conservé pour compatibilité clients.
     */
    List<LivraisonParJourDTO> getCommandesVsLivraisons();

    /**
     * Effectifs pour le donut "Stocks - État global" : Normal, Sous seuil, Critique (rupture).
     */
    StockEtatGlobalDTO getStockEtatGlobal();

    /**
     * Effectifs des tournées par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) pour le donut « Statut des livraisons ».
     */
    StatutTourneesDTO getStatutTournees();

    /**
     * Graphique « Livraisons 7 jours » : pour chaque des 7 derniers jours, date + nbPrevues, nbLivreesALaDate, nbRetard.
     */
    List<LivraisonParJourDTO> getLivraisonsParJour();

    /**
     * Graphique "Tendance des coupons utilisés" : pour chacun des 7 derniers jours, date (dd/MM) et nombre d'utilisations (commandes avec coupon).
     */
    List<CouponUsageParJourDTO> getCouponsUtilisesParJour();

    /**
     * Liste des alertes pour le tableau de bord admin (livraisons en retard, stocks sous seuil).
     * Chaque alerte contient type, message, detail, module (LIVRAISONS / STOCKS pour la navigation), date.
     */
    AdminAlertsDTO getAlerts();

    // ============================================================================
    // 📋 RÉFÉRENTIELS (types réclamation, raisons livraison livreur/salarié)
    // ============================================================================

    List<ReferenceItemDTO> getAllClaimProblemTypes();
    ReferenceItemDTO getClaimProblemTypeById(Long id);
    void createClaimProblemType(CreateReferenceItemDTO dto);
    void updateClaimProblemType(Long id, CreateReferenceItemDTO dto);
    void deleteClaimProblemType(Long id);

    List<ReferenceItemDTO> getAllDeliveryIssueReasons();
    ReferenceItemDTO getDeliveryIssueReasonById(Long id);
    void createDeliveryIssueReason(CreateReferenceItemDTO dto);
    void updateDeliveryIssueReason(Long id, CreateReferenceItemDTO dto);
    void deleteDeliveryIssueReason(Long id);

    List<ReferenceItemDTO> getAllEmployeeDeliveryIssueReasons();
    ReferenceItemDTO getEmployeeDeliveryIssueReasonById(Long id);
    void createEmployeeDeliveryIssueReason(CreateReferenceItemDTO dto);
    void updateEmployeeDeliveryIssueReason(Long id, CreateReferenceItemDTO dto);
    void deleteEmployeeDeliveryIssueReason(Long id);

    List<ReferenceItemDTO> getAllCompanySectors();
    ReferenceItemDTO getCompanySectorById(Long id);
    void createCompanySector(CreateReferenceItemDTO dto);
    void updateCompanySector(Long id, CreateReferenceItemDTO dto);
    void deleteCompanySector(Long id);
}