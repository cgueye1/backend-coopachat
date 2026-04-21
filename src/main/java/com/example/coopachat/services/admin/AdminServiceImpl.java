package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.user.SaveUserDTO;
import com.example.coopachat.dtos.user.UpdateUserStatusDTO;
import com.example.coopachat.dtos.user.UserDetailsDTO;
import com.example.coopachat.dtos.user.UserListItemDTO;
import com.example.coopachat.dtos.user.UserListResponseDTO;
import com.example.coopachat.dtos.user.UserStatsByRoleItemDTO;
import com.example.coopachat.dtos.user.UserStatsByStatusItemDTO;
import com.example.coopachat.dtos.user.UserStatsDTO;
import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.fee.CreateFeeDTO;
import com.example.coopachat.dtos.fee.FeeDTO;
import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.categories.CategoryKpiDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.categories.UpdateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListItemDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminAlertsDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminDashboardStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.AlertItemDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.PaymentStatusItemDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatutTourneesDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatusCountDTO;
import com.example.coopachat.dtos.reference.CreateReferenceItemDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.ClaimStatus;
import com.example.coopachat.enums.EtatStock;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.exceptions.BadRequestBusinessException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.DeliveryDriver.DriverNotificationService;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import com.example.coopachat.services.user.UserReferenceGenerator;
import com.example.coopachat.services.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.ArrayList;

/**
 * Implémentation du service de gestion des actions de l'administrateur
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final FeeRepository feeRepository;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;
    private final DriverNotificationService driverNotificationService;
    private final MinioService minioService;
    private final DeliveryDriverRepository deliveryDriverRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ClaimRepository claimRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final ClaimProblemTypeRepository claimProblemTypeRepository;
    private final DeliveryIssueReasonRepository deliveryIssueReasonRepository;
    private final EmployeeDeliveryIssueReasonRepository employeeDeliveryIssueReasonRepository;
    private final CompanySectorRepository companySectorRepository;
    private final UserReferenceGenerator userReferenceGenerator;
    private final DeliveryTourRepository deliveryTourRepository;

    /**
     * Ajuste la largeur des colonnes Excel pour éviter les erreurs
     */
    private void autoSizeColumnsSafe(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Throwable e) {
                log.debug("autoSizeColumn({}) ignoré (headless / polices): {}", i, e.getMessage());
                sheet.setColumnWidth(i, 20 * 256);
            }
        }
    }

    // ============================================================================
    // 📁 GESTION DES CATÉGORIES
    // ============================================================================


    @Override
    @Transactional
    public void createCategory(CreateCategoryDTO createCategoryDTO) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer une catégorie");
        }

        // Vérifier si le nom de la catégorie existe déjà
        if (categoryRepository.existsByName(createCategoryDTO.getName())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }

        // Créer la catégorie (nom + icon si fourni)
        Category category = new Category();
        category.setName(createCategoryDTO.getName());
        if (createCategoryDTO.getIcon() != null && !createCategoryDTO.getIcon().isBlank()) {
            category.setIcon(createCategoryDTO.getIcon().trim());
        }
        categoryRepository.save(category);

        log.info("Catégorie créée avec succès par l'administrateur {}: {}",
                admin.getEmail(), category.getName());
    }

    @Override
    public List<CategoryListItemDTO> getAllCategories(String search) {

        // Normaliser le terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer les catégories selon les filtres fournis
        //si terme de recherche fourni, on recherche par nom sinon on récupère toutes les catégories triées par id décroissant
        List<Category> categories = (searchTerm != null)
                ? categoryRepository.findByNameContainingIgnoreCaseOrderByIdDesc(searchTerm)
                : categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        return categories
                .stream()
                .map(this::mapCategoryToListItemDTO)
                .collect(Collectors.toList());
    }



     // Statistiques de la page catégories (total catégories, total produits, produits actifs)
    @Override
    public CategoryKpiDTO getCategoryKpis() {
        long totalCategories = categoryRepository.count();
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus(true);
        return new CategoryKpiDTO(totalCategories, totalProducts, activeProducts);
    }

    @Override
    public CategoryListItemDTO getCategoryById(Long id) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter une catégorie");
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        return mapCategoryToListItemDTO(category);
    }

    private CategoryListItemDTO mapCategoryToListItemDTO(Category c) {
        CategoryListItemDTO dto = new CategoryListItemDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setIcon(c.getIcon());
        dto.setProductCount(productRepository.countByCategory(c));
        dto.setActiveProductCount(productRepository.countByCategoryAndStatus(c, true));
        return dto;
    }

    @Override
    @Transactional
    public void updateCategory(Long id, UpdateCategoryDTO dto) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut modifier une catégorie");
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        // Mise à jour uniquement des champs non null
        if (dto.getName() != null && !dto.getName().isBlank()) {
            String newName = dto.getName().trim();
            if (!newName.equals(category.getName()) && Boolean.TRUE.equals(categoryRepository.existsByName(newName))) {
                throw new RuntimeException("Une catégorie avec ce nom existe déjà");
            }
            category.setName(newName);
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon().isBlank() ? null : dto.getIcon().trim());
        }
        categoryRepository.save(category);
        log.info("Catégorie {} mise à jour par l'admin {}", category.getName(), admin.getEmail());
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut supprimer une catégorie");
        }

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

        List<Product> productsInCategory = productRepository.findByCategory(category, Pageable.unpaged()).getContent();
        if (!productsInCategory.isEmpty()) {
            productRepository.deleteAll(productsInCategory);
        }
        categoryRepository.delete(category);

        log.info("Catégorie {} supprimée par l'admin {} avec {} produit(s) associé(s)",
                category.getName(), admin.getEmail(), productsInCategory.size());
    }


    // ============================================================================
    // 📦 GESTION DES PRODUITS
    // ============================================================================
    @Override
    @Transactional
    public void createProduct(CreateProductDTO createProductDTO) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer un produit");
        }

        // Vérifier si le produit existe déjà (par nom)
        if (productRepository.existsByName(createProductDTO.getName())) {
            throw new RuntimeException("Un produit avec ce nom existe déjà");
        }

        // Vérifier que la catégorie existe
        Category category = categoryRepository.findById(createProductDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

        // Créer le produit
        Product product = new Product();
        product.setName(createProductDTO.getName());
        product.setDescription(createProductDTO.getDescription());
        product.setImage(createProductDTO.getImage());
        product.setCategory(category);
        product.setPrice(createProductDTO.getPrice());
        product.setCurrentStock(createProductDTO.getCurrentStock());
        product.setMinThreshold(createProductDTO.getMinThreshold() != null ? createProductDTO.getMinThreshold() : 0);
        product.setStatus(false); // Nouveau produit toujours inactif par défaut

        // Générer le code produit unique (ex: "CP-2025-001")
        String productCode = generateUniqueProductCode();
        product.setProductCode(productCode);

        productRepository.save(product);

        log.info("Produit créé avec succès par l'administrateur {}: {} (code: {})",
                admin.getEmail(), product.getName(), productCode);
    }

    @Override
    public ProductListResponseDTO getAllProducts(int page, int size, String search, Long categoryId, Boolean status) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter la liste des produits");
        }

        // Créer l'objet Pageable pour la pagination (les plus récents en premier)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // Normaliser le terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer la catégorie si categoryId est fourni
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }

        // Récupérer la page de produits selon les filtres fournis
        Page<Product> productPage;

        // Cas 1 : Recherche + Catégorie + Statut
        if (searchTerm != null && category != null && status != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
                    searchTerm, searchTerm, category, status, pageable);
        }
        // Cas 2 : Recherche + Catégorie (pas de statut)
        else if (searchTerm != null && category != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategory(
                    searchTerm, searchTerm, category, pageable);
        }
        // Cas 3 : Recherche + Statut (pas de catégorie)
        else if (searchTerm != null && status != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndStatus(
                    searchTerm, searchTerm, status, pageable);
        }
        // Cas 4 : Recherche seulement (pas de catégorie, pas de statut)
        else if (searchTerm != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        }
        // Cas 5 : Catégorie + Statut (pas de recherche)
        else if (category != null && status != null) {
            productPage = productRepository.findByCategoryAndStatus(category, status, pageable);
        }
        // Cas 6 : Catégorie seulement (pas de recherche, pas de statut)
        else if (category != null) {
            productPage = productRepository.findByCategory(category, pageable);
        }
        // Cas 7 : Statut seulement (pas de recherche, pas de catégorie)
        else if (status != null) {
            productPage = productRepository.findByStatus(status, pageable);
        }
        // Cas 8 : Aucun filtre (tous les produits)
        else {
            productPage = productRepository.findAll(pageable);
        }

        // Mapper les entités Product vers ProductListItemDTO
        List<ProductListItemDTO> productList = productPage.getContent().stream()
                .map(this::mapToProductListItemDTO)
                .collect(Collectors.toList());

        // Créer la réponse avec pagination
        ProductListResponseDTO response = new ProductListResponseDTO();
        response.setContent(productList);
        response.setTotalElements(productPage.getTotalElements());
        response.setTotalPages(productPage.getTotalPages());
        response.setCurrentPage(productPage.getNumber());
        response.setPageSize(productPage.getSize());
        response.setHasNext(productPage.hasNext());
        response.setHasPrevious(productPage.hasPrevious());

        log.info("Page {} de {} produits récupérée par l'administrateur {} (total: {} produits, recherche: '{}', catégorie: {}, statut: {})",
                page + 1, productPage.getTotalPages(), admin.getEmail(), productPage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune", categoryId != null ? category.getName() : "toutes", status != null ? status : "tous");

        return response;
    }

    @Override
    public ProductDetailsDTO getProductById(Long id) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les détails d'un produit");
        }

        // Récupérer le produit par son ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Mapper vers ProductDetailsDTO
        ProductDetailsDTO productDetails = mapToProductDetailsDTO(product);

        log.info("Détails du produit {} récupérés par l'administrateur {}",
                product.getName(), admin.getEmail());

        return productDetails;
    }

    @Override
    @Transactional
    public void updateProduct(Long id, UpdateProductDTO updateProductDTO) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut modifier un produit");
        }

        // Récupérer le produit par son ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Vérifier si le nom est modifié et qu'aucun autre produit n'a déjà ce nom
        if (updateProductDTO.getName() != null && !updateProductDTO.getName().trim().isEmpty()) {
            String newName = updateProductDTO.getName().trim();
            String currentName = product.getName() != null ? product.getName().trim() : "";
            if (!newName.equalsIgnoreCase(currentName)) {
                // Nom modifié : vérifier l'unicité en excluant ce produit
                if (Boolean.TRUE.equals(productRepository.existsByNameAndIdNot(newName, product.getId()))) {
                    throw new RuntimeException("Un produit avec ce nom existe déjà");
                }
            }
            product.setName(newName);
        }

        // Mettre à jour la description si fournie
        if (updateProductDTO.getDescription() != null) {
            product.setDescription(updateProductDTO.getDescription());
        }

        // Vérifier et mettre à jour la catégorie si fournie
        if (updateProductDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateProductDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            product.setCategory(category);
        }

        // Mettre à jour le prix si fourni
        if (updateProductDTO.getPrice() != null) {
            product.setPrice(updateProductDTO.getPrice());
        }

        // Mettre à jour le seuil minimum si fourni
        if (updateProductDTO.getMinThreshold() != null) {
            product.setMinThreshold(updateProductDTO.getMinThreshold());
        }

        // Mettre à jour le stock actuel si fourni
        if (updateProductDTO.getCurrentStock() != null) {
            product.setCurrentStock(updateProductDTO.getCurrentStock());
        }

        // Supprimer l'image si demandé, sinon mettre à jour si une nouvelle est fournie
        if (Boolean.TRUE.equals(updateProductDTO.getRemoveImage())) {
            product.setImage(null);
        } else if (updateProductDTO.getImage() != null && !updateProductDTO.getImage().trim().isEmpty()) {
            product.setImage(updateProductDTO.getImage());
        }

        // Sauvegarder les modifications
        productRepository.save(product);

        log.info("Produit {} modifié avec succès par l'administrateur {}",
                product.getName(), admin.getEmail());
    }

    @Override
    @Transactional
    public void updateProductStatus(Long id, UpdateProductStatusDTO updateProductStatusDTO) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut modifier le statut d'un produit");
        }

        // Récupérer le produit par son ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Mettre à jour le statut
        Boolean oldStatus = product.getStatus();
        product.setStatus(updateProductStatusDTO.getStatus());

        // Sauvegarder la modification
        productRepository.save(product);

        String statusChange = updateProductStatusDTO.getStatus() ? "activé" : "désactivé";
        log.info("Produit {} {} par l'administrateur {} (ancien statut: {}, nouveau statut: {})",
                product.getName(), statusChange, admin.getEmail(), oldStatus, updateProductStatusDTO.getStatus());
    }

    @Override
    public ByteArrayResource exportProducts(String search, Long categoryId, Boolean status) {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut exporter les produits");
        }

        // Normaliser le terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer la catégorie si categoryId est fourni
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }

        // Récupérer tous les produits selon les filtres (sans pagination)
        List<Product> products;

        // Application des mêmes filtres que getAllProducts
        if (searchTerm != null && category != null && status != null) {
            products = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
                    searchTerm, searchTerm, category, status, Pageable.unpaged()).getContent();
        } else if (searchTerm != null && category != null) {
            products = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategory(
                    searchTerm, searchTerm, category, Pageable.unpaged()).getContent();
        } else if (searchTerm != null && status != null) {
            products = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndStatus(
                    searchTerm, searchTerm, status, Pageable.unpaged()).getContent();
        } else if (searchTerm != null) {
            products = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(
                    searchTerm, searchTerm, Pageable.unpaged()).getContent();
        } else if (category != null && status != null) {
            products = productRepository.findByCategoryAndStatus(category, status, Pageable.unpaged()).getContent();
        } else if (category != null) {
            products = productRepository.findByCategory(category, Pageable.unpaged()).getContent();
        } else if (status != null) {
            products = productRepository.findByStatus(status, Pageable.unpaged()).getContent();
        } else {
            products = productRepository.findAll();
        }
        // Créer le workbook Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produits");

            // Créer le style pour les en-têtes
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Créer la ligne d'en-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Code Produit", "Nom", "Catégorie", "Prix (F)", "Stock", "Seuil Min", "Statut", "Date MAJ"};

            //on va parcourir le tableau headers et on va créer une cellule pour chaque en-tête
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]); //on va mettre la valeur de l'en-tête dans la cellule
                cell.setCellStyle(headerStyle);
            }

            // Créer le style pour les cellules de données
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Remplir les données
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //on va formatter la date en français

            //on va parcourir les produits et on va créer une ligne pour chaque produit
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);


                // Code produit (colonne 0)
                Cell cell0 = row.createCell(0); //on va créer une cellule pour la colonne 0 (code produit) à la ligne courante
                cell0.setCellValue(product.getProductCode() != null ? product.getProductCode() : ""); //on va mettre la valeur du code produit dans la cellule

                // Nom
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(product.getName());

                // Catégorie
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(product.getCategory().getName());

                // Prix
                Cell cell3 = row.createCell(3);
                if (product.getPrice() != null) {
                    cell3.setCellValue(product.getPrice().doubleValue());
                }

                // Stock
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(product.getCurrentStock() != null ? product.getCurrentStock() : 0);

                // Seuil minimum
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(product.getMinThreshold() != null ? product.getMinThreshold() : 0);

                // Statut
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(product.getStatus() != null && product.getStatus() ? "Actif" : "Inactif");

                // Date MAJ
                Cell cell7 = row.createCell(7);
                if (product.getUpdatedAt() != null) {
                    cell7.setCellValue(product.getUpdatedAt().format(dateFormatter));
                }
            }

            autoSizeColumnsSafe(sheet, headers.length);

            // Convertir le workbook en byte array pour l'envoyer au client
            // Le navigateur attend des données binaires (bytes) pour télécharger le fichier Excel
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream); // Écrire le workbook dans le flux de sortie
            return new ByteArrayResource(outputStream.toByteArray()); // Retourner le byte array sous forme de ByteArrayResource (enveloppe Spring qui contient les bytes du fichier Excel)
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    @Override
    public ProductStatsDTO getProductStats() {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les statistiques des produits");
        }

        long total = productRepository.count();
        long active = productRepository.countByStatus(true);
        long inactive = productRepository.countByStatus(false);

        return new ProductStatsDTO(total, active, inactive);
    }

    /**
     * Récupère le top 5 des produits les plus commandés avec leur taux d'utilisation en %.
     *
     * <p><b>Principe :</b>
     * <ul>
     *   <li>On récupère les 5 produits ayant le plus de quantités commandées depuis une date (ex. 30 derniers jours).</li>
     *   <li>On calcule le total des quantités commandées (tous produits) sur la même période.</li>
     *   <li>Pour chaque produit du top 5 : usagePercent = (quantité du produit / total) × 100.</li>
     * </ul>
     *
     * <p><b>Méthode associée :</b>
     * <ul>
     *   <li>{@code orderItemRepository.findTop5ProductsByQuantitySince(dateDebut, PageRequest.of(0, 5))} : retourne [nom, sommeQuantité] pour les 5 premiers.</li>
     *   <li>{@code orderItemRepository.sumQuantityByOrderCreatedAtAfter(dateDebut)} : retourne la somme totale des quantités sur la période.</li>
     *   <li>Pour chaque ligne : usagePercent = totalSum > 0 ? (sum * 100.0 / totalSum) : 0.</li>
     * </ul>
     *
     * @return liste de TopProductUsageDTO (productName, usagePercent entre 0 et 100)
     */
    @Override
    public List<TopProductUsageDTO> getTop5ProductUsage() {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter le top 5 produits.");
        }

        // Période : 30 derniers jours (pour alignement avec d'autres stats catalogue si besoin).
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(30);
        Pageable top5 = PageRequest.of(0, 5);

        // 1) Récupérer les 5 produits les plus commandés (nom + somme des quantités).
        List<Object[]> rawTop5 = orderItemRepository.findTop5ProductsByQuantitySince(dateDebut, top5);

        // 2) Total des quantités commandées sur la période (dénominateur pour le %).
        long totalSum = orderItemRepository.sumQuantityByOrderCreatedAtAfter(dateDebut);

        // 3) Construire les DTO avec le pourcentage d'utilisation.
        List<TopProductUsageDTO> result = new ArrayList<>();
        for (Object[] row : rawTop5) {
            String productName = (String) row[0];
            long sumQuantity = ((Number) row[1]).longValue();
            double usagePercent = totalSum > 0 ? (sumQuantity * 100.0 / totalSum) : 0.0;
            result.add(new TopProductUsageDTO(productName, Math.round(usagePercent * 10) / 10.0));
        }
        return result;
    }

    // ----------------------------------------------------------------------------
    // 🧾 GESTION DES FOURNISSEURS
    // ----------------------------------------------------------------------------

    @Override
    public List<SupplierListItemDTO> getAllSuppliers() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.SUPPLIER)
                .stream()
                .map(user -> new SupplierListItemDTO(user.getId(), user.getFirstName() + " " + user.getLastName()))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------------
    //  GESTION DES OPTIONS DE LIVRAISON 🛵
    // ----------------------------------------------------------------------------
    @Override
    @Transactional
    public void createDeliveryOption(DeliveryOptionDTO dto) {
        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer une option de livraison");

        }
        // Validation
        if (deliveryOptionRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Une option avec ce nom existe déjà");
        }

        //Création
        DeliveryOption option = new DeliveryOption();
        option.setName(dto.getName());
        option.setDescription(dto.getDescription());
        option.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        deliveryOptionRepository.save(option);

    }

    @Override
    public List<DeliveryOptionDTO> getAllDeliveryOptions() {

        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut lister les options de livraisons");

        }
        return deliveryOptionRepository.findAll().stream()
                .map(deliveryOption -> new DeliveryOptionDTO(
                        deliveryOption.getId(),
                        deliveryOption.getName(),
                        deliveryOption.getDescription(),
                        deliveryOption.getIsActive()
                ))
                .toList();

    }

    // ============================================================================
    // 💰 FRAIS (paramétrables par l'admin)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public List<FeeDTO> getAllFees() {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut lister les frais");
        }
        return feeRepository.findAll().stream()
                .map(this::mapToFeeDTO)
                .toList();
    }

    @Override
    @Transactional
    public void createFee(CreateFeeDTO dto) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer un frais");
        }
        if (feeRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Un frais avec ce nom existe déjà");
        }
        Fee fee = new Fee();
        fee.setName(dto.getName());
        fee.setDescription(dto.getDescription());
        fee.setAmount(dto.getAmount());
        fee.setIsActive(true);
        feeRepository.save(fee);
        log.info("Frais créé : {} = {} FCFA", dto.getName(), dto.getAmount());
    }

    // ============================================================================
    // 👤 Gestion des Utilisateurs
    // ============================================================================

    @Override
    @Transactional
    public void createUser(SaveUserDTO dto) {
        Users admin = getCurrentUser();

        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer un utilisateur");
        }
        // Champs obligatoires à la création
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new RuntimeException("Le prénom est obligatoire");
        }
        if (dto.getLastName() == null || dto.getLastName().isBlank()) {
            throw new RuntimeException("Le nom est obligatoire");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RuntimeException("L'adresse email est obligatoire");
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            throw new RuntimeException("Le téléphone est obligatoire");
        }
        if (dto.getRole() == null) {
            throw new RuntimeException("Le rôle est obligatoire");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }
        if (userRepository.existsByPhone(dto.getPhoneNumber())) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        if (dto.getRole() == UserRole.COMMERCIAL) {
            if (dto.getCompanyCommercial() == null || dto.getCompanyCommercial().isBlank()) {
                throw new RuntimeException("L'entreprise / entité est obligatoire pour un commercial");
            }
        }

        Users user = new Users();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhoneNumber());
        user.setRole(dto.getRole());
        user.setProfilePhotoUrl(dto.getProfilePhoto());
        user.setCompanyCommercial(dto.getCompanyCommercial());
        user.setIsActive(false);
        user.setRefUser(userReferenceGenerator.generateUniqueRefUser());

        Users savedUser = userRepository.save(user);

        // Seuls les rôles "agents" : Admin, Responsable logistique, Commercial, Livreur.
        switch (dto.getRole()) {
            case EMPLOYEE -> throw new RuntimeException(
                    "Les salariés ne se créent pas ici. Utilisez le flux Commercial (Créer un employé).");
            case DELIVERY_DRIVER -> {
                Driver driver = new Driver();
                driver.setUser(savedUser);
                driver.setCreatedBy(admin);
                deliveryDriverRepository.save(driver);
                
                // Génération du code et envoi du lien d'activation direct
                String code = activationCodeService.generateAndStoreCode(dto.getEmail());
                emailService.sendDriverActivationLink(dto.getEmail(), code, dto.getFirstName());
                log.info("Livreur créé par l'admin : {}, lien d'activation envoyé", dto.getEmail());
            }
            case COMMERCIAL, LOGISTICS_MANAGER, ADMINISTRATOR, SUPPLIER -> {
                String code = activationCodeService.generateAndStoreCode(dto.getEmail());
                emailService.sendActivationLink(dto.getEmail(), code, dto.getFirstName());
                log.info("Utilisateur {} créé par l'admin : {}, lien d'activation envoyé", dto.getRole(), dto.getEmail());
            }
        }
    }

    @Override
    public UserListResponseDTO getUsers(int page, int size, String search, UserRole role, Boolean status) {
        // Vérifier que l'utilisateur connecté est bien un administrateur
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter la liste des utilisateurs");
        }

        // Normaliser la recherche (null si vide ou blanc)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(page, size);

        //Tous les users sauf les employés
        Page<Users> userPage = userRepository.findAllWithFilters(searchTerm, role, status, pageable);

        // Mapper chaque utilisateur vers un DTO de liste
        List<UserListItemDTO> content = userPage.getContent().stream()
                .map(u -> {
                    UserListItemDTO dto = new UserListItemDTO();
                    dto.setId(u.getId());
                    String ref = u.getRefUser();
                    if (ref == null || ref.isEmpty()) {
                        ref = "US-LEGACY-" + u.getId();
                    }
                    dto.setReference(ref);
                    dto.setFirstName(u.getFirstName());
                    dto.setLastName(u.getLastName());
                    dto.setEmail(u.getEmail());
                    dto.setRoleLabel(u.getRole() != null ? u.getRole().getLabel() : "");
                    dto.setCreatedAt(u.getCreatedAt());
                    dto.setIsActive(u.getIsActive());
                    dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
                    return dto;
                })
                .collect(Collectors.toList());

        // Retourner la réponse paginée (contenu + métadonnées)
        return new UserListResponseDTO(
                content,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.hasNext(),
                userPage.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportUsers(String search, UserRole role, Boolean status) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut exporter les utilisateurs");
        }

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Pageable pageable = Pageable.unpaged();
        // Les salariés (EMPLOYEE) ne sont pas gérés ici : exclus au niveau repository (findAllWithFilters).
        Page<Users> userPage = userRepository.findAllWithFilters(searchTerm, role, status, pageable);
        List<Users> users = userPage.getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Utilisateurs");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Référence", "Prénom", "Nom", "Email", "Téléphone", "Rôle", "Statut", "Date création"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (Users u : users) {
                Row row = sheet.createRow(rowNum++);
                String ref = u.getRefUser();
                if (ref == null || ref.isEmpty()) {
                    ref = "US-LEGACY-" + u.getId();
                }
                row.createCell(0).setCellValue(ref);
                row.createCell(1).setCellValue(u.getFirstName() != null ? u.getFirstName() : "");
                row.createCell(2).setCellValue(u.getLastName() != null ? u.getLastName() : "");
                row.createCell(3).setCellValue(u.getEmail() != null ? u.getEmail() : "");
                row.createCell(4).setCellValue(u.getPhone() != null ? u.getPhone() : "");
                row.createCell(5).setCellValue(u.getRole() != null ? u.getRole().getLabel() : "");
                row.createCell(6).setCellValue(Boolean.TRUE.equals(u.getIsActive()) ? "Actif" : "Inactif");
                row.createCell(7).setCellValue(u.getCreatedAt() != null ? u.getCreatedAt().format(dtFormatter) : "");
            }

            autoSizeColumnsSafe(sheet, headers.length);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    @Override
    public UserStatsDTO getUsersStats() {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les statistiques des utilisateurs");
        }
        long total = userRepository.countExcludingEmployee();
        long active = userRepository.countByIsActiveTrueExcludingEmployee();
        long inactive = userRepository.countByIsActiveFalseExcludingEmployee();
        log.info("Statistiques utilisateurs (hors salariés) : total={}, actifs={}, inactifs={}", total, active, inactive);
        return new UserStatsDTO(total, active, inactive);
    }

    @Override
    public List<UserStatsByRoleItemDTO> getUsersStatsByRole() {
        // Vérifier que l'utilisateur connecté est bien un administrateur
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les statistiques des utilisateurs par rôle");
        }

        // Nombre total d'utilisateurs (pour calculer les pourcentages)
        long total = userRepository.count();
        List<UserStatsByRoleItemDTO> result = new ArrayList<>();

        // Parcourir tous les rôles définis dans l'enum (EMPLOYEE, COMMERCIAL, DELIVERY_DRIVER, etc.)
        for (UserRole role : UserRole.values()) {
            // Pour Salarié : compter les fiches Employee (aligné avec le commercial, tous salariés du système)
            // Pour les autres rôles : compter les Users
            long count = (role == UserRole.EMPLOYEE)
                    ? employeeRepository.count()
                    : userRepository.countByRole(role);
            // Part en % par rapport au total (0 si aucun utilisateur)
            double percentage = total > 0 ? count * 100.0 / total : 0;
            // Ajouter au résultat : rôle, libellé affichable, effectif, pourcentage
            result.add(new UserStatsByRoleItemDTO(role, role.getLabel(), count, percentage));
        }
        return result;
    }

    @Override
    public List<UserStatsByStatusItemDTO> getUsersStatsByStatus() {
        // Vérifier que l'utilisateur connecté est bien un administrateur
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter la répartition des statuts");
        }

        // Total et effectifs actifs / inactifs (hors salariés, aligné avec la liste admin)
        long total = userRepository.countExcludingEmployee();
        long active = userRepository.countByIsActiveTrueExcludingEmployee();
        long inactive = userRepository.countByIsActiveFalseExcludingEmployee();

        // Si total est supérieur à 0, on calcule le pourcentage d'actifs et inactifs ; sinon on retourne 0
        double activePct = total > 0 ? active * 100.0 / total : 0;
        double inactivePct = total > 0 ? inactive * 100.0 / total : 0;

        // Deux éléments : Actifs, puis Inactifs (label, count, percentage)
        List<UserStatsByStatusItemDTO> result = new ArrayList<>();
        result.add(new UserStatsByStatusItemDTO("Actifs", active, activePct));
        result.add(new UserStatsByStatusItemDTO("Inactifs", inactive, inactivePct));
        return result;
    }

    @Override
    public UserDetailsDTO getUserById(Long id) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les détails d'un utilisateur");
        }
        Users u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        UserDetailsDTO dto = new UserDetailsDTO();
        dto.setId(u.getId());
        dto.setRefUser(u.getRefUser());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setEmail(u.getEmail());
        dto.setPhoneNumber(u.getPhone());
        dto.setRole(u.getRole());
        dto.setRoleLabel(u.getRole() != null ? u.getRole().getLabel() : "");
        dto.setCompanyCommercial(u.getCompanyCommercial());
        dto.setIsActive(u.getIsActive());
        dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
    @Override
    @Transactional
    public void updateUserStatus(Long id, UpdateUserStatusDTO dto) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut activer ou désactiver un utilisateur");
        }
        if (dto.getIsActive() == null) {
            throw new RuntimeException("Le statut (isActive) est obligatoire");
        }
        Users u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Impossible d'activer un utilisateur qui n'a pas encore défini son mot de passe
        if (Boolean.TRUE.equals(dto.getIsActive())
                && (u.getPassword() == null || u.getPassword().isBlank())) {
            throw new BadRequestBusinessException(
                    "Impossible d'activer ce compte : l'utilisateur n'a pas encore défini son mot de passe. "
                            + "Il doit d'abord terminer son inscription (code d'activation reçu par e-mail, puis création du mot de passe). "
                            + "Ensuite vous pourrez activer le compte depuis cet écran.");
        }

        u.setIsActive(dto.getIsActive());
        userRepository.save(u);
        log.info("Statut utilisateur {} mis à jour : isActive={}", u.getEmail(), dto.getIsActive());
    }

    @Override
    @Transactional
    public void updateUser(Long id, SaveUserDTO dto) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut modifier un utilisateur");
        }
        Users u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Ne mettre à jour que les champs non null
        if (dto.getFirstName() != null) {
            u.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            u.setLastName(dto.getLastName());
        }
        //on vérifie que l'email n'est pas déjà utilisé par un autre utilisateur (sauf pour l'utilisateur lui-même)
        if (dto.getEmail() != null) {
            if (Boolean.TRUE.equals(userRepository.existsByEmailAndIdNot(dto.getEmail(), id))) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur");
            }
            u.setEmail(dto.getEmail());
        }
        //on vérifie que le numéro de téléphone n'est pas déjà utilisé par un autre utilisateur (sauf pour l'utilisateur lui-même)
        if (dto.getPhoneNumber() != null) {
            if (Boolean.TRUE.equals(userRepository.existsByPhoneAndIdNot(dto.getPhoneNumber(), id))) {
                throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre utilisateur");
            }
            u.setPhone(dto.getPhoneNumber());
        }
        if (dto.getRole() != null) {
            u.setRole(dto.getRole());
        }
        if (dto.getCompanyCommercial() != null) {
            u.setCompanyCommercial(dto.getCompanyCommercial());
        }
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isBlank()) {
            u.setProfilePhotoUrl(dto.getProfilePhoto());
        }
        userRepository.save(u);
        log.info("Utilisateur {} mis à jour par l'admin", u.getEmail());
    }

    private static final long PROFILE_PHOTO_MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> PROFILE_PHOTO_ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> PROFILE_PHOTO_ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    @Transactional
    public void updateUserProfilePhoto(Long userId, MultipartFile file) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut modifier la photo de profil d'un autre utilisateur.");
        }
        doUpdateUserProfilePhoto(userId, file);
    }

    @Override
    @Transactional
    public void updateProfilePhotoForCurrentUser(MultipartFile file) {
        Users currentUser = getCurrentUser();
        doUpdateUserProfilePhoto(currentUser.getId(), file);
    }

    @Override
    @Transactional
    public void removeUserProfilePhoto(Long userId) {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut retirer la photo de profil d'un utilisateur.");
        }
        doRemoveProfilePhotoForUserId(userId);
    }

    @Override
    @Transactional
    public void removeProfilePhotoForCurrentUser() {
        Users u = getCurrentUser();
        doRemoveProfilePhotoForUserId(u.getId());
    }

    private void doRemoveProfilePhotoForUserId(Long userId) {
        Users u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        String oldPhoto = u.getProfilePhotoUrl();
        if (oldPhoto != null && !oldPhoto.isBlank()) {
            try {
                minioService.deleteFile(oldPhoto);
            } catch (Exception e) {
                log.warn("Impossible de supprimer le fichier photo MinIO {}: {}", oldPhoto, e.getMessage());
            }
        }
        u.setProfilePhotoUrl(null);
        userRepository.save(u);
        log.info("Photo de profil retirée pour l'utilisateur {}", u.getEmail());
    }

    /**
     * Logique commune : validation image, suppression ancienne photo, upload, mise à jour en BDD.
     */
    private void doUpdateUserProfilePhoto(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Aucun fichier fourni");
        }
        //Validation du type de fichier
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(';')).trim();
        }
        if (contentType == null || !PROFILE_PHOTO_ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Type de fichier non autorisé. Utilisez une image JPEG, PNG, GIF ou WebP.");
        }
        // Validation extension (sécurité supplémentaire)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            if (!PROFILE_PHOTO_ALLOWED_EXTENSIONS.contains(ext)) {
                throw new RuntimeException("Extension non autorisée. Utilisez .jpg, .png, .gif ou .webp.");
            }
        }
        // Taille max 5 Mo
        if (file.getSize() > PROFILE_PHOTO_MAX_SIZE_BYTES) {
            throw new RuntimeException("La photo ne doit pas dépasser 5 Mo.");
        }
        // Récupération de l'utilisateur
        Users u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        try {
            //  Supprimer l'ancienne photo si elle existe 
            String oldPhoto = u.getProfilePhotoUrl();
            if (oldPhoto != null && !oldPhoto.isBlank()) {
                minioService.deleteFile(oldPhoto);
            }
            String relativePath = minioService.uploadFile(file, "profiles");//Upload de la nouvelle photo
            u.setProfilePhotoUrl(relativePath);//Mise à jour de la photo de profil dans la base de données
            userRepository.save(u);//Sauvegarde de l'utilisateur
            log.info("Photo de profil mise à jour pour l'utilisateur {}", u.getEmail());
        } catch (Exception e) {
            log.error("Erreur upload photo de profil: {}", e.getMessage());
            throw new RuntimeException("Impossible d'enregistrer la photo");
        }
    }

    // ============================================================================
    // 📊 DASHBOARD ADMIN
    // ============================================================================

    /**
     * Construit les statistiques du tableau de bord admin (sans filtre de période).
     * Utilisé par l'API GET /api/admin/dashboard/stats.
     * Tous les comptages sont globaux (toutes les commandes/paiements concernés, sans restriction de date).
     */
    @Override
    public AdminDashboardStatsDTO getDashboardStats(String periode) {
        // KPIs et paiements par statut : plus de filtre par période, on compte tout
        long commandesEnAttente = orderRepository.countByStatus(OrderStatus.EN_ATTENTE);
        long paiementsEchoues = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long reclamationsOuvertes = claimRepository.countByStatus(ClaimStatus.EN_ATTENTE);

        List<PaymentStatusItemDTO> paiementsParStatut = new ArrayList<>();
        long payes = paymentRepository.countByStatus(PaymentStatus.PAID);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.PAID.getLabel(), payes));
        long enAttente = paymentRepository.countByStatus(PaymentStatus.UNPAID);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.UNPAID.getLabel(), enAttente));
        long echoues = paymentRepository.countByStatus(PaymentStatus.FAILED);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.FAILED.getLabel(), echoues));

        return new AdminDashboardStatsDTO(
                commandesEnAttente,
                paiementsEchoues,
                reclamationsOuvertes,
                paiementsParStatut
        );
    }

    @Override
    public List<LivraisonParJourDTO> getCommandesVsLivraisons() {
        return getLivraisonsParJour();
    }

    @Override
    public List<LivraisonParJourDTO> getLivraisonsParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<LivraisonParJourDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long nbPrevues = orderRepository.countByDeliveryDateExcludingCancelled(day, OrderStatus.ANNULEE);
            long nbLivreesALaDate = orderRepository.countByStatusAndDeliveryDate(OrderStatus.LIVREE, day);
            // Retard par jour : date prévue = ce jour, encore EN_ATTENTE (pas un cumul « livraison avant ce jour »).
            long nbRetard = orderRepository.countByStatusAndDeliveryDate(OrderStatus.EN_ATTENTE, day);
            result.add(new LivraisonParJourDTO(
                    day.format(formatter), nbPrevues, nbLivreesALaDate, nbRetard));
        }
        return result;
    }

    //Retourne le nombre de fois qu'un coupon a été utilisé dans une commande par jour (7 derniers jours)
    @Override
    public List<CouponUsageParJourDTO> getCouponsUtilisesParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<CouponUsageParJourDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.atTime(23, 59, 59, 999_999_999);
            long nbUtilisations = orderRepository.countByCouponIsNotNullAndCreatedAtBetween(dayStart, dayEnd);
            result.add(new CouponUsageParJourDTO(day.format(formatter), nbUtilisations));
        }
        return result;
    }

    @Override
    public StockEtatGlobalDTO getStockEtatGlobal() {
        long total = productRepository.count();
        long sousSeuil = productRepository.countLowStock();
        long critique = productRepository.countByCurrentStock(0);
        long normal = total - sousSeuil - critique;
        return new StockEtatGlobalDTO(normal, sousSeuil, critique);
    }

    @Override
    @Transactional(readOnly = true)
    public StatutTourneesDTO getStatutTournees() {
        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (DeliveryTourStatus s : DeliveryTourStatus.values()) {
            parStatut.put(s.name(), 0L);
        }
        for (StatusCountDTO row : deliveryTourRepository.countGroupByStatus()) {
            parStatut.put(row.status().name(), row.count());
        }
        return new StatutTourneesDTO(parStatut);
    }

    /**
     * Construit la liste des alertes pour le tableau de bord admin (GET /admin/alerts).
     * — Alerte 1 : livraisons en retard (commandes EN_ATTENTE avec date de livraison avant aujourd'hui).
     * — Alerte 2 : stocks critiques (produits dont le stock est strictement inférieur au seuil ; message = nombre).
     * Chaque alerte contient un module (LIVRAISONS / STOCKS) pour que le front redirige au clic (ex. STOCKS → page Gestion des stocks).
     */
    @Override
    public AdminAlertsDTO getAlerts() {
        List<AlertItemDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Alerte 1 — Livraisons en retard
        long retard = orderRepository.countByStatusAndDeliveryDateBefore(OrderStatus.EN_ATTENTE, today);
        if (retard > 0) {
            alerts.add(new AlertItemDTO(
                    "WARNING",
                    retard + " livraison(s) en retard",
                    "Cliquez pour ouvrir le module concerné",
                    "LIVRAISONS",
                    today
            ));
        }

        // Alerte 2 — Stocks critiques (stock strictement inférieur au seuil ; on affiche le nombre)
        long stocksCritiques = productRepository.countByCurrentStockLessThanMinThreshold();
        if (stocksCritiques > 0) {//si le nombre de stocks critiques est supérieur à 0, on ajoute une alerte
            alerts.add(new AlertItemDTO(
                    "DANGER",
                    stocksCritiques + " stock(s) en critique",
                    "Cliquez pour ouvrir le module concerné",
                    "STOCKS",
                    today
            ));
        }

        return new AdminAlertsDTO(alerts);
    }

    // ============================================================================
    // 📋 RÉFÉRENTIELS (types réclamation, raisons livraison)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemDTO> getAllClaimProblemTypes() {
        return claimProblemTypeRepository.findAll().stream()
                .map(e -> new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceItemDTO getClaimProblemTypeById(Long id) {
        ClaimProblemType e = claimProblemTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de réclamation introuvable"));
        return new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription());
    }

    @Override
    @Transactional
    public void createClaimProblemType(CreateReferenceItemDTO dto) {
        ClaimProblemType e = new ClaimProblemType();
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        claimProblemTypeRepository.save(e);
    }

    @Override
    @Transactional
    public void updateClaimProblemType(Long id, CreateReferenceItemDTO dto) {
        ClaimProblemType e = claimProblemTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de réclamation introuvable"));
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        claimProblemTypeRepository.save(e);
    }

    @Override
    @Transactional
    public void deleteClaimProblemType(Long id) {
        if (!claimProblemTypeRepository.existsById(id)) {
            throw new RuntimeException("Type de réclamation introuvable");
        }
        claimProblemTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemDTO> getAllDeliveryIssueReasons() {
        return deliveryIssueReasonRepository.findAll().stream()
                .map(e -> new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceItemDTO getDeliveryIssueReasonById(Long id) {
        DeliveryIssueReason e = deliveryIssueReasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raison introuvable"));
        return new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription());
    }

    @Override
    @Transactional
    public void createDeliveryIssueReason(CreateReferenceItemDTO dto) {
        DeliveryIssueReason e = new DeliveryIssueReason();
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        deliveryIssueReasonRepository.save(e);
    }

    @Override
    @Transactional
    public void updateDeliveryIssueReason(Long id, CreateReferenceItemDTO dto) {
        DeliveryIssueReason e = deliveryIssueReasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raison introuvable"));
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        deliveryIssueReasonRepository.save(e);
    }

    @Override
    @Transactional
    public void deleteDeliveryIssueReason(Long id) {
        if (!deliveryIssueReasonRepository.existsById(id)) {
            throw new RuntimeException("Raison introuvable");
        }
        deliveryIssueReasonRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemDTO> getAllEmployeeDeliveryIssueReasons() {
        return employeeDeliveryIssueReasonRepository.findAll().stream()
                .map(e -> new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceItemDTO getEmployeeDeliveryIssueReasonById(Long id) {
        EmployeeDeliveryIssueReason e = employeeDeliveryIssueReasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raison introuvable"));
        return new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription());
    }

    @Override
    @Transactional
    public void createEmployeeDeliveryIssueReason(CreateReferenceItemDTO dto) {
        EmployeeDeliveryIssueReason e = new EmployeeDeliveryIssueReason();
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        employeeDeliveryIssueReasonRepository.save(e);
    }

    @Override
    @Transactional
    public void updateEmployeeDeliveryIssueReason(Long id, CreateReferenceItemDTO dto) {
        EmployeeDeliveryIssueReason e = employeeDeliveryIssueReasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raison introuvable"));
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        employeeDeliveryIssueReasonRepository.save(e);
    }

    @Override
    @Transactional
    public void deleteEmployeeDeliveryIssueReason(Long id) {
        if (!employeeDeliveryIssueReasonRepository.existsById(id)) {
            throw new RuntimeException("Raison introuvable");
        }
        employeeDeliveryIssueReasonRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceItemDTO> getAllCompanySectors() {
        return companySectorRepository.findAll().stream()
                .map(e -> new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceItemDTO getCompanySectorById(Long id) {
        CompanySector e = companySectorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur introuvable"));
        return new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription());
    }

    @Override
    @Transactional
    public void createCompanySector(CreateReferenceItemDTO dto) {
        CompanySector e = new CompanySector();
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        companySectorRepository.save(e);
    }

    @Override
    @Transactional
    public void updateCompanySector(Long id, CreateReferenceItemDTO dto) {
        CompanySector e = companySectorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur introuvable"));
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        companySectorRepository.save(e);
    }

    @Override
    @Transactional
    public void deleteCompanySector(Long id) {
        if (!companySectorRepository.existsById(id)) {
            throw new RuntimeException("Secteur introuvable");
        }
        companySectorRepository.deleteById(id);
    }

    // ----------------------------------------------------------------------------
    // 🔧 MÉTHODES UTILITAIRES
    // ----------------------------------------------------------------------------
    private FeeDTO mapToFeeDTO(Fee fee) {
        return new FeeDTO(
                fee.getId(),
                fee.getName(),
                fee.getDescription(),
                fee.getAmount(),
                fee.getIsActive()
        );
    }

    /**
     * Code produit (catalogue) unique :
     * format {@code CP-XXXXXXXX}.
     */
    private String generateUniqueProductCode() {
        final int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String candidate = "CP-" + suffix;
            if (!productRepository.existsByProductCode(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Impossible de générer un code produit unique");
    }

    /**
     * Convertit le booléen status en statut textuel pour l'affichage
     *
     * @param status L'état actif/inactif du produit
     * @return "Actif" si true, "Inactif" si false
     */
    private String status(Boolean status) {
        if (status != null && status) {
            return "Actif";
        }
        return "Inactif";
    }

    /**
     * Mappe une entité Product vers un ProductListItemDTO
     *
     * @param product L'entité Product à mapper
     * @return Le DTO correspondant
     */
    private ProductListItemDTO mapToProductListItemDTO(Product product) {
        ProductListItemDTO dto = new ProductListItemDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setProductCode(product.getProductCode());
        dto.setCategoryName(product.getCategory().getName());
        dto.setPrice(product.getPrice());
        dto.setCurrentStock(product.getCurrentStock());
        dto.setImage(product.getImage());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setStatus(status(product.getStatus())); // Convertit status en "Actif" ou "Inactif"
        return dto;
    }

    /**
     * Mappe une entité Product vers un ProductDetailsDTO
     *
     * @param product L'entité Product à mapper
     * @return Le DTO de détails correspondant
     */
    private ProductDetailsDTO mapToProductDetailsDTO(Product product) {
        ProductDetailsDTO dto = new ProductDetailsDTO();
        dto.setProductCode(product.getProductCode());
        dto.setImage(product.getImage());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryName(product.getCategory().getName());
        dto.setPrice(product.getPrice());
        dto.setStatus(product.getStatus());
        dto.setCurrentStock(product.getCurrentStock() != null ? product.getCurrentStock() : 0);
        dto.setCurrentStockStatus(getStockStatus(product.getCurrentStock())); // "En stock" ou "Rupture" (EtatStock)
        return dto;
    }

    /**
     * Détermine le statut du stock en fonction de la quantité disponible.
     * Stock = 0 ou null → Rupture (EtatStock.RUPTURE), sinon "En stock".
     *
     * @param currentStock La quantité en stock
     * @return "En stock" si stock > 0, "Rupture" sinon (libellé EtatStock.RUPTURE)
     */
    private String getStockStatus(Integer currentStock) {
        if (currentStock != null && currentStock > 0) {
            return "En stock";
        }
        return EtatStock.RUPTURE.getLabel(); // "Rupture"
    }


    /**
     * Récupère l'utilisateur actuellement connecté.
     * @return Users l'utilisateur connecté
     * @throws RuntimeException si aucun utilisateur n'est authentifié
     */
    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable avec email: " + userEmail
                ));
    }

}




