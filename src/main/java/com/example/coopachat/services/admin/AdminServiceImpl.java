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
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.categories.UpdateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListItemDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.suppliers.CreateSupplierDTO;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.dtos.dashboard.admin.AdminDashboardStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.CommandesVsLivraisonsDayDTO;
import com.example.coopachat.dtos.dashboard.admin.PaymentStatusItemDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.ClaimStatus;
import com.example.coopachat.enums.EtatStock;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import com.example.coopachat.util.FileTransferUtil;
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
import java.util.List;
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
    private final SupplierRepository supplierRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final FeeRepository feeRepository;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;
    private final FileTransferUtil fileTransferUtil;
    private final DeliveryDriverRepository deliveryDriverRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ClaimRepository claimRepository;

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
    public List<CategoryListItemDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapCategoryToListItemDTO)
                .collect(Collectors.toList());
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
        product.setStatus(createProductDTO.getStatus() != null ? createProductDTO.getStatus() : false);

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

        // Vérifier si le nom est modifié et s'il existe déjà (sauf si c'est le même nom)
        if (updateProductDTO.getName() != null && !updateProductDTO.getName().trim().isEmpty()) {
            String newName = updateProductDTO.getName().trim();
            // Si le nouveau nom est différent de l'ancien, vérifier l'unicité
            if (!newName.equals(product.getName())) {
                if (productRepository.existsByName(newName)) {
                    throw new RuntimeException("Un produit avec ce nom existe déjà");
                }
                product.setName(newName);
            }
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

        // Mettre à jour l'image si fournie
        if (updateProductDTO.getImage() != null && !updateProductDTO.getImage().trim().isEmpty()) {
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

            // Ajuster la largeur des colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir le workbook en byte array pour l'envoyer au client
            // Le navigateur attend des données binaires (bytes) pour télécharger le fichier Excel
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream); // Écrire le workbook dans le flux de sortie
            return new ByteArrayResource(outputStream.toByteArray()); // Retourner le byte array sous forme de ByteArrayResource (enveloppe Spring qui contient les bytes du fichier Excel)
        } catch (IOException e) {
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

    // ----------------------------------------------------------------------------
    // 🧾 GESTION DES FOURNISSEURS
    // ----------------------------------------------------------------------------

    @Override
    @Transactional
    public void createSupplier(CreateSupplierDTO createSupplierDTO) {
        Users admin = getCurrentUser();

        // Vérifier que l'utilisateur connecté est bien un Administrateur
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut créer un fournisseur");
        }

        if (supplierRepository.existsByEmail(createSupplierDTO.getEmail())) {
            throw new RuntimeException("Un fournisseur avec cet email existe déjà");
        }
        if (supplierRepository.existsByPhone(createSupplierDTO.getPhone())) {
            throw new RuntimeException("Un fournisseur avec ce téléphone existe déjà");
        }

        Supplier supplier = new Supplier();
        supplier.setName(createSupplierDTO.getName());
        supplier.setEmail(createSupplierDTO.getEmail());
        supplier.setPhone(createSupplierDTO.getPhone());
        supplier.setAddress(createSupplierDTO.getAddress());
        supplier.setIsActive(createSupplierDTO.getIsActive() != null ? createSupplierDTO.getIsActive() : true);

        supplierRepository.save(supplier);

        log.info("Fournisseur créé avec succès par l'administrateur {}: {}",
                admin.getEmail(), supplier.getName());
    }

    @Override
    public List<SupplierListItemDTO> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(supplier -> new SupplierListItemDTO(supplier.getId(), supplier.getName()))
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

        Users user = new Users();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhoneNumber());
        user.setRole(dto.getRole());
        user.setCompanyCommercial(dto.getCompanyCommercial());
        user.setIsActive(false);

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
                //les liveurs on les envoie de code d'activation que lorsqu'ils souhaitent activer leurs comptes
            }
            case COMMERCIAL, LOGISTICS_MANAGER, ADMINISTRATOR -> {
                String code = activationCodeService.generateAndStoreCode(dto.getEmail());
                emailService.sendActivationCode(dto.getEmail(), code, dto.getFirstName());
                log.info("Utilisateur {} créé par l'admin : {}, code d'activation envoyé", dto.getRole(), dto.getEmail());
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

        // Récupérer la page d'utilisateurs avec les filtres (recherche, rôle, statut actif/inactif)
        Page<Users> userPage = userRepository.findAllWithFilters(searchTerm, role, status, pageable);

        // Mapper chaque utilisateur vers un DTO de liste
        List<UserListItemDTO> content = userPage.getContent().stream()
                .map(u -> {
                    UserListItemDTO dto = new UserListItemDTO();
                    dto.setId(u.getId());
                    String ref = u.getRefUser();
                    if (ref == null || ref.isEmpty()) {
                        int year = u.getCreatedAt() != null ? u.getCreatedAt().getYear() : LocalDateTime.now().getYear();
                        ref = "US-" + year + "-" + u.getId();
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
    public UserStatsDTO getUsersStats() {
        Users admin = getCurrentUser();
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new RuntimeException("Seul un administrateur peut consulter les statistiques des utilisateurs");
        }
        long total = userRepository.count();
        long active = userRepository.countByIsActiveTrue();
        long inactive = userRepository.countByIsActiveFalse();
        log.info("Statistiques utilisateurs : total={}, actifs={}, inactifs={}", total, active, inactive);
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
            // UserRole.values() renvoie toutes les valeurs de l'enum UserRole 
            // Nombre d'utilisateurs ayant ce rôle
            long count = userRepository.countByRole(role);
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

        // Total et effectifs actifs / inactifs (isActive = true / false)
        long total = userRepository.count();
        long active = userRepository.countByIsActiveTrue();
        long inactive = userRepository.countByIsActiveFalse();

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

    /**
     * Logique commune : validation image, suppression ancienne photo, upload, mise à jour en BDD.
     */
    private void doUpdateUserProfilePhoto(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Aucun fichier fourni");
        }
        // Validation type MIME (ignorer paramètres ex. "image/jpeg; charset=utf-8")
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
        Users u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        try {
            // Supprimer l'ancienne photo si elle existe (local + SFTP)
            String oldPhoto = u.getProfilePhotoUrl();
            if (oldPhoto != null && !oldPhoto.isBlank()) {
                fileTransferUtil.deleteFile(oldPhoto);
            }
            // Upload dans le sous-dossier "profiles" (ex. files/profiles/uuid.jpg)
            String relativePath = fileTransferUtil.handleFileUpload(file, "profiles");
            u.setProfilePhotoUrl(relativePath);
            userRepository.save(u);
            log.info("Photo de profil mise à jour pour l'utilisateur {}", u.getEmail());
        } catch (IOException e) {
            log.error("Erreur upload photo de profil: {}", e.getMessage());
            throw new RuntimeException("Impossible d'enregistrer la photo");
        }
    }

    // ============================================================================
    // 📊 DASHBOARD ADMIN
    // ============================================================================

    /**
     * Construit les statistiques du tableau de bord admin pour une période donnée.
     * Utilisé par l'API GET /api/admin/dashboard/stats?periode=TODAY ou THIS_MONTH.
     */
    @Override
    public AdminDashboardStatsDTO getDashboardStats(String periode) {
        // ---- 1) Définir la plage de dates (début et fin) selon la période demandée ----
        LocalDateTime start;
        LocalDateTime end;
        LocalDate today = LocalDate.now();

        if ("TODAY".equalsIgnoreCase(periode)) {
            // Aujourd'hui : de 00h00 à 23h59
            start = today.atStartOfDay();
            end = today.atTime(23, 59, 59, 999_999_999);
        } else if ("THIS_MONTH".equalsIgnoreCase(periode)) {
            // Mois en cours : du 1er du mois à aujourd'hui 23h59
            start = today.withDayOfMonth(1).atStartOfDay();  // 01/02/2026 00:00:00
            end = today.atTime(23, 59, 59, 999_999_999);
        } else {
            // Si une autre valeur est passée, on prend le mois en cours par défaut
            start = today.withDayOfMonth(1).atStartOfDay();
            end = today.atTime(23, 59, 59, 999_999_999);
        }

        //a. En attente
        long commandesEnAttente =  orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.EN_ATTENTE, start, end);

        // ----b) KPI : nombre de paiements échoués dans la période ----
        long paiementsEchoues = paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.FAILED, start, end);

        // ---- c) KPI : réclamations encore ouvertes (en attente de traitement) ----
        // Ici on ne filtre pas par période : on compte toutes les réclamations "En attente" actuellement.
        long reclamationsOuvertes = claimRepository.countByStatus(ClaimStatus.EN_ATTENTE);

        // ---- d) Liste "paiements par statut" pour le graphique (3 statuts uniquement) ----
        List<PaymentStatusItemDTO> paiementsParStatut = new ArrayList<>();

        long payes = paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.PAID, start, end);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.PAID.getLabel(), payes));
        long enAttente = paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.UNPAID, start, end);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.PENDING.getLabel(), enAttente));
        long echoues = paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.FAILED, start, end);
        paiementsParStatut.add(new PaymentStatusItemDTO(PaymentStatus.FAILED.getLabel(), echoues));

        // ---- 2) On renvoie le DTO ----
        return new AdminDashboardStatsDTO(
                commandesEnAttente,
                paiementsEchoues,
                reclamationsOuvertes,
                paiementsParStatut
        );
    }

    @Override
    public List<CommandesVsLivraisonsDayDTO> getCommandesVsLivraisons() {

        // Définit le format d'affichage de la date (ex : 25/02)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        // Récupère la date du jour
        LocalDate today = LocalDate.now();

        // Liste qui va contenir les résultats des 7 derniers jours
        List<CommandesVsLivraisonsDayDTO> result = new ArrayList<>();

        // Boucle sur les 7 derniers jours (de J-6 jusqu'à aujourd’hui)
        for (int i = 6; i >= 0; i--) {

            // Calcule la date du jour en cours dans la boucle
            LocalDate day = today.minusDays(i);

            // Définit le début de la journée (00:00:00)
            LocalDateTime dayStart = day.atStartOfDay();

            // Définit la fin de la journée (23:59:59.999999999)
            LocalDateTime dayEnd = day.atTime(23, 59, 59, 999_999_999);

            // Compte les commandes créées ce jour-là avec le statut EN_ATTENTE
            long commandesEnAttente = orderRepository.countByStatusAndCreatedAtBetween(
                    OrderStatus.EN_ATTENTE, dayStart, dayEnd);

            // Compte les commandes livrées ce jour-là (date de livraison effective)
            long livraisons = orderRepository.countByStatusAndDeliveryCompletedAtBetween(
                    OrderStatus.LIVREE, dayStart, dayEnd);

            // Ajoute les données du jour dans la liste (date + commandes + livraisons)
            result.add(new CommandesVsLivraisonsDayDTO(
                    day.format(formatter),
                    commandesEnAttente,
                    livraisons
            ));
        }

        // Retourne la liste complète pour alimenter le graphique
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
     * Génère un code produit unique au format "CP-YYYY-XXX"
     */
    private String generateUniqueProductCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String baseCode = "CP-" + year + "-";
        String productCode;
        int counter = 1;

        do {
            productCode = baseCode + String.format("%03d", counter);
            counter++;
        } while (productRepository.existsByProductCode(productCode));

        return productCode;
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




