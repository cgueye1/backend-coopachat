package com.example.coopachat.controllers;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.CreateSupplierOrderDTO;
import com.example.coopachat.dtos.supplierOrders.UpdateSupplierOrderDTO;
import com.example.coopachat.services.LogisticsManager.LogisticsManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion des actions du Responsable Logistique
 * Regroupe toutes les fonctionnalités liées au rôle Responsable Logistique
 */
@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
@Tag(name = "Responsable Logistique", description = "API pour la gestion des actions du Responsable Logistique ")
public class LogisticsManagerController {

    private final LogisticsManagerService logisticsManagerService;

    // ============================================================================
    // 🚚 GESTION DES LIVREURS
    // ============================================================================

    @Operation(
            summary = "Créer un nouveau livreur",
            description = "Permet à un Responsable Logistique de créer un nouveau livreur. " +
                         "Un email d'invitation avec un code d'activation sera envoyé au livreur."
    )
    @PostMapping("/drivers")
    public ResponseEntity<String> createDriver(@RequestBody @Valid RegisterDriverRequestDTO driverDTO) {
        logisticsManagerService.createDriver(driverDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Livreur créé avec succès. Un email d'invitation a été envoyé.");
    }

    // ============================================================================
    // 📦 GESTION DES COMMANDES FOURNISSEURS
    // ============================================================================

    @Operation(
            summary = "Créer une nouvelle commande fournisseur",
            description = "Permet à un Responsable Logistique de créer une nouvelle commande fournisseur. " +
                         "La commande peut contenir un ou plusieurs produits. " +
                         "Chaque produit doit avoir une quantité commandée."
    )
    @PostMapping("/supplier-orders")
    public ResponseEntity<String> createSupplierOrder(@RequestBody @Valid CreateSupplierOrderDTO createSupplierOrderDTO) {
        try {
            logisticsManagerService.createSupplierOrder(createSupplierOrderDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Commande fournisseur créée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @Operation(
            summary = "Modifier une commande fournisseur",
            description = "Met à jour les informations d'une commande fournisseur existante. " +
                         "Seules les commandes avec le statut 'En attente' peuvent être modifiées. " +
                         "Tous les champs sont optionnels - seuls les champs fournis seront mis à jour. " +
                         "Si 'items' est fourni, il remplace toute la liste des produits."
    )
    @PutMapping("/supplier-orders/{id}")
    public ResponseEntity<String> updateSupplierOrder(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSupplierOrderDTO updateSupplierOrderDTO
    ) {
        try {
            logisticsManagerService.updateSupplierOrder(id, updateSupplierOrderDTO);
            return ResponseEntity.ok("Commande fournisseur modifiée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}


