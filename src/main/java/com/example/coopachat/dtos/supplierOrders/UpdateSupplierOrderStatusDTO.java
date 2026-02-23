package com.example.coopachat.dtos.supplierOrders;

import com.example.coopachat.enums.SupplierOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la modification du statut d'une commande fournisseur
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSupplierOrderStatusDTO {

    @NotNull(message = "Le statut de la commande est obligatoire")
    private SupplierOrderStatus status;
}




