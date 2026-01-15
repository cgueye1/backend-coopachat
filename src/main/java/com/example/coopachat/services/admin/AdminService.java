package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import org.springframework.core.io.ByteArrayResource;

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
}
