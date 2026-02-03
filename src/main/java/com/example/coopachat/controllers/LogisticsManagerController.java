package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.CancelDeliveryTourDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.EligibleOrderDTO;
import com.example.coopachat.dtos.order.OrderEmployeeListResponseDTO;
import com.example.coopachat.dtos.order.OrderItemDetailsDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.SupplierOrderStatus;
import com.example.coopachat.enums.TimeSlot;
import com.example.coopachat.services.LogisticsManager.LogisticsManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Operation(
            summary = "Lister les fournisseurs",
            description = "Récupère la liste complète des fournisseurs actifs (id + nom)."
    )
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierListItemDTO>> getAllSuppliers() {
        List<SupplierListItemDTO> suppliers = logisticsManagerService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
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

    // ============================================================================
    // 📦 SUIVI DES STOCKS
    // ============================================================================
    @Operation(
            summary = "Lister le suivi des stocks",
            description = "Récupère la liste paginée des produits pour le suivi des stocks avec recherche et filtres optionnels."
    )
    @GetMapping("/stocks")
    public ResponseEntity<ProductStockListResponseDTO> getStockList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean status
    ) {
        ProductStockListResponseDTO response = logisticsManagerService.getStockList(page, size, search, categoryId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Exporter le suivi des stocks",
            description = "Exporte la liste des produits du suivi des stocks en Excel avec recherche et filtres optionnels."
    )
    @GetMapping("/stocks/export")
    public ResponseEntity<Resource> exportStockList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean status
    ) {
        ByteArrayResource resource = logisticsManagerService.exportStockList(search, categoryId, status);

        String fileName = "suivi_stocks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    // ============================================================================
    // ➕/➖ MOUVEMENTS DE STOCK
    // ============================================================================
    @Operation(
            summary = "Entrée de stock",
            description = "Augmente le stock d'un produit (quantité positive)."
    )
    @PostMapping("/stocks/{productId}/in")
    public ResponseEntity<String> increaseStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity
    ) {
        try {
            logisticsManagerService.increaseStock(productId, quantity);
            return ResponseEntity.ok("Stock augmenté avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Sortie de stock",
            description = "Diminue le stock d'un produit (quantité positive)."
    )
    @PostMapping("/stocks/{productId}/out")
    public ResponseEntity<String> decreaseStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity
    ) {
        try {
            logisticsManagerService.decreaseStock(productId, quantity);
            return ResponseEntity.ok("Stock diminué avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Modifier le seuil minimum",
            description = "Met à jour le seuil minimum de stock d'un produit."
    )
    @PatchMapping("/stocks/{productId}/threshold")
    public ResponseEntity<String> updateMinThreshold(
            @PathVariable Long productId,
            @RequestParam Integer minThreshold
    ) {
        try {
            logisticsManagerService.updateMinThreshold(productId, minThreshold);
            return ResponseEntity.ok("Seuil minimum mis à jour avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Modifier le seuil minimum par pourcentage",
            description = "Met à jour le seuil minimum en appliquant un pourcentage sur le seuil actuel."
    )
    @PatchMapping("/stocks/{productId}/threshold/percent")
    public ResponseEntity<String> updateMinThresholdByPercent(
            @PathVariable Long productId,
            @RequestParam Integer percent
    ) {
        try {
            logisticsManagerService.updateMinThresholdByPercent(productId, percent);
            return ResponseEntity.ok("Seuil minimum mis à jour avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Statistiques du suivi des stocks",
            description = "Retourne le total des produits, le nombre sous-seuil et en rupture."
    )
    @GetMapping("/stocks/stats")
    public ResponseEntity<StockStatsDTO> getStockStats() {
        StockStatsDTO stats = logisticsManagerService.getStockStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Liste des alertes de réapprovisionnement",
            description = "Récupère la liste paginée des produits en alerte (stock < seuil) avec recherche et filtre catégorie."
    )
    @GetMapping("/stocks/alerts")
    public ResponseEntity<ProductStockListResponseDTO> getStockAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId
    ) {
        ProductStockListResponseDTO response = logisticsManagerService.getStockAlerts(page, size, search, categoryId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Exporter les alertes de réapprovisionnement",
            description = "Exporte la liste des produits en alerte de stock (stock < seuil) en Excel."
    )
    @GetMapping("/stocks/alerts/export")
    public ResponseEntity<Resource> exportStockAlerts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId
    ) {
        ByteArrayResource resource = logisticsManagerService.exportStockAlerts(search, categoryId);

        String fileName = "alertes_stock_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
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

    // ============================================================================
    // 📦 GESTION DES COMMANDES SALARIÉS
    // ============================================================================
    @Operation(
            summary = "Lister les commandes salariés",
            description = "Récupère la liste paginée des commandes passées par les salariés avec recherche et filtres optionnels."
    )
    @GetMapping("/employee-orders")
    public ResponseEntity<OrderEmployeeListResponseDTO> getAllEmployeeOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status) {

        OrderEmployeeListResponseDTO response = logisticsManagerService.getAllEmployeeOrders(page, size, search, status);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'une commande salarié",
            description = "Récupère toutes les informations détaillées d'une commande salarié"
    )
    @GetMapping("/employee-order/{id}")
    public ResponseEntity<OrderItemDetailsDTO> getOrderById(@PathVariable Long id) {
        OrderItemDetailsDTO details = logisticsManagerService.getOrderItemDetailById(id);
        return ResponseEntity.ok(details);
    }

    @Operation(
            summary = "Exporter les commandes salariés",
            description = "Exporte la liste des commandes salariés en fichier Excel (une ligne par commande)."
    )
    @GetMapping("/employee-orders/export")
    public ResponseEntity<Resource> exportEmployeeOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status) {

        ByteArrayResource resource = logisticsManagerService.exportEmployeeOrders(search, status);
        //le nom du fichier
        String fileName = "commandes_salaries_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm"))
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    // ============================================================================
   // 🚚 GESTION DES TOURNÉES DE LIVRAISON
   // ============================================================================

    @Operation(
            summary = "Récupérer les zones disponibles",
            description = "Retourne la liste des zones de livraison actives pour le formulaire de création de tournée."
    )
    @GetMapping("/delivery-tours/zones")
    public ResponseEntity<List<ZoneOptionDTO>> getAvailableZones() {
        List<ZoneOptionDTO> zones = logisticsManagerService.getAvailableZones();
        return ResponseEntity.ok(zones);
    }

    @Operation(
            summary = "Récupérer les commandes éligibles",
            description = "Retourne la liste des commandes disponibles pour une tournée selon la date et le créneau."
    )
    @GetMapping("/delivery-tours/eligible-orders")
    public ResponseEntity<List<EligibleOrderDTO>> getEligibleOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam TimeSlot timeSlot) {

        List<EligibleOrderDTO> orders = logisticsManagerService.getEligibleOrders(deliveryDate, timeSlot);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Récupérer les chauffeurs disponibles",
            description = "Retourne la liste des chauffeurs disponibles pour une tournée selon la date, le créneau et la zone."
    )
    @GetMapping("/delivery-tours/available-drivers")
    public ResponseEntity<List<AvailableDriverDTO>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam TimeSlot timeSlot,
            @RequestParam String deliveryZone) {

        List<AvailableDriverDTO> drivers = logisticsManagerService.getAvailableDrivers(deliveryDate, timeSlot, deliveryZone);
        return ResponseEntity.ok(drivers);
    }

    @Operation(
            summary = "Créer une tournée de livraison",
            description = "Permet au responsable logistique de créer une nouvelle tournée de livraison avec un chauffeur et des commandes sélectionnées."
    )
    @PostMapping("/delivery-tours")
    public ResponseEntity<String> createDeliveryTour(@RequestBody @Valid CreateDeliveryTourDTO dto) {
        logisticsManagerService.createDeliveryTour(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Tournée créée avec succès");
    }

    @Operation(
            summary = "Récupérer les détails d'une tournée",
            description = "Retourne les détails complets d'une tournée de livraison spécifique."
    )
    @GetMapping("/delivery-tours/{tourId}")
    public ResponseEntity<DeliveryTourDetailsDTO> getDeliveryTourDetails(
            @PathVariable Long tourId) {

        DeliveryTourDetailsDTO tourDetails = logisticsManagerService.getDeliveryTourDetails(tourId);
        return ResponseEntity.ok(tourDetails);
    }

    @Operation(
            summary = "Lister les tournées de livraison",
            description = "Retourne la liste paginée des tournées avec filtres."
    )
    @GetMapping("/delivery-tours")
    public ResponseEntity<DeliveryTourListResponseDTO> getAllDeliveryTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tourNumber,
            @RequestParam(required = false) DeliveryTourStatus status) {

        DeliveryTourListResponseDTO response = logisticsManagerService
                .getAllDeliveryTours(page, size, tourNumber, status);
        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Modifier une tournée",
            description = "Met à jour les informations d'une tournée (zone, véhicule, notes, statut)"
    )
    @PatchMapping("/delivery-tours/{tourId}")
    public ResponseEntity<String> updateDeliveryTour(
            @PathVariable Long tourId,
            @RequestBody @Valid UpdateDeliveryTourDTO dto) {

        logisticsManagerService.updateDeliveryTour(tourId, dto);
        return ResponseEntity.ok("Tournée de livraison mis à jour avec succès");
    }

    @Operation(
            summary = "Proposer une tournée",
            description = "Propose une tournée planifiée à un livreur (statut: PLANIFIEE → PROPOSEE) "
                    + "après vérification chauffeur assigné et commandes existantes."
    )
    @PostMapping("/delivery-tours/{tourId}/ propose")
    public ResponseEntity<String> proposeDeliveryTour(@PathVariable Long tourId) {

        logisticsManagerService. proposeDeliveryTour(tourId);

        return ResponseEntity.ok("Tournée proposée avec succès");
    }

    @Operation(
            summary = "Annuler une tournée",
            description = "Annule une tournée planifiée ou proposée (statut: PLANIFIEE/PROPOSEE → ANNULEE) "
                    + "avec motif obligatoire."
    )
    @PostMapping("/delivery-tours/{tourId}/cancel")
    public ResponseEntity<String> cancelDeliveryTour(
            @PathVariable Long tourId,
            @RequestBody @Valid CancelDeliveryTourDTO dto) {

        logisticsManagerService.cancelDeliveryTour(tourId, dto);
        return ResponseEntity.ok("Tournée annulée avec succès");
    }

    @Operation(
            summary = "Exporter les tournées de livraison",
            description = "Exporte la liste des tournées en fichier Excel"
    )
    @GetMapping("/delivery-tours/export")
    public ResponseEntity<Resource> exportDeliveryTours(
            @RequestParam(required = false) String tourNumber,
            @RequestParam(required = false) DeliveryTourStatus status) {

        ByteArrayResource resource = logisticsManagerService.exportDeliveryTours(tourNumber, status);

        String fileName = "tournees_livraison_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm"))
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }


}


