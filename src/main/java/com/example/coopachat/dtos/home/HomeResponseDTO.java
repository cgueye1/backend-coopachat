package com.example.coopachat.dtos.home;

import com.example.coopachat.dtos.categories.CategoryHomeItemDTO;
import com.example.coopachat.dtos.coupons.CouponPromoDTO;
import com.example.coopachat.dtos.products.ProductPromoItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour l'accueil salarié (home).
 * Sans filtre : products = 4 derniers, champs pagination à null.
 * Avec filtre (search et/ou categoryId) : products = page de résultats, pagination renseignée.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeResponseDTO {

    private String firstName;
    private List<ProductPromoItemDTO> products;
    private List<CategoryHomeItemDTO> categories; // max 4
    private List<CouponPromoDTO> activeCoupons; // liste des coupons "panier" / hors produit-catégorie (vide si aucun)

    /** Présents uniquement quand un filtre (search/categoryId) est appliqué. */
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
