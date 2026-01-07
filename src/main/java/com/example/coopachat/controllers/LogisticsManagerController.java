package com.example.coopachat.controllers;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.services.LogisticsManagerService;
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
}

