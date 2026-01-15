package com.example.coopachat.controllers;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.enums.SupplierOrderStatus;
import com.example.coopachat.services.LogisticsManager.LogisticsManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Operation(
            summary = "Récupérer les détails d'une commande fournisseur",
            description = "Récupère toutes les informations détaillées d'une commande fournisseur, " +
                         "incluant le fournisseur, la date prévue, le statut, les notes et la liste complète des produits commandés."
    )
    @GetMapping("/supplier-orders/{id}")
    public ResponseEntity<SupplierOrderDetailsDTO> getSupplierOrderById(@PathVariable Long id) {
        try {
            SupplierOrderDetailsDTO details = logisticsManagerService.getSupplierOrderById(id);
            return ResponseEntity.ok(details);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @Operation(
            summary = "Lister les commandes fournisseurs",
            description = "Récupère la liste paginée des commandes fournisseurs avec recherche et filtres optionnels."
    )
    @GetMapping("/supplier-orders")
    public ResponseEntity <SupplierOrderListResponseDTO> getAllSupplierOrders (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) SupplierOrderStatus status
    ){
        SupplierOrderListResponseDTO response = logisticsManagerService.getAllSupplierOrders( page, size, search, supplierId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Modifier le statut d'une commande fournisseur",
            description = "Permet de modifier le statut d'une commande fournisseur (ex: En attente, En cours, Livrée, Annulée)."
    )
    @PatchMapping("/suppliers-orders/{id}/status")
    public ResponseEntity <String> updateSupplierOrderStatus (@PathVariable Long id, @RequestBody @Valid UpdateSupplierOrderStatusDTO updateSupplierOrderStatusDTO){
        logisticsManagerService.updateSupplierOrderStatus(id, updateSupplierOrderStatusDTO);
        return  ResponseEntity.ok("Statut de la commande mis à jour avec succès");
    }

    @Operation(
            summary = "Statistiques des commandes fournisseurs",
            description = "Retourne le total des commandes, le nombre en attente, livrées et annulées."
    )
    @GetMapping("/supplier-orders/stats")
    public ResponseEntity<SupplierOrderStatsDTO> getSupplierOrderStats() {
        SupplierOrderStatsDTO stats = logisticsManagerService.getSupplierOrderStats();
        return ResponseEntity.ok(stats);
    }
    // ============================================================================
    // 📤 EXPORT DES COMMANDES FOURNISSEURS
    // ============================================================================
    @GetMapping("/supplier-orders/export")
    public ResponseEntity<Resource> exportSupplierOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) SupplierOrderStatus status
    ) {
        ByteArrayResource resource = logisticsManagerService.exportSupplierOrders(search, supplierId, status);

        // Générer le nom du fichier avec la date et l'heure actuelles
        String fileName = "Commandes Fournisseurs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm")) + ".xlsx";


        // Retourner le fichier avec les headers appropriés pour le téléchargement
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);

        }
}


