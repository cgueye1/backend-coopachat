package com.example.coopachat.controllers;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.suppliers.CreateSupplierDTO;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
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
@Tag(name = "Administrateur", description = "API pour la gestion des actions de l'administrateur (catalogue, catégories, fournisseurs)")
public class AdminController {

    private final AdminService adminService;

    // ============================================================================
    // 📁 GESTION DES CATÉGORIES
    // ============================================================================

    @Operation(
            summary = "Créer une nouvelle catégorie",
            description = "Permet à un administrateur de créer une nouvelle catégorie de produit. " +
                    "Le nom de la catégorie doit être unique."
    )
    @PostMapping("/categories")
    public ResponseEntity<String> createCategory(@RequestBody @Valid CreateCategoryDTO createCategoryDTO) {
        adminService.createCategory(createCategoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Catégorie créée avec succès");
    }

    @Operation(
            summary = "Lister les catégories",
            description = "Récupère la liste complète des catégories (id + nom)."
    )
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryListItemDTO>> getAllCategories() {
        List<CategoryListItemDTO> categories = adminService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    // ============================================================================
    // 📦 GESTION DES PRODUITS
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
                imageFileName = FileTransferUtil.handleFileUpload(image);
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
                imageFileName = FileTransferUtil.handleFileUpload(image);
            }

            // 2. Créer le DTO avec tous les champs
            UpdateProductDTO updateProductDTO = new UpdateProductDTO();
            updateProductDTO.setName(name);
            updateProductDTO.setDescription(description);
            updateProductDTO.setCategoryId(categoryId);
            updateProductDTO.setPrice(price);
            updateProductDTO.setMinThreshold(minThreshold);
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
}