package com.example.coopachat.controllers;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.services.DeliveryDriver.DeliveryDriverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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


}
