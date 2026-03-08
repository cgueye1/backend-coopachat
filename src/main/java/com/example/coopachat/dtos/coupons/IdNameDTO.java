package com.example.coopachat.dtos.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO simple id + nom pour listes déroulantes (produits, catégories). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdNameDTO {
    private long id;
    private String name;
}
