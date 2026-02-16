package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.DriverDeliveryListItemDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.services.DeliveryDriver.DeliveryDriverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


/**
 * Contrôleur pour la gestion des actions du livreur
 * Regroupe toutes les fonctionnalités liées au rôle Livreur
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver")
public class DeliveryDriverController {

    private final DeliveryDriverService deliveryDriverService;

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

    @Operation(
            summary = "Mes livraisons",
            description = "Liste des livraisons du livreur connecté (commandes de ses tournées). Filtres optionnels : date, statut, recherche par numéro ou nom client."
    )
    @GetMapping("/deliveries")
    public ResponseEntity<List<DriverDeliveryListItemDTO>> getMyDeliveries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {
        List<DriverDeliveryListItemDTO> list = deliveryDriverService.getMyDeliveries(deliveryDate, status, search);
        return ResponseEntity.ok(list);
    }
}
