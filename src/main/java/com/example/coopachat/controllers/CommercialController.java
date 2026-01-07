package com.example.coopachat.controllers;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.dtos.CompanyListResponseDTO;
import com.example.coopachat.dtos.CompanyDetailsDTO;
import com.example.coopachat.dtos.auth.ResetPasswordRequestDTO;
import com.example.coopachat.services.CommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion des actions du commercial
 * Regroupe toutes les fonctionnalités liées au rôle Commercial
 */
@RestController
@RequestMapping("/api/commercial")
@RequiredArgsConstructor
@Tag(name = "Commercial", description = "API pour la gestion des actions du commercial (entreprises et employés)")
public class CommercialController {

    private final CommercialService commercialService;

    // ============================================================================
    // 🏢 GESTION DES ENTREPRISES
    // ============================================================================

    @Operation(
            summary = "Créer une entreprise",
            description = "Permet à un commercial de créer une nouvelle entreprise. " +
                         "L'entreprise est automatiquement associée au commercial connecté."
    )
    @PostMapping("/companies")
    public ResponseEntity<String> createCompany(@RequestBody @Valid CreateCompanyDTO createCompanyDTO) {
        commercialService.createCompany(createCompanyDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Entreprise créée avec succès");
    }

    @Operation(
            summary = "Lister les entreprises (paginé)",
            description = "Récupère la liste paginée de toutes les entreprises créées par le commercial connecté. " +
                         "Les paramètres 'page' (défaut: 0) et 'size' (défaut: 6) permettent de contrôler la pagination."
    )
    @GetMapping("/companies")
    public ResponseEntity<CompanyListResponseDTO> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        CompanyListResponseDTO response = commercialService.getAllCompanies(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'une entreprise",
            description = "Récupère les détails complets d'une entreprise spécifique par son ID. " +
                         "L'entreprise doit appartenir au commercial connecté."
    )
    @GetMapping("/companies/{id}")
    public ResponseEntity<CompanyDetailsDTO> getCompanyById(@PathVariable Long id) {
        CompanyDetailsDTO companyDetails = commercialService.getCompanyById(id);
        return ResponseEntity.ok(companyDetails);
    }

    // ============================================================================
    // 👤 GESTION DES EMPLOYÉS
    // ============================================================================

    @Operation(
            summary = "Créer un nouveau salarié",
            description = "Permet à un commercial d'ajouter un nouveau salarié à une entreprise. " +
                    "Un email d'invitation avec un lien d'activation sera envoyé au salarié."
    )
    @PostMapping("/employees")
    public ResponseEntity<String> createEmployee(@RequestBody @Valid CreateEmployeeDTO createEmployeeDTO) {
        commercialService.createEmployee(createEmployeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Salarié créé avec succès. Un email d'invitation a été envoyé.");
    }


}

