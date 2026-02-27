package com.example.coopachat.enums;

/**
 * Périmètre d'application du coupon.
 * - ALL_PRODUCTS / CATEGORIES / PRODUCTS : coupon produit ou catégorie (auto, appliqué au prix unitaire, stocké dans CartItem.promoPrice).
 * - CART_TOTAL : code promo manuel, pas lié à un produit/catégorie, s'applique sur le TOTAL du panier (saisi par le salarié dans CreateOrderDTO.couponCode).
 */
public enum CouponScope {
    ALL_PRODUCTS("Tous les produits"),
    CATEGORIES("Catégories"),
    PRODUCTS("Produits ciblés"),
    CART_TOTAL("Sur le total du panier");

    private final String label;

    CouponScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
