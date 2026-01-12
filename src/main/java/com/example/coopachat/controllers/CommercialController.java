package com.example.coopachat.controllers;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.dtos.CompanyListResponseDTO;
import com.example.coopachat.dtos.CompanyDetailsDTO;
import com.example.coopachat.dtos.CompanyStatsDTO;
import com.example.coopachat.dtos.UpdateCompanyDTO;
import com.example.coopachat.dtos.UpdateCompanyStatusDTO;
import com.example.coopachat.dtos.EmployeeListResponseDTO;
import com.example.coopachat.dtos.EmployeeStatsDTO;
import com.example.coopachat.dtos.EmployeeDetailsDTO;
import com.example.coopachat.dtos.UpdateEmployeeDTO;
import com.example.coopachat.enums.CompanySector;
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
            summary = "Récupérer les statistiques des entreprises",
            description = "Récupère les statistiques des entreprises du commercial connecté " +
                         "(total, actives, inactives)."
    )
    @GetMapping("/companies/stats")
    public ResponseEntity<CompanyStatsDTO> getCompanyStats() {
        CompanyStatsDTO stats = commercialService.getCompanyStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Lister les entreprises (paginé avec recherche et filtres)",
            description = "Récupère la liste paginée de toutes les entreprises créées par le commercial connecté. " +
                         "Les paramètres 'page' (défaut: 0) et 'size' (défaut: 6) permettent de contrôler la pagination. " +
                         "Les paramètres 'search' (recherche par nom), 'sector' (filtre par secteur) et 'isActive' (filtre actif/inactif: true/false) sont optionnels."
    )
    @GetMapping("/companies")
    public ResponseEntity<CompanyListResponseDTO> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CompanySector sector,
            @RequestParam(required = false) Boolean isActive
    ) {
        CompanyListResponseDTO response = commercialService.getAllCompanies(page, size, search, sector, isActive);
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

    @Operation(
            summary = "Modifier une entreprise",
            description = "Met à jour les informations d'une entreprise existante. " +
                         "L'entreprise doit appartenir au commercial connecté. " +
                         "Les champs id, companyCode, createdAt et commercial ne peuvent pas être modifiés."
    )
    @PutMapping("/companies/{id}")
    public ResponseEntity<String> updateCompany(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyDTO updateCompanyDTO
    ) {
        commercialService.updateCompany(id, updateCompanyDTO);
        return ResponseEntity.ok("Entreprise modifiée avec succès");
    }

    @Operation(
            summary = "Activer/Désactiver une entreprise",
            description = "Active ou désactive une entreprise. " +
                         "L'entreprise doit appartenir au commercial connecté. " +
                         "Le body doit contenir 'isActive' (true pour activer, false pour désactiver)."
    )
    @PatchMapping("/companies/{id}/status")
    public ResponseEntity<String> updateCompanyStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyStatusDTO updateCompanyStatusDTO
    ) {
        commercialService.updateCompanyStatus(id, updateCompanyStatusDTO);
        
        //message = entreprise activée avec succès si isActive = true, entreprise désactivée avec succès si isActive = false
        String message = updateCompanyStatusDTO.getIsActive() 
                ? "Entreprise activée avec succès" 
                : "Entreprise désactivée avec succès";
        
        return ResponseEntity.ok(message);
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

    @Operation(
            summary = "Récupérer les statistiques des salariés",
            description = "Récupère les statistiques des salariés du commercial connecté " +
                         "(total, actifs, en attente d'activation)."
    )
    @GetMapping("/employees/stats")
    public ResponseEntity<EmployeeStatsDTO> getEmployeeStats() {
        EmployeeStatsDTO stats = commercialService.getEmployeeStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Lister les salariés (paginé avec recherche et filtres)",
            description = "Récupère la liste paginée de tous les salariés créés par le commercial connecté. " +
                         "Les paramètres 'page' (défaut: 0) et 'size' (défaut: 6) permettent de contrôler la pagination. " +
                         "Les paramètres 'search' (recherche par prénom ou nom), 'companyId' (filtre par entreprise) et 'isActive' (filtre actif/inactif: true/false) sont optionnels."
    )
    @GetMapping("/employees")
    public ResponseEntity<EmployeeListResponseDTO> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean isActive
    ) {
        EmployeeListResponseDTO response = commercialService.getAllEmployees(page, size, search, companyId, isActive);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un salarié",
            description = "Récupère les détails complets d'un salarié spécifique par son ID. " +
                         "Le salarié doit appartenir au commercial connecté."
    )
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDetailsDTO> getEmployeeById(@PathVariable Long id) {
        EmployeeDetailsDTO employeeDetails = commercialService.getEmployeeById(id);
        return ResponseEntity.ok(employeeDetails);
    }

    @Operation(
            summary = "Modifier un salarié",
            description = "Met à jour les informations d'un salarié existant. " +
                         "Le salarié doit appartenir au commercial connecté. " +
                         "Les champs id, employeeCode, createdAt et createdBy ne peuvent pas être modifiés. " +
                         "Si l'email ou le téléphone est modifié, il sera vérifié qu'il n'existe pas déjà."
    )
    @PutMapping("/employees/{id}")
    public ResponseEntity<String> updateEmployee(
            @PathVariable Long id,
            @RequestBody @Valid UpdateEmployeeDTO updateEmployeeDTO
    ) {
        commercialService.updateEmployee(id, updateEmployeeDTO);
        return ResponseEntity.ok("Salarié modifié avec succès");
    }

}

