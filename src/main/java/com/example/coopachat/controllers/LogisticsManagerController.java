package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.CancelDeliveryTourDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.dashboard.admin.CommandesVsLivraisonsDayDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.CommandesParJourDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.RLDashboardKpisDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatutTourneesDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.TauxRetoursParJourDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.EligibleOrderDTO;
import com.example.coopachat.dtos.order.EmployeeOrderStatsDTO;
import com.example.coopachat.dtos.order.EligibleOrderLotDTO;
import com.example.coopachat.dtos.order.OrderEmployeeListResponseDTO;
import com.example.coopachat.dtos.order.OrderItemDetailsDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.claim.ClaimDetailDTO;
import com.example.coopachat.dtos.claim.ClaimListResponseDTO;
import com.example.coopachat.dtos.claim.ClaimStatsDTO;
import com.example.coopachat.dtos.claim.RejectClaimDTO;
import com.example.coopachat.dtos.claim.ValidateClaimDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.enums.ClaimDecisionType;
import com.example.coopachat.enums.ClaimStatus;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.SupplierOrderStatus;
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

    // ============================================================================
    // 📦 GESTION DES  FOURNISSEURS
    // ============================================================================

    @Operation(
            summary = "Lister les fournisseurs",
            description = "Récupère la liste complète des fournisseurs actifs (id + nom)."
    )
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierListItemDTO>> getAllSuppliers() {
        List<SupplierListItemDTO> suppliers = logisticsManagerService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }
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


    // ============================================================================
    // 📦 GESTION DES COMMANDES SALARIÉS
    // ============================================================================
    @Operation(
            summary = "Lister les commandes salariés",
            description = "Récupère la liste paginée des commandes passées par les salariés avec recherche et filtres optionnels."
    )
    @GetMapping(value = "/employee-orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderEmployeeListResponseDTO> getAllEmployeeOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        OrderStatus orderStatus = parseOrderStatus(status);
        OrderEmployeeListResponseDTO response = logisticsManagerService.getAllEmployeeOrders(page, size, search, orderStatus);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Statistiques des commandes salariés",
            description = "Retourne les compteurs pour la page Gestion des commandes : EN ATTENTE, EN RETARD, EN COURS, LIVRÉES ce mois."
    )
    @GetMapping(value = "/employee-orders/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeOrderStatsDTO> getEmployeeOrderStats() {
        EmployeeOrderStatsDTO stats = logisticsManagerService.getEmployeeOrderStats();
        return ResponseEntity.ok(stats);
    }

    /** Convertit le paramètre status (optionnel) en OrderStatus. null si vide ou invalide. */
    private static OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank() || "--".equals(status.trim())) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Operation(
            summary = "Récupérer les détails d'une commande salarié",
            description = "Récupère toutes les informations détaillées d'une commande salarié"
    )
    @GetMapping(value = "/employee-order/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderItemDetailsDTO> getOrderById(@PathVariable Long id) {
        OrderItemDetailsDTO details = logisticsManagerService.getOrderItemDetailById(id);
        return ResponseEntity.ok(details);
    }

    @Operation(
            summary = "Replanifier une commande en échec",
            description = "Passe la commande en EN_ATTENTE, la retire de la tournée et notifie le salarié. Réservé au RL."
    )
    @PatchMapping("/employee-orders/{orderId}/replan")
    public ResponseEntity<String> replanOrder(@PathVariable Long orderId) {
        try {
            logisticsManagerService.replanOrder(orderId);
            return ResponseEntity.ok("Commande replanifiée. Elle réapparaîtra dans les commandes éligibles.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Annuler définitivement une commande après échec",
            description = "Passe la commande en ANNULEE, réintègre les produits en stock et notifie le salarié. Irréversible. Réservé au RL."
    )
    @PatchMapping("/employee-orders/{orderId}/cancel-after-failure")
    public ResponseEntity<String> cancelOrderAfterFailure(@PathVariable Long orderId) {
        try {
            logisticsManagerService.cancelOrderAfterFailure(orderId);
            return ResponseEntity.ok("Commande annulée. Stock réintégré.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Exporter les commandes salariés",
            description = "Exporte la liste des commandes salariés en fichier Excel (une ligne par commande)."
    )
    @GetMapping("/employee-orders/export")
    public ResponseEntity<Resource> exportEmployeeOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        OrderStatus orderStatus = parseOrderStatus(status);
        ByteArrayResource resource = logisticsManagerService.exportEmployeeOrders(search, orderStatus);
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
            summary = "Calendrier planification (mois)",
            description = "Vue globale : pour chaque jour du mois, nb commandes en attente (non planifiées) et nb commandes déjà planifiées."
    )
    @GetMapping("/delivery-tours/planning-calendar")
    public ResponseEntity<List<DeliveryPlanningCalendarDayDTO>> getPlanningCalendar(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(logisticsManagerService.getDeliveryPlanningCalendar(year, month));
    }

    @Operation(
            summary = "Récupérer les commandes éligibles",
            description = "Retourne la liste des commandes disponibles pour une tournée selon la date."
    )
    @GetMapping("/delivery-tours/eligible-orders")
    public ResponseEntity<List<EligibleOrderDTO>> getEligibleOrders(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate deliveryDate) {
        List<EligibleOrderDTO> orders = logisticsManagerService.getEligibleOrders(deliveryDate);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Commandes éligibles groupées par proximité",
            description = "Retourne les commandes éligibles regroupées en lots par proximité GPS (date + lotSize)."
    )
    @GetMapping("/delivery-tours/eligible-orders/grouped")
    public ResponseEntity<List<EligibleOrderLotDTO>> getGroupedEligibleOrders(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate deliveryDate,
            @RequestParam(defaultValue = "5") int lotSize) {
        List<EligibleOrderLotDTO> lots = logisticsManagerService.getGroupedEligibleOrders(deliveryDate, lotSize);
        return ResponseEntity.ok(lots);
    }

    @Operation(
            summary = "Récupérer les chauffeurs disponibles",
            description = "Chauffeurs actifs. Avec deliveryDate (dd-MM-yyyy), exclut ceux déjà engagés "
                    + "(tournée ASSIGNEE ou EN_COURS ce jour). excludeTourId : ignorer cette tournée (modification)."
    )
    @GetMapping("/delivery-tours/available-drivers")
    public ResponseEntity<List<AvailableDriverDTO>> getAvailableDrivers(
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate deliveryDate,
            @RequestParam(required = false) Long excludeTourId) {
        List<AvailableDriverDTO> drivers = logisticsManagerService.getAvailableDrivers(deliveryDate, excludeTourId);
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
            description = "Met à jour les informations d'une tournée (véhicule, notes, liste des commandes)."
    )
    @PatchMapping("/delivery-tours/{tourId}")
    public ResponseEntity<?> updateDeliveryTour(
            @PathVariable Long tourId,
            @RequestBody @Valid UpdateDeliveryTourDTO dto) {

        try {
            logisticsManagerService.updateDeliveryTour(tourId, dto);
            return ResponseEntity.ok("Tournée de livraison mis à jour avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Retirer une commande d'une tournée", description = "Retire la commande de la tournée (tournée au statut ASSIGNEE uniquement).")
    @DeleteMapping("/delivery-tours/{tourId}/orders/{orderId}")
    public ResponseEntity<String> removeOrderFromTour(
            @PathVariable Long tourId,
            @PathVariable Long orderId) {
        logisticsManagerService.removeOrderFromTour(tourId, orderId);
        return ResponseEntity.ok("Commande retirée de la tournée");
    }

    @Operation(
            summary = "Annuler une tournée",
            description = "Annule une tournée assignée avant départ livreur (statut: ASSIGNEE → ANNULEE) "
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

    @Operation(
            summary = "Récupérer les statistiques des tournées",
            description = "Retourne le nombre de tournées par statut"
    )
    @GetMapping("/delivery-tours/stats")
    public ResponseEntity<DeliveryTourStatsDTO> getDeliveryTourStats() {
        DeliveryTourStatsDTO stats = logisticsManagerService.getDeliveryTourStats();
        return ResponseEntity.ok(stats);
    }

    // ============================================================================
    // 📋 GESTION DES RETOURS ET RÉCLAMATIONS UTILISATEURS
    // ============================================================================

    @Operation(
            summary = "Statistiques des retours et réclamations",
            description = "Pour le tableau de bord / page Gestion des retours : total réclamations, validées, rejetées, réintégrées au stock, montant total remboursé."
    )
    @GetMapping("/claims/stats")
    public ResponseEntity<ClaimStatsDTO> getClaimStats() {
        return ResponseEntity.ok(logisticsManagerService.getClaimStats());
    }

    /**
     * Liste paginée de toutes les réclamations utilisateurs (retours), avec recherche par référence commande ou nom client,
     * et filtre par statut : EN_ATTENTE, VALIDE, REJETE.
     */
    @Operation(
            summary = "Liste paginée des réclamations utilisateurs",
            description = "Toutes les réclamations (retours) avec recherche par référence commande ou nom client, et filtre par statut : EN_ATTENTE, VALIDE, REJETE."
    )
    @GetMapping("/claims")
    public ResponseEntity<ClaimListResponseDTO> getClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ClaimStatus status) {
        return ResponseEntity.ok(logisticsManagerService.getClaims(page, size, search, status));
    }

    /**
     * Détails complets d'une réclamation : commande, produit concerné, type de problème, commentaire,
     * statut, décision (réintégration/remboursement), motif de rejet éventuel.
     */
    @Operation(
            summary = "Détail d'une réclamation",
            description = "Détails complets d'une réclamation : commande, produit concerné, type de problème, commentaire, statut, décision (réintégration/remboursement), motif de rejet éventuel."
    )
    @GetMapping("/claims/{id}")
    public ResponseEntity<ClaimDetailDTO> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(logisticsManagerService.getClaimById(id));
    }

    /**
     * Valider une réclamation (retour) : décision du RL soit réintégration au stock (quantité remise en stock),
     * soit remboursement (montant obligatoire). La réclamation doit être en statut EN_ATTENTE.
     */
    @Operation(
            summary = "Valider une réclamation (retour)",
            description = "Décision du RL : soit réintégration au stock (quantité remise en stock), soit remboursement (montant obligatoire). La réclamation doit être en attente."
    )
    @PostMapping("/claims/{id}/validate")
    public ResponseEntity<String> validateClaim(
            @PathVariable Long id,
            @RequestBody @Valid ValidateClaimDTO dto) {
        logisticsManagerService.validateClaim(id, dto);
        return ResponseEntity.ok(
                dto.getDecisionType() == ClaimDecisionType.REINTEGRATION
                        ? "Retour enregistré - Produit réintégré au stock"
                        : "Retour enregistré - Remboursement enregistré");
    }

    /**
     * Rejeter une réclamation avec un motif obligatoire. La réclamation doit être en statut EN_ATTENTE.
     */
    @Operation(
            summary = "Rejeter une réclamation",
            description = "Rejette la réclamation avec un motif obligatoire. La réclamation doit être en attente."
    )
    @PostMapping("/claims/{id}/reject")
    public ResponseEntity<String> rejectClaim(
            @PathVariable Long id,
            @RequestBody @Valid RejectClaimDTO dto) {
        logisticsManagerService.rejectClaim(id, dto);
        return ResponseEntity.ok("Réclamation rejetée");
    }

    //-----------  Tableau de Bord ------------

    @Operation(summary = "KPIs tableau de bord RL", description = "Commandes en attente, en retard, tournées actives, livrées ce mois.")
    @GetMapping("/dashboard/kpis")
    public ResponseEntity<RLDashboardKpisDTO> getDashboardKpis() {
        return ResponseEntity.ok(logisticsManagerService.getDashboardKpis());
    }

    @Operation(summary = "Statut tournées", description = "Effectif par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) pour le graphique.")
    @GetMapping("/dashboard/statut-tournees")
    public ResponseEntity<StatutTourneesDTO> getStatutTournees() {
        return ResponseEntity.ok(logisticsManagerService.getStatutTournees());
    }

    @Operation(summary = "Commandes par jour (7 derniers jours)", description = "Pour chaque jour : date (dd/MM), nbCommandes. Alimente le graphique « Commandes par jour » du tableau de bord RL.")
    @GetMapping("/dashboard/commandes-par-jour")
    public ResponseEntity<List<CommandesParJourDTO>> getCommandesParJour() {
        return ResponseEntity.ok(logisticsManagerService.getCommandesParJour());
    }

    @Operation(
            summary = "Taux de retours par jour (7 derniers jours)",
            description = "Pour chaque jour : date (dd/MM), tauxPercent = (réclamations créées ce jour / commandes ce jour) × 100. Alimente le graphique « Taux de retours (%) » du tableau de bord RL."
    )
    @GetMapping("/dashboard/taux-retours-par-jour")
    public ResponseEntity<List<TauxRetoursParJourDTO>> getTauxRetoursParJour() {
        return ResponseEntity.ok(logisticsManagerService.getTauxRetoursParJour());
    }

    @Operation(summary = "Top 5 produits les plus commandés (en %)", description = "Pour le graphique « Produits les plus fréquents » sur la page Gestion des commandes. Retourne productName et usagePercent (30 derniers jours).")
    @GetMapping("/dashboard/top5-products-usage")
    public ResponseEntity<List<TopProductUsageDTO>> getTop5ProductUsage() {
        return ResponseEntity.ok(logisticsManagerService.getTop5ProductUsage());
    }

    @Operation(summary = "Commandes vs Livraisons (7 derniers jours)", description = "Pour chaque jour : date, commandesEnAttente, livraisons.")
    @GetMapping("/dashboard/commandes-vs-livraisons")
    public ResponseEntity<List<CommandesVsLivraisonsDayDTO>> getCommandesVsLivraisons() {
        return ResponseEntity.ok(logisticsManagerService.getCommandesVsLivraisons());
    }

    @Operation(summary = "Stocks - État global", description = "Effectifs : normal, sous seuil, critique (donut).")
    @GetMapping("/dashboard/stock-etat-global")
    public ResponseEntity<StockEtatGlobalDTO> getStockEtatGlobal() {
        return ResponseEntity.ok(logisticsManagerService.getStockEtatGlobal());
    }

    @Operation(summary = "Livraisons par jour (7 derniers jours)", description = "Pour chaque jour : date, nbLivrees, nbAssignes, nbEnAttente.")
    @GetMapping("/dashboard/livraisons-par-jour")
    public ResponseEntity<List<LivraisonParJourDTO>> getLivraisonsParJour() {
        return ResponseEntity.ok(logisticsManagerService.getLivraisonsParJour());
    }

}


