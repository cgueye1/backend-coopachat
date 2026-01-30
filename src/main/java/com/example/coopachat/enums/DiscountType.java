package com.example.coopachat.enums;

public enum DiscountType {
    PERCENTAGE ("Pourcentage"),      // Réduction en pourcentage (ex: 10%)
    FIXED_AMOUNT ("Montant Fixe");    // Réduction en montant fixe (ex: 500 FCFA)

    private final String name;
    DiscountType (String name){
        this.name = name;
    }
    public String getName(){
        return name;
    }

}