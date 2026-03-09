package com.example.coopachat.services.Employee;

import com.example.coopachat.dtos.employees.DeliveryPreferenceDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.employees.AddressDTO;
import com.example.coopachat.dtos.employees.EmployeePersonalInfo;
import com.example.coopachat.dtos.employees.EmployeePersonalInfoDTO;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.claim.ClaimDetailDTO;
import com.example.coopachat.dtos.claim.ClaimListResponseDTO;
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.enums.ClaimStatus;
import com.example.coopachat.dtos.products.ProductCatalogueListResponseDTO;
import com.example.coopachat.dtos.products.ProductMobileDetailsDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
/**
 * Interface pour le service de gestion des actions de l'employé
 */
public interface EmployeeService {

     // ============================================================================
    // 🏠 ACCUEIL SALARIÉ
    // ============================================================================

    /**
     * Récupère les données d'accueil du salarié.
     * Sans filtre (search et categoryId null/vides) : 4 derniers produits, pas de pagination.
     * Avec filtre : produits filtrés avec pagination (page, size).
     */
    HomeResponseDTO getHomeData(String search, Long categoryId, int page, int size);

    // ============================================================================
    // 📚 CATALOGUE (CATÉGORIES + PRODUITS)
    // ============================================================================

    /**
     * Liste toutes les categories (pour le catalogue).
     */
    List<CategoryListItemDTO> getAllCategories();

    /**
     * Liste les produits du catalogue avec filtres (nom + categorie).
     */
    ProductCatalogueListResponseDTO getCatalogueProducts(int page, int size, String search, Long categoryId);

    // ============================================================================
    // 🔍 DÉTAILS PRODUIT
    // ============================================================================

    /**
     * Récupère les détails complets d'un produit par son ID.
     *
     * @param productId L'ID du produit à récupérer
     * @return Les détails complets du produit
     * @throws RuntimeException si le produit n'existe pas ou n'est pas actif
     */
    ProductMobileDetailsDTO getProductDetailsById(Long productId);

    // ============================================================================
    // 🛒 PANIER
    // ============================================================================

    /**
     * Ajoute un produit au panier de l'utilisateur connecté.
     * Si le produit est déjà dans le panier, augmente sa quantité.
     * @param productId L'ID du produit à ajouter
     * @throws RuntimeException  si le stock est insuffisant ou si une erreur survient
     */
    void addProductToCart(Long productId);

    /**
     * Récupère le panier complet de l'utilisateur connecté.
     *
     * @return CartResponseDTO contenant tous les articles du panier et le total
     * @throws RuntimeException si utilisateur non connecté
     */
    CartResponseDTO getCart();

    /**
     * Augmente de 1 la quantité d'un produit déjà présent dans le panier.
     *
     * @param productId ID du produit à augmenter
     * @throws RuntimeException si produit non trouvé, non dans le panier ou stock insuffisant
     */
    void increaseProductQuantity(Long productId);

    /**
     * Diminue de 1 la quantité d'un produit déjà présent dans le panier.
     * Si la quantité atteint 0, l'article est supprimé du panier.
     *
     * @param productId ID du produit à diminuer
     * @throws RuntimeException si produit non trouvé ou non dans le panier
     */
    void decreaseProductQuantity(Long productId);

    /**
     * Supprime complètement un produit du panier.
     *
     * @param productId ID du produit à supprimer
     * @throws RuntimeException si produit non trouvé ou non dans le panier
     */
    void removeProductFromCart(Long productId);

    // ============================================================================
    // Préférences de Livraisons🛵
    // ============================================================================
    /**
     * Crée ou met à jour les préférences de livraison de l'utilisateur connecté
     *
     * @param dto DTO contenant les préférences (jours, créneaux, mode)
     * @throws RuntimeException si utilisateur non trouvé
     */
     void saveDeliveryPreference(DeliveryPreferenceDTO dto);

    /**
     * Récupère les préférences de livraison de l'utilisateur connecté
     *
     * @return DeliveryPreferenceDTO les préférences de l'utilisateur
     * @throws RuntimeException si utilisateur non trouvé
     */
    DeliveryPreferenceDTO getDeliveryPreference();

    // ============================================================================
    // Informations Personnelles 📋
    // ============================================================================

    /**
     * Récupère les informations personnelles d'un employé
     * @return DTO contenant les informations personnelles
     */
    EmployeePersonalInfoDTO getPersonalInfo();

    /**
     *  Met à jour les informations personnelles
     * (mais utilise que les 3 champs modifiables)
     * @param employeeId ID de l'employé
     * @param updateRequest DTO contenant les nouvelles valeurs
     */
    void updatePersonalInfo(EmployeePersonalInfoDTO updateRequest);

    /**
     * Crée une nouvelle adresse de livraison pour le salarié
     * @param dto Données de l'adresse
     * @return L'adresse créée
     */
     void createAddress(AddressDTO dto);

    /**
     * Met à jour l' adresses du salarié
     * @param addressId ID de l'adresse
     * @param dto Nouvelles données
     */
    void updateAddress(Long addressId, AddressDTO dto);

    /**
     * Récupère toutes les adresses du salarié
     * @return Liste des adresses
     */
    List<AddressDTO> getMyAddresses();


    // ============================================================================
    // Mes commandes (profil client)
    // ============================================================================

    /**
     * Liste des options de livraison actives (pour le formulaire de commande : fréquence Hebdomadaire, etc.).
     */
    List<com.example.coopachat.dtos.delivery.DeliveryOptionDTO> getActiveDeliveryOptions();

    /**
     * Étape 2 (Livraison) : retourne un aperçu de la commande sans rien sauvegarder en base.
     * Utilisé pour afficher l'écran de récap avant que le salarié ne clique sur "Valider ma commande".
     *
     * @param dto Contient l'option de livraison (deliveryOptionId) et le code promo (optionnel)
     * @return OrderPreviewDTO avec nbArticles, option de livraison, date estimée, adresse, total
     */
    OrderPreviewDTO previewOrder(CreateOrderDTO dto);

    /**
     * Étape 3 (Confirmation) : crée la commande en base, ajoute le paiement (Impayé), vide le panier.
     * À appeler uniquement quand le salarié clique sur "Valider ma commande".
     *
     * @param dto Contient l'option de livraison (deliveryOptionId) et le code promo (optionnel)
     */
    void createOrder(CreateOrderDTO dto);

    /**
     * Liste des commandes du client (salarié connecté), avec filtres optionnels et pagination.
     * Chaque commande inclut les infos livreur si EN_COURS/ARRIVE, rating/canRate si LIVREE.
     *
     * @param status filtre par statut (ex. LIVREE) ou null
     * @param search recherche sur le numéro de commande ou null
     * @param page   numéro de page (0-based)
     * @param size   nombre d'éléments par page
     */
    ClientOrderListResponseDTO getMyOrders(String status, String search, int page, int size);

    /**
     * Détail d'une commande (client clique sur une commande).
     * Vérifie que la commande appartient à l'employé connecté.
     * Inclut driver si EN_COURS/ARRIVE, rating/canRate si LIVREE.
     */
    ClientOrderDetailsDTO getOrderDetails(Long orderId);

    /**
     * Infos de paiement pour l'écran "Payer la facture" (sous-total, frais de service, total, statut).
     * Vérifie que la commande appartient au client connecté.
     */
    PaymentInfoDTO getPaymentInfo(Long orderId);

    /**
     * Traite un paiement (simulation) pour une commande : Mobile Money ou Carte bancaire.
     */
    PaymentResponseDTO processPayment(Long orderId, ProcessPaymentDTO request);

    /**
     * Historique des paiements du salarié connecté (commandes payées, tri par date de paiement décroissante).
     */
    List<PaymentHistoryItemDTO> getPaymentHistory();

    /**
     * Génère une facture au format PDF pour un paiement déjà effectué sur une commande.
     *  @param orderId l'identifiant de la commande dont le paiement a été réalisé
     *  @return un tableau de bytes représentant le contenu du fichier PDF de la facture
     */
    byte[] generateInvoicePdf(Long orderId);

    /**
     * Envoyer une note pour une commande livrée (bouton "Noter le livreur").
     * Possible uniquement si statut = LIVREE, pas déjà noté, note 1-5.
     */
    void submitReview(Long orderId, SubmitReviewDTO dto);

    /**
     * Soumettre une réclamation sur une commande (produit concerné, nature du problème, commentaire).
     * Commande doit être livrée et appartenir au salarié connecté.
     */
    void submitClaim(Long orderId, Long orderItemId, Long claimProblemTypeId, String comment, List<MultipartFile> images);

    /**
     * Historique des réclamations du salarié connecté (liste paginée). Filtre optionnel par statut.
     */
    ClaimListResponseDTO getMyClaims(int page, int size, ClaimStatus status);

    /**
     * Détail d'une réclamation par id. Uniquement si la réclamation appartient au salarié connecté.
     */
    ClaimDetailDTO getMyClaimById(Long id);

    /**
     * Annuler une commande. Uniquement si la commande appartient au salarié connecté et si le statut est EN_ATTENTE.
     */
    void cancelOrder(Long orderId);

    /**
     * Signaler un problème de livraison sur sa commande (salarié).
     * Possible uniquement si la commande appartient au salarié et si le statut est EN_COURS ou ARRIVE.
     */
    void reportDeliveryIssue(Long orderId, com.example.coopachat.dtos.employee.EmployeeDeliveryIssueDTO dto);
}
