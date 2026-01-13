package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;

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
}
