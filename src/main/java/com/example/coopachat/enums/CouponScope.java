package com.example.coopachat.enums;

public enum CouponScope {
    ALL_PRODUCTS("Tous les produits"),
    CATEGORIES("Categories"),
    PRODUCTS("Produits");

    private final String label;

    CouponScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
