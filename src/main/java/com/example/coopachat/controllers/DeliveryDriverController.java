package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveredOrderDetailsDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;
import com.example.coopachat.dtos.documentTypes.SubmitDriverDocumentDTO;
import com.example.coopachat.dtos.documentTypes.DriverDocumentListItemDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.services.DeliveryDriver.DeliveryDriverService;
import com.example.coopachat.services.admin.AdminService;
import com.example.coopachat.services.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


/**
 * Contrôleur pour la gestion des actions du livreur
 * Regroupe toutes les fonctionnalités liées au rôle Livreur
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DELIVERY_DRIVER')")
@Tag(name = "Livreur", description = "API pour les actions du Livreur")
public class DeliveryDriverController {

    private final DeliveryDriverService deliveryDriverService;
    private final AdminService adminService;
    private final MinioService minioService;

    @Operation(
            summary = "Récupérer les informations personnelles du livreur",
            description = "Récupère les informations personnelles du livreur connecté"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/personal-info")
    public ResponseEntity<DriverPersonalInfoDTO> getPersonalInfo() {
        DriverPersonalInfoDTO personalInfo = deliveryDriverService.getPersonalInfo();
        return ResponseEntity.ok(personalInfo);
    }

    @Operation(
            summary = "Modifier les informations personnelles du livreur",
            description = "Met à jour uniquement le nom, prénom et téléphone du livreur connecté"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping("/personal-info")
    public ResponseEntity<String> updatePersonalInfo(@RequestBody DriverPersonalInfoDTO dto) {
        deliveryDriverService.updatePersonalInfo(dto);
        return ResponseEntity.ok("Informations personnelles mises à jour avec succès");
    }

    @Operation(summary = "Mon adresse", description = "Récupère l'adresse du livreur (formattedAddress + lat/long). Pas de mode ni isPrimary.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/address")
    public ResponseEntity<DriverAddressDTO> getMyAddress() {
        return ResponseEntity.ok(deliveryDriverService.getMyAddress());
    }

 
    @Operation(summary = "Modifier mon adresse", description = "Met à jour l'adresse du livreur (formattedAddress + lat/long, rempli par le mobile via Google Places).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping("/address")
    public ResponseEntity<String> updateMyAddress(@RequestBody @Valid DriverAddressDTO dto) {
        deliveryDriverService.updateMyAddress(dto);
        return ResponseEntity.ok("Adresse mise à jour");
    }

    @Operation(
            summary = "Mes livraisons",
            description = "Liste paginée des livraisons du livreur. Filtre : ALL | TO_CONFIRM | IN_PROGRESS | COMPLETED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/deliveries/{orderId}/details")
    public ResponseEntity<DeliveryDetailDTO> getDeliveryDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryDriverService.getDeliveryDetail(orderId));
    }

    @Operation(
            summary = "Détail complet commande livrée",
            description = "Détail complet (items, paiement, timeline, salarié). Uniquement si la commande est LIVREE et appartient au livreur."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/deliveries/{orderId}/delivered-details")
    public ResponseEntity<DriverDeliveredOrderDetailsDTO> getDeliveredOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryDriverService.getDeliveredOrderDetails(orderId));
    }

    @Operation(summary = "Confirmer la récupération au dépôt", description = "Le livreur confirme avoir récupéré les colis → tournée EN_COURS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/delivery-tours/{tourId}/orders/{orderId}/pickup")
    public ResponseEntity<String> confirmPickup(@PathVariable Long tourId, @PathVariable Long orderId) {
        deliveryDriverService.confirmPickup(tourId, orderId);
        return ResponseEntity.ok("Récupération confirmée");
    }

    @Operation(summary = "Lancer la livraison", description = "Le livreur part vers le client → commande EN_COURS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/start")
    public ResponseEntity<String> startDelivery(@PathVariable Long orderId) {
        deliveryDriverService.startDelivery(orderId);
        return ResponseEntity.ok("Livraison en cours");
    }

    @Operation(summary = "Confirmer l'arrivée", description = "Le livreur est sur place → commande ARRIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/arrive")
    public ResponseEntity<String> confirmArrival(@PathVariable Long orderId) {
        deliveryDriverService.confirmArrival(orderId);
        return ResponseEntity.ok("Arrivée confirmée");
    }

    @Operation(summary = "Finaliser la livraison", description = "Colis remis au client → commande LIVREE. Si toutes les commandes de la tournée sont livrées → tournée TERMINEE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/complete")
    public ResponseEntity<String> completeDelivery(@PathVariable Long orderId) {
        deliveryDriverService.completeDelivery(orderId);
        return ResponseEntity.ok("Livraison finalisée");
    }

    @Operation(
            summary = "Confirmer le paiement en espèces",
            description = "Le livreur confirme avoir reçu le montant en espèces du client (bouton \"Confirmer le paiement\"). Interface dédiée au livreur uniquement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/confirm-cash-payment")
    public ResponseEntity<String> confirmCashPayment(@PathVariable Long orderId) {
        deliveryDriverService.confirmCashPayment(orderId);
        return ResponseEntity.ok("Paiement en espèces confirmé");
    }

    @Operation(
            summary = "Confirmer le paiement en ligne",
            description = "Le livreur vérifie que le salarié a payé en ligne (bouton \"Confirmer paiement\"). Si status = PAID → succès, sinon erreur."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/confirm-online-payment")
    public ResponseEntity<String> confirmOnlinePayment(@PathVariable Long orderId) {
        deliveryDriverService.confirmOnlinePayment(orderId);
        return ResponseEntity.ok("Paiement en ligne confirmé");
    }

    @Operation(summary = "Raisons d'échec livraison (livreur)", description = "Liste des raisons pour le dropdown du formulaire « Signaler un problème » (id, name, description).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/delivery-issue-reasons")
    public ResponseEntity<List<ReferenceItemDTO>> getDeliveryIssueReasons() {
        return ResponseEntity.ok(deliveryDriverService.getDeliveryIssueReasons());
    }

    @Operation(
            summary = "Signaler un problème",
            description = "Le livreur soumet un signalement sur une ligne de commande. Le bouton apparaît en swipant sur l’article."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/deliveries/{orderId}/report-issue")
    public ResponseEntity<String> reportDeliveryIssue(@PathVariable Long orderId, @RequestBody @Valid DeliveryIssueDTO dto) {
        deliveryDriverService.reportDeliveryIssue(orderId, dto);
        return ResponseEntity.ok("Échec de livraison signalé");
    }

    @Operation(
            summary = "Modifier ma photo de profil",
            description = "Met à jour la photo de profil du livreur connecté. Accepte multipart/form-data, partie 'file' (JPEG, PNG, GIF, WebP, max 5 Mo)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping(value = "/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateMyProfilePhoto(@RequestParam("file") MultipartFile file) {
        adminService.updateProfilePhotoForCurrentUser(file);
        return ResponseEntity.ok("Photo de profil mise à jour");
    }
    @Operation(
            summary = "Tableau de bord",
            description = "Photo, nom, en ligne, véhicule | Livraisons aujourd'hui, total, gains, tarif/livraison, note | Graphique performances : mois en cours découpé en tranches de 7 jours (S1, S2, …)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<DriverDashboardDTO> getDashboard() {
        return ResponseEntity.ok(deliveryDriverService.getDashboard());
    }

    @Operation(
            summary = "Mes documents requis",
            description = "Liste des types de documents requis avec leur état de soumission actuel pour le livreur connecté."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/required-documents")
    public ResponseEntity<List<DriverDocumentListItemDTO>> getRequiredDocuments() {
        return ResponseEntity.ok(deliveryDriverService.getRequiredDocuments());
    }

    @Operation(
            summary = "Soumettre un document",
            description = "Envoie d'un document (recto/verso) pour validation par l'administration. Formats acceptés: JPG, PNG."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping(value = "/documents/submit", consumes = "multipart/form-data")
    public ResponseEntity<String> submitDocument(
            @Parameter(description = "ID du type de document", required = true)
            @RequestParam Long documentTypeId,

            @Parameter(description = "Fichier (recto)", required = true)
            @RequestParam MultipartFile file,

            @Parameter(description = "Fichier (verso)")
            @RequestParam(required = false) MultipartFile fileVerso,

            @Parameter(description = "Date d'expiration (dd-mm-yyyy)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate expirationDate,

            @Parameter(description = "Date de délivrance (dd-mm-yyyy)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate issueDate
    ) {
        try {
            // 1. Validation des formats de fichiers resto et verso si fournit
            validateFileFormat(file);
            if (fileVerso != null && !fileVerso.isEmpty()) {
                validateFileFormat(fileVerso);
            }

            // 2. Upload des fichiers
            String fileUrl = minioService.uploadFile(file, "driver-documents");
            String fileVersoUrl = null;
            if (fileVerso != null && !fileVerso.isEmpty()) {
                fileVersoUrl = minioService.uploadFile(fileVerso, "driver-documents");
            }

            // 3. Créer le DTO
            SubmitDriverDocumentDTO dto = new SubmitDriverDocumentDTO();
            dto.setDocumentTypeId(documentTypeId);
            dto.setFileUrl(fileUrl);
            dto.setFileVersoUrl(fileVersoUrl);
            dto.setIssueDate(issueDate);
            dto.setExpirationDate(expirationDate);

            // 4. Appeler le service
            deliveryDriverService.submitDocument(dto);

            return ResponseEntity.ok("Document soumis avec succès. Il est en attente de validation.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de la soumission : " + e.getMessage());
        }
    }

    /**
     * Valide que le fichier est au format JPG ou PNG.
     */
    private void validateFileFormat(MultipartFile file) {
        //on récupère le nom original de l'image
        String originalFilename = file.getOriginalFilename();
        //si le nom original de l'image n'est pas null
        if (originalFilename != null) {
            //on récupère l'extension de l'image
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                throw new IllegalArgumentException("Format de fichier non supporté pour " + originalFilename + ". Formats acceptés: JPG, PNG");
            }
        }
    }
}
