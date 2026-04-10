package com.example.coopachat.controllers;

import com.example.coopachat.dtos.companies.CreateCompanyDTO;
import com.example.coopachat.dtos.companies.CompanyListResponseDTO;
import com.example.coopachat.dtos.companies.CompanyDetailsDTO;
import com.example.coopachat.dtos.companies.CompanyStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.commercial.CommercialDashboardKpisDTO;
import com.example.coopachat.dtos.companies.UpdateCompanyDTO;
import com.example.coopachat.dtos.companies.ProspectStatsDTO;
import com.example.coopachat.dtos.companies.UpdateCompanyStatusDTO;
import com.example.coopachat.dtos.coupons.CartTotalCouponStatsDTO;
import com.example.coopachat.dtos.coupons.CouponDetailsDTO;
import com.example.coopachat.dtos.coupons.CouponListResponseDTO;
import com.example.coopachat.dtos.coupons.CreateCouponDTO;
import com.example.coopachat.dtos.coupons.IdNameDTO;
import com.example.coopachat.dtos.coupons.UpdateCouponStatusDTO;
import com.example.coopachat.dtos.employees.CreateEmployeeDTO;
import com.example.coopachat.dtos.employees.EmployeeDetailsDTO;
import com.example.coopachat.dtos.employees.EmployeeListResponseDTO;
import com.example.coopachat.dtos.employees.EmployeeStatsDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeStatusDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.dtos.promotions.CreatePromotionDTO;
import com.example.coopachat.dtos.promotions.PromotionDetailsDTO;
import com.example.coopachat.dtos.promotions.PromotionListResponseDTO;
import com.example.coopachat.dtos.promotions.PromotionStatsDTO;
import com.example.coopachat.enums.CompanyStatus;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.services.commercial.CommercialService;
import com.example.coopachat.services.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    private final MinioService minioService;

    // ============================================================================
    // 🏢 GESTION DES ENTREPRISES
    // ============================================================================

    @Operation(summary = "Lister les secteurs d'activité", description = "Référentiel pour formulaires et filtres (lecture seule).")
    @GetMapping("/company-sectors")
    public ResponseEntity<List<ReferenceItemDTO>> getCompanySectors() {
        return ResponseEntity.ok(commercialService.getCompanySectors());
    }

    @Operation(
            summary = "Créer une entreprise",
            description = "Création via multipart/form-data. Tous les champs en request param. Logo optionnel (JPG, PNG, max 5MB)."
    )
    @PostMapping(value = "/companies", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createCompany(
            @Parameter(description = "Nom de l'entreprise", required = true)
            @RequestParam String name,

            @Parameter(description = "Localisation (adresse ou région)", required = true)
            @RequestParam String location,

            @Parameter(description = "Nom du contact", required = true)
            @RequestParam String contactName,

            @Parameter(description = "Téléphone du contact", required = true)
            @RequestParam String contactPhone,

            @Parameter(description = "Statut de prospection (ex. En attente, Partenaire signé)", required = true)
            @RequestParam String status,

            @Parameter(description = "ID du secteur d'activité (référentiel GET /api/admin/company-sectors)")
            @RequestParam(required = false) Long sectorId,

            @Parameter(description = "Email du contact")
            @RequestParam(required = false) String contactEmail,

            @Parameter(description = "Note ou commentaire")
            @RequestParam(required = false) String note,

            @Parameter(description = "Logo (JPG, PNG, max 5MB). Si fourni, enregistré avec l'entreprise.")
            @RequestParam(required = false) MultipartFile logo) {
        try {
            String logoFileName = "";
            if (logo != null && !logo.isEmpty()) {
                String originalFilename = logo.getOriginalFilename();
                if (originalFilename != null) {
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Format d'image non supporté. Formats acceptés: JPG, PNG");
                    }
                }
                String uploaded = minioService.uploadFile(logo, "companies");
                logoFileName = uploaded != null ? uploaded : "";
            }

            CreateCompanyDTO dto = new CreateCompanyDTO();
            dto.setName(name != null ? name.trim() : "");
            dto.setLocation(location != null ? location.trim() : "");
            dto.setContactName(contactName != null ? contactName.trim() : "");
            dto.setContactPhone(contactPhone != null ? contactPhone.trim() : "");
            dto.setStatus(CompanyStatus.fromLabelOrName(status));
            dto.setSectorId(sectorId);
            dto.setContactEmail(contactEmail != null && !contactEmail.isBlank() ? contactEmail.trim() : null);
            dto.setNote(note != null && !note.isBlank() ? note.trim() : null);
            dto.setLogo(logoFileName);

            commercialService.createCompany(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Entreprise créée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload du logo: " + e.getMessage());
        }
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
            summary = "Lister les prospects uniquement (paginé)",
            description = "Retourne uniquement les prospects (status != Partenaire signé). Filtres : search, sector, prospectionStatus."
    )
    @GetMapping("/prospects")
    public ResponseEntity<CompanyListResponseDTO> getProspects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) com.example.coopachat.enums.CompanyStatus prospectionStatus
    ) {
        CompanyListResponseDTO response = commercialService.getProspectsOnly(page, size, search, sectorId, prospectionStatus);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Statistiques prospections",
            description = "Total prospects + comptage par statut (En attente, Relancé, Intéressé, Signé)."
    )
    @GetMapping("/prospects/stats")
    public ResponseEntity<ProspectStatsDTO> getProspectStats() {
        return ResponseEntity.ok(commercialService.getProspectStats());
    }

    @Operation(
            summary = "Statistiques entreprises partenaires",
            description = "Total partenaires + actives + inactives (uniquement status = Partenaire signé)."
    )
    @GetMapping("/partners/stats")
    public ResponseEntity<CompanyStatsDTO> getPartnerStats() {
        return ResponseEntity.ok(commercialService.getPartnerStats());
    }

    @Operation(
            summary = "KPIs du tableau de bord commercial",
            description = "Retourne les indicateurs : totalSalaries, nouveauxSalariesCeMois, commandesCeMois, " +
                         "evolutionCommandesPct, ventesCeMois, evolutionVentesPct, promotionsActives. " +
                         "Données limitées au périmètre du commercial connecté."
    )
    @GetMapping("/dashboard/kpis")
    public ResponseEntity<CommercialDashboardKpisDTO> getDashboardKpis() {
        return ResponseEntity.ok(commercialService.getDashboardKpis());
    }

    @Operation(
            summary = "Coupons utilisés par jour (7 derniers jours)",
            description = "Pour le graphique « Tendance des coupons utilisés » du tableau de bord commercial."
    )
    @GetMapping("/dashboard/coupons-utilises-par-jour")
    public ResponseEntity<List<CouponUsageParJourDTO>> getCouponsUtilisesParJour() {
        return ResponseEntity.ok(commercialService.getCouponsUtilisesParJour());
    }

    @Operation(
            summary = "Lister les entreprises partenaires uniquement (paginé)",
            description = "Retourne uniquement les entreprises partenaires (status = Partenaire signé). Filtres : search, sector, isActive."
    )
    @GetMapping("/companies")
    public ResponseEntity<CompanyListResponseDTO> getCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) Boolean isActive
    ) {
        CompanyListResponseDTO response = commercialService.getCompaniesOnly(page, size, search, sectorId, isActive);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Derniers prospects", description = "Récupère les N derniers prospects (entreprises non partenaires).")
    @GetMapping("/companies/last-prospects")
    public ResponseEntity<java.util.List<com.example.coopachat.dtos.companies.CompanyListItemDTO>> getLastProspects(
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(commercialService.getLastProspects(limit));
    }

    @Operation(
            summary = "Récupérer les détails d'une entreprise",
            description = "Récupère les détails complets d'une entreprise spécifique par son ID. " +
                         "Tout commercial connecté peut consulter l'entreprise."
    )
    @GetMapping("/companies/{id}")
    public ResponseEntity<CompanyDetailsDTO> getCompanyById(@PathVariable Long id) {
        CompanyDetailsDTO companyDetails = commercialService.getCompanyById(id);
        return ResponseEntity.ok(companyDetails);
    }

    @Operation(
            summary = "Modifier une entreprise",
            description = "Met à jour les informations d'une entreprise existante. " +
                         "Vivier partagé : tout commercial connecté peut modifier l'entreprise. " +
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
                         "Vivier partagé : tout commercial connecté peut agir sur l'entreprise. " +
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

    @Operation(
            summary = "Supprimer le logo d'une entreprise",
            description = "Retire le logo associé à l'entreprise."
    )
    @DeleteMapping("/companies/{id}/logo")
    public ResponseEntity<String> deleteCompanyLogo(@PathVariable Long id) {
        commercialService.deleteCompanyLogo(id);
        return ResponseEntity.ok("Logo supprimé");
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
            summary = "Importer des salariés via Excel",
            description = "Multipart : partie 'file' (.xlsx) et paramètre 'companyId' (entreprise cible). " +
                         "Colonnes attendues : prénom, nom, email, téléphone, adresse."
    )
    @PostMapping(value = "/employees/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importEmployeesFromExcel(
            @Parameter(description = "Fichier Excel (.xlsx)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Identifiant de l'entreprise")
            @RequestParam Long companyId
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Le fichier est obligatoire et ne doit pas être vide.");
        }
        try {
            commercialService.saveEmployeesFromMultipart(file, companyId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Import terminé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body("Erreur lors de l'import du fichier : " + e.getMessage());
        }
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
            summary = "Lister les salariés d'une entreprise (paginé avec recherche et filtres)",
            description = "Récupère la liste paginée des salariés de l'entreprise indiquée (vivier partagé entre commerciaux). " +
                         "Les paramètres 'page' (défaut: 0) et 'size' (défaut: 6) contrôlent la pagination. " +
                         "'companyId' est obligatoire. 'search' (prénom ou nom) et 'isActive' (true/false) sont optionnels."
    )
    @GetMapping("/employees")
    public ResponseEntity<EmployeeListResponseDTO> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam Long companyId,
            @RequestParam(required = false) Boolean isActive
    ) {
        EmployeeListResponseDTO response = commercialService.getAllEmployees(page, size, search, companyId, isActive);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un salarié",
            description = "Récupère les détails complets d'un salarié par son ID (vivier partagé entre commerciaux)."
    )
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDetailsDTO> getEmployeeById(@PathVariable Long id) {
        EmployeeDetailsDTO employeeDetails = commercialService.getEmployeeById(id);
        return ResponseEntity.ok(employeeDetails);
    }

    @Operation(
            summary = "Modifier un salarié",
            description = "Met à jour les informations d'un salarié existant (vivier partagé entre commerciaux). " +
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
            description = "Active ou désactive un salarié (vivier partagé entre commerciaux). " +
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

    @Operation(summary = "Créer un coupon", description = "Code promo panier (réduction sur le total). Nom, code, type % ou F CFA, valeur, dates.")
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
            summary = "Statistiques coupons panier (CART_TOTAL)",
            description = "Retourne le nombre de coupons actifs (scope CART_TOTAL) et le nombre total d'utilisations. Utilisé pour les cartes « Coupons actives » et « Utilisations totales »."
    )
    @GetMapping("/coupons/cart-total-stats")
    public ResponseEntity<CartTotalCouponStatsDTO> getCartTotalCouponStats() {
        CartTotalCouponStatsDTO stats = commercialService.getCartTotalCouponStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Liste des produits actifs pour création de coupon", description = "Retourne id et nom des produits actifs, triés par nom.")
    @GetMapping("/coupons/products")
    public ResponseEntity<List<IdNameDTO>> getActiveProductsForCoupon() {
        return ResponseEntity.ok(commercialService.getActiveProductsForCoupon());
    }

    @Operation(summary = "Liste des catégories pour création de coupon", description = "Retourne id et nom des catégories.")
    @GetMapping("/coupons/categories")
    public ResponseEntity<List<IdNameDTO>> getCategoriesForCoupon() {
        return ResponseEntity.ok(commercialService.getCategoriesForCoupon());
    }

    @Operation(
            summary = "Lister les coupons (paginé avec recherche et filtres)",
            description = "Filtres optionnels: search, status, isActive. Coupons = codes promo panier."
    )
    @GetMapping("/coupons")
    public ResponseEntity<CouponListResponseDTO> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) Boolean isActive
    ) {
        CouponListResponseDTO response = commercialService.getAllCoupons(page, size, search, status, isActive);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un coupon",
            description = " Inclut les produits/catégories liés selon le scope\n" +
                    " Pour CART_TOTAL : inclut le nombre d'utilisations"
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

    // ============================================================================
    // 🏷️ GESTION DES PROMOTIONS
    // ============================================================================

    @Operation(
            summary = "Lister les promotions (paginé avec recherche et filtre)",
            description = "Promotions = réductions en % sur des produits. Filtres optionnels: search (nom), status (PLANNED, ACTIVE, EXPIRED, DISABLED)."
    )
    @GetMapping("/promotions")
    public ResponseEntity<PromotionListResponseDTO> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CouponStatus status) {
        return ResponseEntity.ok(commercialService.getAllPromotions(page, size, search, status));
    }

    @Operation(summary = "Statistiques des promotions", description = "Total, actives, planifiées, expirées, désactivées, nombre de produits concernés.")
    @GetMapping("/promotions/stats")
    public ResponseEntity<PromotionStatsDTO> getPromotionStats() {
        return ResponseEntity.ok(commercialService.getPromotionStats());
    }

    @Operation(summary = "Détails d'une promotion", description = "Nom, dates, statut, liste des produits avec réduction %.")
    @GetMapping("/promotions/{id}")
    public ResponseEntity<PromotionDetailsDTO> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(commercialService.getPromotionById(id));
    }

    @Operation(summary = "Activer/Désactiver une promotion", description = "Comme pour les coupons. Body: { \"isActive\": true/false }.")
    @PatchMapping("/promotions/{id}/status")
    public ResponseEntity<String> updatePromotionStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCouponStatusDTO updateCouponStatusDTO) {
        commercialService.updatePromotionStatus(id, updateCouponStatusDTO);
        String message = updateCouponStatusDTO.getIsActive()
                ? "Promotion activée avec succès"
                : "Promotion désactivée avec succès";
        return ResponseEntity.ok(message);
    }

    @Operation(summary = "Liste des produits pour création de promotion", description = "Produits actifs (id, name). Optionnel : categoryId pour filtrer par catégorie.")
    @GetMapping("/promotions/products")
    public ResponseEntity<List<IdNameDTO>> getProductsForPromotion(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(commercialService.getProductsForPromotion(categoryId));
    }

    @Operation(
            summary = "Créer une promotion",
            description = "Promotion = réductions en % sur une liste de produits. Nom, dates, productItems (productId, discountValue en %). Au moins un produit obligatoire."
    )
    @PostMapping("/promotions")
    public ResponseEntity<String> createPromotion(@RequestBody @Valid CreatePromotionDTO createPromotionDTO) {
        try {
            commercialService.addPromotion(createPromotionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Promotion créée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}


