package com.example.coopachat.services.Employee;

import com.example.coopachat.dtos.UserDeliveryPrefererence.DeliveryPreferenceDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.employees.AddressDTO;
import com.example.coopachat.dtos.employees.EmployeePersonalInfoDTO;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.products.ProductCatalogueListResponseDTO;
import com.example.coopachat.dtos.products.ProductMobileDetailsDTO;

import java.util.List;

public interface EmployeeService {

    // ============================================================================
    // 🔐 ACTIVATION COMPTE SALARIÉ
    // ============================================================================

    /**
     * Active le compte d'un salarié et crée son mot de passe via le token d'invitation
     *
     * @param token Le token d'invitation reçu par email
     * @param newPassword Le nouveau mot de passe à définir
     * @param confirmPassword La confirmation du nouveau mot de passe
     * @throws RuntimeException si le token est invalide, expiré, si les mots de passe ne correspondent pas, ou si l'utilisateur n'existe pas
     */
    void activateEmployeeAccount(String token, String newPassword, String confirmPassword);

    // ============================================================================
    // 🏠 ACCUEIL SALARIÉ
    // ============================================================================

    /**
     * Récupère les données d'accueil du salarié.
     */
    HomeResponseDTO getHomeData();

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



}
