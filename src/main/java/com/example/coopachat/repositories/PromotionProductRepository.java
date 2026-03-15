package com.example.coopachat.repositories;

import com.example.coopachat.entities.PromotionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité PromotionProduct.
 */
@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {

    List<PromotionProduct> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);

    /** Nombre de produits dans une promotion. */
    long countByPromotionId(Long promotionId);
}
