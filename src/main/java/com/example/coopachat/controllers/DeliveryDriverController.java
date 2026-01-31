package com.example.coopachat.controllers;

import com.example.coopachat.dtos.DeliveryDriver.DeliveryDriverPreferenceDTO;
import com.example.coopachat.dtos.DeliveryDriver.DeliveryZoneDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.services.DeliveryDriver.DeliveryDriverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            summary = "Récupérer les disponibilités du livreur",
            description = "Récupère les jours et créneaux  du livreur connecté"
    )
    @GetMapping("/availability")
    public ResponseEntity<DeliveryDriverPreferenceDTO> getAvailabilityPreference() {
        DeliveryDriverPreferenceDTO availability = deliveryDriverService.getAvailabilityPreference();
        return ResponseEntity.ok(availability);
    }

    @Operation(
            summary = "Sauvegarder ou modifier les disponibilités du livreur",
            description = "Crée ou met à jour les disponibilités du livreur. " +
                    "Pour une modification, il faut envoyer TOUS les champs (jours, créneau), " +
                    "même ceux qui ne changent pas."
    )
    @PostMapping("/availability")
    public ResponseEntity<String> saveAvailabilityPreference(@RequestBody DeliveryDriverPreferenceDTO dto) {
        deliveryDriverService.saveAvailabilityPreference(dto);
        return ResponseEntity.ok("Disponibilités sauvegardées avec succès");
    }
    // ============================================================================
    // 🗺️ ZONES DE LIVRAISON
    // ============================================================================

    @Operation(
            summary = "Récupérer les zones de livraison du livreur",
            description = "Retourne toutes les zones de livraison configurées par le livreur connecté"
    )
    @GetMapping("/zones")
    public ResponseEntity<DeliveryZoneDTO> getDeliveryZones() {
        DeliveryZoneDTO zones = deliveryDriverService.getAllZones();
        return ResponseEntity.ok(zones);
    }

    @Operation(
            summary = "Sauvegarder ou modifier les zones de livraison",
            description = "Crée ou met à jour les zones de livraison du livreur. " +
                    "Pour une modification, il faut envoyer TOUTES les zones, " +
                    "même celles qui ne changent pas."
    )
    @PostMapping("/zones")
    public ResponseEntity<String> saveDeliveryZones(@RequestBody DeliveryZoneDTO dto) {
        deliveryDriverService.saveZones(dto);
        return ResponseEntity.ok("Zones de livraison sauvegardées avec succès");
    }
}
