package com.example.coopachat.controllers;

import com.example.coopachat.dtos.user.SaveUserDTO;
import com.example.coopachat.dtos.user.UpdateUserStatusDTO;
import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserListResponseDTO;
import com.example.coopachat.dtos.user.UserStatsByRoleItemDTO;
import com.example.coopachat.dtos.user.UserStatsByStatusItemDTO;
import com.example.coopachat.dtos.user.UserStatsDTO;
import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.fee.CreateFeeDTO;
import com.example.coopachat.dtos.fee.FeeDTO;
import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.categories.UpdateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.suppliers.CreateSupplierDTO;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminAlertsDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminDashboardStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.CommandesVsLivraisonsDayDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.services.admin.AdminService;
import com.example.coopachat.util.FileTransferUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour la gestion des actions de l'administrateur
 * Regroupe toutes les fonctionnalités liées au rôle Administrateur
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administrateur", description = "API pour la gestion des actions de l'administrateur")
public class AdminController {

    private final AdminService adminService;
    private final FileTransferUtil fileTransferUtil;

    // ============================================================================
    // 📁 GESTION DES CATÉGORIES (inspiré du catalogue produits)
    // ============================================================================

    @Operation(
            summary = "Créer une nouvelle catégorie",
            description = "Permet à un administrateur de créer une nouvelle catégorie. " +
                    "Le nom doit être unique. L'icon est optionnel (nom d'icône ou URL)."
    )
    @PostMapping("/categories")
    public ResponseEntity<String> createCategory(@RequestBody @Valid CreateCategoryDTO createCategoryDTO) {
        try {
            adminService.createCategory(createCategoryDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Catégorie créée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Lister les catégories",
            description = "Récupère la liste complète des catégories (id + nom + icon)."
    )
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryListItemDTO>> getAllCategories() {
        List<CategoryListItemDTO> categories = adminService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
            summary = "Récupérer les détails d'une catégorie",
            description = "Récupère une catégorie par son ID (id + nom + icon)."
    )
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryListItemDTO> getCategoryById(@PathVariable Long id) {
        CategoryListItemDTO category = adminService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @Operation(
            summary = "Modifier une catégorie",
            description = "Met à jour les informations d'une catégorie. " +
                    "Seuls les champs fournis (non null) sont mis à jour. " +
                    "Le nom doit rester unique si modifié."
    )
    @PutMapping("/categories/{id}")
    public ResponseEntity<String> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoryDTO dto) {
        try {
            adminService.updateCategory(id, dto);
            return ResponseEntity.ok("Catégorie mise à jour");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Upload une icône de catégorie",
            description = "Envoie un fichier image (SVG, PNG, etc.) pour l'icône d'une catégorie. " +
                    "Retourne le chemin relatif à utiliser dans le champ 'icon' (ex: uuid.svg), stocké directement dans files/."
    )
    @PostMapping("/categories/upload-icon")
    public ResponseEntity<?> uploadCategoryIcon(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichier requis");
        }
        // Même règle que produit : valider le format (produit = JPG/PNG ; icône = JPG, PNG, SVG)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png") && !extension.equals("svg")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Format non supporté. Formats acceptés: JPG, PNG, SVG");
            }
        }
        try {
            String relativePath = fileTransferUtil.handleFileUpload(file);
            return ResponseEntity.ok(java.util.Map.of("path", relativePath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    // ============================================================================
    // 📦 GESTION DES PRODUITS (Catalogue)
    // ============================================================================

    @Operation(
            summary = "Créer un nouveau produit",
            description = "Permet à un administrateur de créer un nouveau produit dans le catalogue. " +
                    "Le nom du produit doit être unique. L'image est optionnelle (formats acceptés: JPG, PNG, max 5MB)."
    )
    @PostMapping("/products")
    public ResponseEntity<String> createProduct(
            @Parameter(description = "Nom du produit", required = true)
            @RequestParam String name,

            @Parameter(description = "ID de la catégorie", required = true)
            @RequestParam Long categoryId,

            @Parameter(description = "Prix unitaire", required = true)
            @RequestParam BigDecimal price,

            @Parameter(description = "Stock initial", required = true)
            @RequestParam Integer currentStock,

            @Parameter(description = "Description du produit")
            @RequestParam(required = false) String description,

            @Parameter(description = "Image du produit (JPG, PNG, max 5MB)")
            @RequestParam(required = false) MultipartFile image,

            @Parameter(description = "Seuil minimum de réapprovisionnement")
            @RequestParam(required = false) Integer minThreshold,

            @Parameter(description = "Statut actif/inactif")
            @RequestParam(required = false) Boolean status
    ) {
        try {
            // 1. Upload de l'image si présente
            String imageFileName = "";
            if (image != null && !image.isEmpty()) {
                // Vérifier le format de l'image (JPG, PNG)
                //on récupère le nom original de l'image
                String originalFilename = image.getOriginalFilename();
                //si le nom original de l'image n'est pas null
                if (originalFilename != null) {
                    //on récupère l'extension de l'image
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Format d'image non supporté. Formats acceptés: JPG, PNG");
                    }
                }
                imageFileName = fileTransferUtil.handleFileUpload(image);
            }

            // 2. Créer le DTO avec tous les champs
            CreateProductDTO createProductDTO = new CreateProductDTO();
            createProductDTO.setName(name);
            createProductDTO.setCategoryId(categoryId);
            createProductDTO.setPrice(price);
            createProductDTO.setCurrentStock(currentStock);
            createProductDTO.setDescription(description);
            createProductDTO.setImage(imageFileName);
            createProductDTO.setMinThreshold(minThreshold != null ? minThreshold : 0);
            createProductDTO.setStatus(status != null ? status : false);

            // 3. Appeler le service
            adminService.createProduct(createProductDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Produit créé avec succès");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload de l'image: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @Operation(
            summary = "Lister les produits (paginé avec recherche et filtres)",
            description = "Récupère la liste paginée de tous les produits. " +
                    "Les paramètres 'page' (défaut: 0) et 'size' (défaut: 6) permettent de contrôler la pagination. " +
                    "Les paramètres 'search' (recherche par nom ou code produit), 'categoryId' (filtre par  catégorie) et 'status' (filtre actif/inactif: true/false) sont optionnels."
    )
    @GetMapping("/products")
    public ResponseEntity<ProductListResponseDTO> getAllProducts(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean status
    ) {
        ProductListResponseDTO response = adminService.getAllProducts(page, size, search, categoryId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un produit",
            description = "Récupère les détails complets d'un produit spécifique par son ID."
    )
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDetailsDTO> getProductById(
            @PathVariable Long id
    ) {
        ProductDetailsDTO productDetails = adminService.getProductById(id);
        return ResponseEntity.ok(productDetails);
    }

    @Operation(
            summary = "Modifier un produit",
            description = "Met à jour les informations d'un produit existant. " +
                    "Tous les champs sont optionnels - seuls les champs fournis seront mis à jour. " +
                    "L'image est optionnelle (formats acceptés: JPG, PNG, max 5MB). " +
                    "Le nom du produit doit être unique si modifié."
    )
    @PutMapping("/products/{id}")
    public ResponseEntity<String> updateProduct(

            @Parameter(description = "ID du produit à modifier")
            @PathVariable Long id,

            @Parameter(description = "Nom du produit")
            @RequestParam(required = false) String name,

            @Parameter(description = "Description du produit")
            @RequestParam(required = false) String description,

            @Parameter(description = "ID de la catégorie")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Prix unitaire")
            @RequestParam(required = false) BigDecimal price,

            @Parameter(description = "Seuil minimum de réapprovisionnement")
            @RequestParam(required = false) Integer minThreshold,

            @Parameter(description = "Stock actuel (quantité en stock)")
            @RequestParam(required = false) Integer currentStock,

            @Parameter(description = "Image du produit (JPG, PNG, max 5MB)")
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            // 1. Upload de l'image si présente
            String imageFileName = null;
            if (image != null && !image.isEmpty()) {
                // Vérifier le format de l'image (JPG, PNG)
                String originalFilename = image.getOriginalFilename();
                if (originalFilename != null) {
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Format d'image non supporté. Formats acceptés: JPG, PNG");
                    }
                }
                imageFileName = fileTransferUtil.handleFileUpload(image);
            }

            // 2. Créer le DTO avec tous les champs
            UpdateProductDTO updateProductDTO = new UpdateProductDTO();
            updateProductDTO.setName(name);
            updateProductDTO.setDescription(description);
            updateProductDTO.setCategoryId(categoryId);
            updateProductDTO.setPrice(price);
            updateProductDTO.setMinThreshold(minThreshold);
            updateProductDTO.setCurrentStock(currentStock);
            updateProductDTO.setImage(imageFileName);

            // 3. Appeler le service
            adminService.updateProduct(id, updateProductDTO);

            return ResponseEntity.ok("Produit modifié avec succès");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload de l'image: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @Operation(
            summary = "Activer/Désactiver un produit",
            description = "Active ou désactive un produit. " +
                    "Le body doit contenir 'status' (true pour activer, false pour désactiver). " +
                    "Un produit désactivé reste visible pour les administrateurs mais n'est plus affichable ni commandable par les salariés."
    )
    @PatchMapping("/products/{id}/status")
    public ResponseEntity<String> updateProductStatus(
            @Parameter(description = "ID du produit à activer/désactiver")
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductStatusDTO updateProductStatusDTO
    ) {
        try {
            adminService.updateProductStatus(id, updateProductStatusDTO);
            String message = updateProductStatusDTO.getStatus()
                    ? "Produit activé avec succès"
                    : "Produit désactivé avec succès";
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @Operation(
            summary = "Exporter la liste des produits en Excel",
            description = "Exporte la liste des produits en fichier Excel (.xlsx) selon les filtres appliqués. " +
                    "Les paramètres 'search' (recherche par nom ou code produit), 'categoryId' (filtre par catégorie) " +
                    "et 'status' (filtre actif/inactif: true/false) sont optionnels."
    )
    @GetMapping("/products/export")
    public  ResponseEntity <Resource>  exportProducts(

            @Parameter(description = "Recherche par nom ou code produit")
            @RequestParam(required = false) String search,

            @Parameter(description = "ID de la catégorie pour filtrer")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Statut actif/inactif pour filtrer (true = actif, false = inactif)")
            @RequestParam(required = false) Boolean status

    ){
        try{
            ByteArrayResource resource = adminService.exportProducts(search, categoryId, status);

            // Générer le nom du fichier avec la date et l'heure actuelles
            String fileName = "produits_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm")) + ".xlsx";

            // Retourner le fichier avec les headers appropriés pour le téléchargement
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

    }

    @Operation(
            summary = "Récupérer les statistiques du catalogue produits",
            description = "Retourne le nombre total de produits, le nombre de produits actifs et inactifs."
    )
    @GetMapping("/products/stats")
    public ResponseEntity<ProductStatsDTO> getProductStats() {
        ProductStatsDTO stats = adminService.getProductStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Top 5 produits par utilisation (%)",
            description = "Retourne les 5 produits les plus commandés avec leur taux d'utilisation en % (part des quantités par rapport au total sur les 30 derniers jours)."
    )
    @GetMapping("/products/top5-usage")
    public ResponseEntity<List<TopProductUsageDTO>> getTop5ProductUsage() {
        return ResponseEntity.ok(adminService.getTop5ProductUsage());
    }

    // ============================================================================
    // 🧾 GESTION DES FOURNISSEURS
    // ============================================================================

    @Operation(
            summary = "Créer un nouveau fournisseur",
            description = "Permet à un administrateur de créer un fournisseur."
    )
    @PostMapping("/suppliers")
    public ResponseEntity<String> createSupplier(@RequestBody @Valid CreateSupplierDTO createSupplierDTO) {
        adminService.createSupplier(createSupplierDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Fournisseur créé avec succès");
    }

    @Operation(
            summary = "Lister les fournisseurs",
            description = "Récupère la liste complète des fournisseurs (id + nom)."
    )
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierListItemDTO>> getAllSuppliers() {
        List<SupplierListItemDTO> suppliers = adminService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }
    // ============================================================================
    // 🚚 GESTION DES OPTIONS DE LIVRAISON
    // ============================================================================

    @Operation(
            summary = "Créer une option de livraison",
            description = "Permet à un administrateur de créer une nouvelle option de livraison"
    )
    @PostMapping("/delivery-options")
    public ResponseEntity<String> createDeliveryOption(@RequestBody DeliveryOptionDTO dto) {
        adminService.createDeliveryOption(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Option de livraison créée");
    }
    @Operation(
            summary = "Lister les options de livraison",
            description = "Récupère toutes les options de livraison "
    )
    @GetMapping("/delivery-options")
    public ResponseEntity<List<DeliveryOptionDTO>> getAllDeliveryOptions() {
        List<DeliveryOptionDTO> options = adminService.getAllDeliveryOptions();
        return ResponseEntity.ok(options);
    }

    // ============================================================================
    // 💰 FRAIS
    // ============================================================================

    @Operation(summary = "Lister les frais", description = "Ex. Frais de livraison, Frais d'emballage (nom + montant fixe)")
    @GetMapping("/fees")
    public ResponseEntity<List<FeeDTO>> getAllFees() {
        return ResponseEntity.ok(adminService.getAllFees());
    }

    @Operation(summary = "Créer un frais", description = "Nom obligatoire, montant obligatoire (fixe), description optionnelle")
    @PostMapping("/fees")
    public ResponseEntity<String> createFee(@RequestBody @Valid CreateFeeDTO dto) {
        adminService.createFee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Frais créé avec succès");
    }

    // ============================================================================
    // 👤 GESTION DES UTILISATEURS
    // ============================================================================

    @Operation(
            summary = "Créer un utilisateur (agent)",
            description = "Permet à un administrateur de créer un nouvel utilisateur (Administrateur, Responsable logistique, Commercial ou Livreur). " +
                    "Les salariés (EMPLOYEE) se créent via le flux Commercial. La photo de profil est optionnelle (formats acceptés: JPG, PNG, max 5MB)."
    )
    @PostMapping("/users")
    public ResponseEntity<String> createUser(
            @Parameter(description = "Prénom", required = true)
            @RequestParam String firstName,

            @Parameter(description = "Nom", required = true)
            @RequestParam String lastName,

            @Parameter(description = "Adresse email", required = true)
            @RequestParam String email,

            @Parameter(description = "Numéro de téléphone", required = true)
            @RequestParam String phoneNumber,

            @Parameter(description = "Rôle (ADMINISTRATOR, COMMERCIAL, LOGISTICS_MANAGER, DELIVERY_DRIVER)", required = true)
            @RequestParam String role,

            @Parameter(description = "Entreprise liée (optionnel, utilisé lorsque le rôle est Commercial)")
            @RequestParam(required = false) String companyCommercial,

            @Parameter(description = "Photo de profil (JPG, PNG, max 5MB)")
            @RequestParam(required = false) MultipartFile profilePhoto
    ) {
        try {
            // 1. Upload de la photo de profil si présente
            String profilePhotoFileName = null;
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                String originalFilename = profilePhoto.getOriginalFilename();
                if (originalFilename != null) {
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Format d'image non supporté. Formats acceptés: JPG, PNG");
                    }
                }
                profilePhotoFileName = fileTransferUtil.handleFileUpload(profilePhoto, "profiles");
            }

            // 2. Convertir le rôle (libellé ou nom enum)
            UserRole userRole = UserRole.fromString(role);

            // 3. Créer le DTO avec tous les champs
            SaveUserDTO dto = new SaveUserDTO();
            dto.setFirstName(firstName);
            dto.setLastName(lastName);
            dto.setEmail(email);
            dto.setPhoneNumber(phoneNumber);
            dto.setRole(userRole);
            dto.setCompanyCommercial(companyCommercial);
            dto.setProfilePhoto(profilePhotoFileName);

            // 4. Appeler le service
            adminService.createUser(dto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Utilisateur créé avec succès. Un code d'activation a été envoyé par email.");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload de la photo: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @Operation(
            summary = "Liste des utilisateurs",
            description = "Liste paginée de tous les utilisateurs (tous rôles). Filtres optionnels : search (prénom, nom ou email), role (EMPLOYEE, COMMERCIAL, etc.), status (true = actif, false = inactif)."
    )
    @GetMapping("/users")
    public ResponseEntity<UserListResponseDTO> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(adminService.getUsers(page, size, search, role, status));
    }

    // ----- Statistiques (cartes : Utilisateurs, Actifs, Inactifs) -----
    @Operation(
            summary = "Statistiques utilisateurs",
            description = "Retourne le nombre total d'utilisateurs, le nombre d'actifs et d'inactifs (pour les cartes de la page Gestion des utilisateurs)."
    )
    @GetMapping("/users/stats")
    public ResponseEntity<UserStatsDTO> getUsersStats() {
        return ResponseEntity.ok(adminService.getUsersStats());
    }

    @Operation(
            summary = "Répartition des utilisateurs par rôle",
            description = "Retourne pour chaque rôle (Salarié, Commercial, Livreur, etc.) l'effectif et le % pour le graphique « Utilisateurs par rôle »."
    )
    @GetMapping("/users/stats/by-role")
    public ResponseEntity<List<UserStatsByRoleItemDTO>> getUsersStatsByRole() {
        return ResponseEntity.ok(adminService.getUsersStatsByRole());
    }

    @Operation(
            summary = "Répartition des statuts ",
            description = "Retourne Actifs et Inactifs avec effectif et % pour le graphique « Répartition des statuts »."
    )
    @GetMapping("/users/stats/by-status")
    public ResponseEntity<List<UserStatsByStatusItemDTO>> getUsersStatsByStatus() {
        return ResponseEntity.ok(adminService.getUsersStatsByStatus());
    }

    // ----- Voir détails -----
    @Operation(
            summary = "Voir détails d'un utilisateur",
            description = "Retourne les informations détaillées d'un utilisateur (référence, nom, email, rôle, statut, date de création, etc.)."
    )
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDetailsDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    // ----- Activer / Désactiver (toggle) -----
    @Operation(
            summary = "Activer ou désactiver un utilisateur",
            description = "Met à jour le statut actif/inactif d'un utilisateur."
    )
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserStatusDTO dto) {
        adminService.updateUserStatus(id, dto);
        return ResponseEntity.ok(dto.getIsActive() ? "Utilisateur activé" : "Utilisateur désactivé");
    }

    // ----- Modifier les infos de base -----
    @Operation(
            summary = "Modifier les infos de base d'un utilisateur",
            description = "Met à jour prénom, nom, email, téléphone, rôle et (optionnel) companyCommercial. " +
                    "Photo de profil optionnelle (JPG, PNG, max 5MB). Email et téléphone doivent rester uniques."
    )
    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,

            @Parameter(description = "Prénom") @RequestParam(required = false) String firstName,
            @Parameter(description = "Nom") @RequestParam(required = false) String lastName,
            @Parameter(description = "Adresse email") @RequestParam(required = false) String email,
            @Parameter(description = "Numéro de téléphone") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Rôle") @RequestParam(required = false) String role,
            @Parameter(description = "Entreprise liée (Commercial)") @RequestParam(required = false) String companyCommercial,

            @Parameter(description = "Photo de profil (JPG, PNG, max 5MB). Si fournie, remplace l'actuelle.")
            @RequestParam(required = false) MultipartFile profilePhoto
    ) {
        try {
            String profilePhotoFileName = null;
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                String originalFilename = profilePhoto.getOriginalFilename();
                if (originalFilename != null) {
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Format d'image non supporté. Formats acceptés: JPG, PNG");
                    }
                }
                profilePhotoFileName = fileTransferUtil.handleFileUpload(profilePhoto, "profiles");
            }

            SaveUserDTO dto = new SaveUserDTO();
            dto.setFirstName(firstName);
            dto.setLastName(lastName);
            dto.setEmail(email);
            dto.setPhoneNumber(phoneNumber);
            if (role != null && !role.isBlank()) {
                dto.setRole(UserRole.fromString(role));
            }
            dto.setCompanyCommercial(companyCommercial);
            dto.setProfilePhoto(profilePhotoFileName);

            adminService.updateUser(id, dto);
            return ResponseEntity.ok("Utilisateur mis à jour avec succès");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload de la photo: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } 
    }

    @Operation(
            summary = "Mettre à jour la photo de profil d'un utilisateur",
            description = "Upload une image (photo de profil). Accepte multipart/form-data avec la partie 'file'. L'image est stockée et l'utilisateur est mis à jour."
    )
    @PutMapping(value = "/users/{id}/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateUserProfilePhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        adminService.updateUserProfilePhoto(id, file);
        return ResponseEntity.ok("Photo de profil mise à jour");
    }

    @Operation(
            summary = "Modifier ma photo de profil (admin)",
            description = "Met à jour la photo de profil de l'administrateur connecté. Accepte multipart/form-data, partie 'file' (JPEG, PNG, GIF, WebP, max 5 Mo)."
    )
    @PutMapping(value = "/me/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateMyProfilePhoto(@RequestParam("file") MultipartFile file) {
        adminService.updateProfilePhotoForCurrentUser(file);
        return ResponseEntity.ok("Photo de profil mise à jour");
    }

    // ============================================================================
    // 📊 DASHBOARD ADMIN
    // ============================================================================

    @Operation(
            summary = "Statistiques du tableau de bord admin",
            description = "Retourne les KPIs (commandes en attente, paiements échoués, réclamations ouvertes) et la répartition des paiements par statut (Payé, En attente, Échoué). Données globales, sans filtre de période."
    )
    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getDashboardStats() {
        AdminDashboardStatsDTO stats = adminService.getDashboardStats(null);
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Commandes vs Livraisons (7 derniers jours)",
            description = "Retourne pour chacun des 7 derniers jours : date (dd/MM), nombre de commandes en attente (EN_ATTENTE), " +
                    "nombre de livraisons (LIVREE). Utilisé par le graphique du tableau de bord admin. "
    )
    @GetMapping("/dashboard/commandes-vs-livraisons")
    public ResponseEntity<List<CommandesVsLivraisonsDayDTO>> getCommandesVsLivraisons() {
        List<CommandesVsLivraisonsDayDTO> list = adminService.getCommandesVsLivraisons();
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Livraisons par jour (7 derniers jours)",
            description = "Pour chaque jour : date (dd/MM), nbLivrees, nbAssignes, nbEnAttente. Graphique « Livraisons » admin."
    )
    @GetMapping("/dashboard/livraisons-par-jour")
    public ResponseEntity<List<LivraisonParJourDTO>> getLivraisonsParJour() {
        return ResponseEntity.ok(adminService.getLivraisonsParJour());
    }

    @Operation(
            summary = "Stocks - État global",
            description = "Retourne les effectifs Normal, Sous seuil, Critique (rupture) pour le donut du dashboard admin."
    )
    @GetMapping("/dashboard/stocks-etat-global")
    public ResponseEntity<StockEtatGlobalDTO> getStockEtatGlobal() {
        return ResponseEntity.ok(adminService.getStockEtatGlobal());
    }

    @Operation(
            summary = "Coupons utilisés par jour (7 derniers jours)",
            description = "Pour chaque jour : date (dd/MM), nombre d'utilisations de coupon (commandes avec coupon). Graphique « Tendance des coupons utilisés »."
    )
    @GetMapping("/dashboard/coupons-utilises-par-jour")
    public ResponseEntity<List<CouponUsageParJourDTO>> getCouponsUtilisesParJour() {
        return ResponseEntity.ok(adminService.getCouponsUtilisesParJour());
    }

    @Operation(
            summary = "Alertes du tableau de bord admin",
            description = "Liste des alertes (livraisons en retard, stocks sous seuil). Chaque alerte a type, message, detail, module (LIVRAISONS / STOCKS) pour redirection au clic, et date."
    )
    @GetMapping("/alerts")
    public ResponseEntity<AdminAlertsDTO> getAlerts() {
        return ResponseEntity.ok(adminService.getAlerts());
    }
}