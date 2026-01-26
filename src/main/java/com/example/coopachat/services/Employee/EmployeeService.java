package com.example.coopachat.services.Employee;

import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
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


}
