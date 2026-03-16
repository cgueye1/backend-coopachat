package com.example.coopachat.dtos.order;

import com.example.coopachat.dtos.products.ProductPreviewDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** DTO pour afficher les détails d'une commande salarié (sérialisé en JSON pour le front). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailsDTO {

    private String orderNumber;
    private LocalDate validationDate;
    private String employeeName;
    private String status;
    /** Nom complet du livreur qui a livré la commande (null si aucun livreur associé). */
    private String driverName;
    private List<ProductPreviewDTO> listProducts;
}
