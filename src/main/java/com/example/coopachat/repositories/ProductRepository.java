package com.example.coopachat.repositories;

import com.example.coopachat.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Product (Produit)
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Vérifie si un produit avec ce nom existe déjà
     */
    Boolean existsByName(String name);

    /**
     * Vérifie si un produit avec ce code existe déjà
     */
    Boolean existsByProductCode(String productCode);
}

