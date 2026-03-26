package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveredOrderDetailsDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.services.DeliveryDriver.DeliveryDriverService;
import com.example.coopachat.services.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * Contrôleur pour la gestion des actions du livreur
 * Regroupe toutes les fonctionnalités liées au rôle Livreur
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver")
@Tag(name = "Livreur", description = "API pour les actions du Livreur")
public class DeliveryDriverController {

    private final DeliveryDriverService deliveryDriverService;
    private final AdminService adminService;

    @Operation(
            summary = "Récupérer les informations personnelles du livreur",
            description = "Récupère les informations personnelles du livreur connecté"
    )
    @GetMapping("/personal-info")
    public ResponseEntity<DriverPersonalInfoDTO> getPersonalInfo() {
        DriverPersonalInfoDTO personalInfo = deliveryDriverService.getPersonalInfo();
        return ResponseEntity.ok(personalInfo);
    }

    @Operation(
            summary = "Modifier les informations personnelles du livreur",
            description = "Met à jour uniquement le nom, prénom et téléphone du livreur connecté"
    )
    @PutMapping("/personal-info")
    public ResponseEntity<String> updatePersonalInfo(@RequestBody DriverPersonalInfoDTO dto) {
        deliveryDriverService.updatePersonalInfo(dto);
        return ResponseEntity.ok("Informations personnelles mises à jour avec succès");
    }

    @Operation(summary = "Mon adresse", description = "Récupère l'adresse du livreur (formattedAddress + lat/long). Pas de mode ni isPrimary.")
    @GetMapping("/address")
    public ResponseEntity<DriverAddressDTO> getMyAddress() {
        return ResponseEntity.ok(deliveryDriverService.getMyAddress());
    }

 
    @Operation(summary = "Modifier mon adresse", description = "Met à jour l'adresse du livreur (formattedAddress + lat/long, rempli par le mobile via Google Places).")
    @PutMapping("/address")
    public ResponseEntity<String> updateMyAddress(@RequestBody @Valid DriverAddressDTO dto) {
        deliveryDriverService.updateMyAddress(dto);
        return ResponseEntity.ok("Adresse mise à jour");
    }

    @Operation(
            summary = "Mes livraisons",
            description = "Liste paginée des livraisons du livreur. Filtre : ALL | TO_CONFIRM | IN_PROGRESS | COMPLETED."
    )
    @GetMapping("/deliveries")
    public ResponseEntity<DriverDeliveriesResponseDTO> getMyDeliveries(
            @RequestParam(required = false, defaultValue = "ALL") String statusFilter,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(deliveryDriverService.getMyDeliveries(statusFilter, page, size));
    }

    @Operation(
            summary = "Détail d'une livraison",
            description = "Détail simplifié pour le livreur : commande, client, adresse, montant. La commande doit appartenir à une de ses tournées."
    )
    @GetMapping("/deliveries/{orderId}/details")
    public ResponseEntity<DeliveryDetailDTO> getDeliveryDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryDriverService.getDeliveryDetail(orderId));
    }

    @Operation(
            summary = "Détail complet commande livrée",
            description = "Détail complet (items, paiement, timeline, salarié). Uniquement si la commande est LIVREE et appartient au livreur."
    )
    @GetMapping("/deliveries/{orderId}/delivered-details")
    public ResponseEntity<DriverDeliveredOrderDetailsDTO> getDeliveredOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryDriverService.getDeliveredOrderDetails(orderId));
    }

    @Operation(summary = "Confirmer la récupération au dépôt", description = "Le livreur confirme avoir récupéré les colis → tournée EN_COURS.")
    @PostMapping("/delivery-tours/{tourId}/orders/{orderId}/pickup")
    public ResponseEntity<String> confirmPickup(@PathVariable Long tourId, @PathVariable Long orderId) {
        deliveryDriverService.confirmPickup(tourId, orderId);
        return ResponseEntity.ok("Récupération confirmée");
    }

    @Operation(summary = "Lancer la livraison", description = "Le livreur part vers le client → commande EN_COURS.")
    @PostMapping("/deliveries/{orderId}/start")
    public ResponseEntity<String> startDelivery(@PathVariable Long orderId) {
        deliveryDriverService.startDelivery(orderId);
        return ResponseEntity.ok("Livraison en cours");
    }

    @Operation(summary = "Confirmer l'arrivée", description = "Le livreur est sur place → commande ARRIVE.")
    @PostMapping("/deliveries/{orderId}/arrive")
    public ResponseEntity<String> confirmArrival(@PathVariable Long orderId) {
        deliveryDriverService.confirmArrival(orderId);
        return ResponseEntity.ok("Arrivée confirmée");
    }

    @Operation(summary = "Finaliser la livraison", description = "Colis remis au client → commande LIVREE. Si toutes les commandes de la tournée sont livrées → tournée TERMINEE.")
    @PostMapping("/deliveries/{orderId}/complete")
    public ResponseEntity<String> completeDelivery(@PathVariable Long orderId) {
        deliveryDriverService.completeDelivery(orderId);
        return ResponseEntity.ok("Livraison finalisée");
    }

    @Operation(
            summary = "Confirmer le paiement en espèces",
            description = "Le livreur confirme avoir reçu le montant en espèces du client (bouton \"Confirmer le paiement\"). Interface dédiée au livreur uniquement."
    )
    @PostMapping("/deliveries/{orderId}/confirm-cash-payment")
    public ResponseEntity<String> confirmCashPayment(@PathVariable Long orderId) {
        deliveryDriverService.confirmCashPayment(orderId);
        return ResponseEntity.ok("Paiement en espèces confirmé");
    }

    @Operation(
            summary = "Confirmer le paiement en ligne",
            description = "Le livreur vérifie que le salarié a payé en ligne (bouton \"Confirmer paiement\"). Si status = PAID → succès, sinon erreur."
    )
    @PostMapping("/deliveries/{orderId}/confirm-online-payment")
    public ResponseEntity<String> confirmOnlinePayment(@PathVariable Long orderId) {
        deliveryDriverService.confirmOnlinePayment(orderId);
        return ResponseEntity.ok("Paiement en ligne confirmé");
    }

    @Operation(summary = "Raisons d'échec livraison (livreur)", description = "Liste des raisons pour le dropdown du formulaire « Signaler un problème » (id, name, description).")
    @GetMapping("/delivery-issue-reasons")
    public ResponseEntity<List<ReferenceItemDTO>> getDeliveryIssueReasons() {
        return ResponseEntity.ok(deliveryDriverService.getDeliveryIssueReasons());
    }

    @Operation(
            summary = "Signaler un problème",
            description = "Le livreur soumet un signalement sur une ligne de commande. Le bouton apparaît en swipant sur l’article."
    )
    @PostMapping("/deliveries/{orderId}/report-issue")
    public ResponseEntity<String> reportDeliveryIssue(@PathVariable Long orderId, @RequestBody @Valid DeliveryIssueDTO dto) {
        deliveryDriverService.reportDeliveryIssue(orderId, dto);
        return ResponseEntity.ok("Échec de livraison signalé");
    }

    @Operation(
            summary = "Modifier ma photo de profil",
            description = "Met à jour la photo de profil du livreur connecté. Accepte multipart/form-data, partie 'file' (JPEG, PNG, GIF, WebP, max 5 Mo)."
    )
    @PutMapping(value = "/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateMyProfilePhoto(@RequestParam("file") MultipartFile file) {
        adminService.updateProfilePhotoForCurrentUser(file);
        return ResponseEntity.ok("Photo de profil mise à jour");
    }
    @Operation(
            summary = "Tableau de bord",
            description = "Photo, nom, en ligne, véhicule | Livraisons aujourd'hui, total, gains, tarif/livraison, note | Graphique performances. Filtre period : SEMAINE | MOIS | ANNEE (défaut : MOIS)."
    )
    @GetMapping("/dashboard")
    public ResponseEntity<DriverDashboardDTO> getDashboard(
            @RequestParam(required = false, defaultValue = "MOIS") String period) {
        return ResponseEntity.ok(deliveryDriverService.getDashboard(period));
    }
}
