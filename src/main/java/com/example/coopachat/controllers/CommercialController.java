package com.example.coopachat.controllers;

import com.example.coopachat.dtos.companies.CreateCompanyDTO;
import com.example.coopachat.dtos.companies.CompanyListResponseDTO;
import com.example.coopachat.dtos.companies.CompanyDetailsDTO;
import com.example.coopachat.dtos.companies.CompanyStatsDTO;
import com.example.coopachat.dtos.companies.UpdateCompanyDTO;
import com.example.coopachat.dtos.companies.UpdateCompanyStatusDTO;
import com.example.coopachat.dtos.coupons.CouponListResponseDTO;
import com.example.coopachat.dtos.coupons.CouponDetailsDTO;
import com.example.coopachat.dtos.coupons.CreateCouponDTO;
import com.example.coopachat.dtos.coupons.UpdateCouponStatusDTO;
import com.example.coopachat.dtos.employees.CreateEmployeeDTO;
import com.example.coopachat.dtos.employees.EmployeeDetailsDTO;
import com.example.coopachat.dtos.employees.EmployeeListResponseDTO;
import com.example.coopachat.dtos.employees.EmployeeStatsDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeStatusDTO;
import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.services.commercial.CommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur pour la gestion des actions du commercial
 * Regroupe toutes les fonctionnalités liées au rôle Commercial
 */
@RestController
@RequestMapping("/api/commercial")
@RequiredArgsConstructor
@Tag(name = "Commercial", description = "API pour la gestion des actions du commercial ")
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

    @Operation(
            summary = "Téléverser le logo d'une entreprise",
            description = "Enregistre ou remplace le logo d'une entreprise. Formats acceptés: JPG, PNG. Taille max 5 Mo."
    )
    @PostMapping("/companies/{id}/logo")
    public ResponseEntity<String> uploadCompanyLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        commercialService.uploadCompanyLogo(id, file);
        return ResponseEntity.ok("Logo enregistré avec succès");
    }

    // ============================================================================
    // 👤 GESTION DES EMPLOYÉS
    // ============================================================================

    @Operation(
            summary = "Créer un nouveau salarié",
            description = "Permet à un commercial d'ajouter un nouveau salarié à une entreprise. " +
                    "Le salarié pourra ensuite activer son compte via le flux mobile."
    )
    @PostMapping("/employees")
    public ResponseEntity<String> createEmployee(@RequestBody @Valid CreateEmployeeDTO createEmployeeDTO) {
        commercialService.createEmployee(createEmployeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Salarié créé avec succès.");
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

    @Operation(
            summary = "Activer/Désactiver un salarié",
            description = "Active ou désactive un salarié. " +
                         "Le salarié doit appartenir au commercial connecté. " +
                         "Le body doit contenir 'isActive' (true pour activer, false pour désactiver)."
    )
    @PatchMapping("/employees/{id}/status")
    public ResponseEntity<String> updateEmployeeStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateEmployeeStatusDTO updateEmployeeStatusDTO
    ) {
        commercialService.updateEmployeeStatus(id, updateEmployeeStatusDTO);
        String message = updateEmployeeStatusDTO.getIsActive()
                ? "Salarié activé avec succès"
                : "Salarié désactivé avec succès";
        return ResponseEntity.ok(message);
    }

    // ============================================================================
    // 🏷️ GESTION DES COUPONS
    // ============================================================================

    @Operation(
            summary = "Créer un coupon",
            description = "Permet à un commercial de créer un coupon"
    )
    @PostMapping("/coupons")
    public ResponseEntity<String> createCoupon(@RequestBody @Valid CreateCouponDTO createCouponDTO) {
        commercialService.addCoupon(createCouponDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Coupon créé avec succès");
    }


    @Operation(
            summary = "Activer/Désactiver un coupon",
            description = "Active ou désactive un coupon. Le body doit contenir 'isActive' (true/false)."
    )
    @PatchMapping("/coupons/{id}/status")
    public ResponseEntity<String> updateCouponStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCouponStatusDTO updateCouponStatusDTO
    ) {
        commercialService.updateCouponStatus(id, updateCouponStatusDTO);
        String message = updateCouponStatusDTO.getIsActive()
                ? "Coupon activé avec succès"
                : "Coupon désactivé avec succès";
        return ResponseEntity.ok(message);
    }

    @Operation(
            summary = "Lister les coupons (paginé avec recherche et filtres)",
            description = "Récupère la liste paginée de tous les coupons. " +
                    "Paramètres: page (défaut 0), size (défaut 6), search (code ou nom), " +
                    "status, scope, isActive (optionnels)."
    )
    @GetMapping("/coupons")
    public ResponseEntity<CouponListResponseDTO> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) CouponScope scope,
            @RequestParam(required = false) Boolean isActive
    ) {
        CouponListResponseDTO response = commercialService.getAllCoupons(page, size, search, status, scope, isActive);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un coupon",
            description = "Récupère les détails d'un coupon par son ID, avec la liste des produits liés."
    )
    @GetMapping("/coupons/{id}")
    public ResponseEntity<CouponDetailsDTO> getCouponById(@PathVariable Long id) {
        CouponDetailsDTO details = commercialService.getCouponById(id);
        return ResponseEntity.ok(details);
    }

    @Operation(
            summary = "Supprimer un coupon",
            description = "Supprime un coupon après avoir délié les produits et catégories associés."
    )
    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<String> deleteCoupon(@PathVariable Long id) {
        commercialService.deleteCoupon(id);
        return ResponseEntity.ok("Coupon supprimé avec succès");
    }

}


