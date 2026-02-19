package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.fee.CreateFeeDTO;
import com.example.coopachat.dtos.fee.FeeDTO;
import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListItemDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.dtos.products.ProductStatsDTO;
import com.example.coopachat.dtos.products.UpdateProductDTO;
import com.example.coopachat.dtos.products.UpdateProductStatusDTO;
import com.example.coopachat.dtos.suppliers.CreateSupplierDTO;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PaymentTimingRepository paymentTimingRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final FeeRepository feeRepository;

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

        // Créer la catégorie
        Category category = new Category();
        category.setName(createCategoryDTO.getName());

        categoryRepository.save(category);

        log.info("Catégorie créée avec succès par l'administrateur {}: {}",
                admin.getEmail(), category.getName());
    }

    @Override
    public List<CategoryListItemDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryListItemDTO(category.getId(), category.getName()))
                .collect(Collectors.toList());
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

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

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

    private FeeDTO mapToFeeDTO(Fee fee) {
        return new FeeDTO(
                fee.getId(),
                fee.getName(),
                fee.getDescription(),
                fee.getAmount(),
                fee.getIsActive()
        );
    }

    // ----------------------------------------------------------------------------
    // 🔧 MÉTHODES UTILITAIRES
    // ----------------------------------------------------------------------------

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
        dto.setCurrentStockStatus(getStockStatus(product.getCurrentStock())); // "En stock" ou "Rupture de stock"
        return dto;
    }

    /**
     * Détermine le statut du stock en fonction de la quantité disponible
     *
     * @param currentStock La quantité en stock
     * @return "En stock" si stock > 0, "Rupture de stock" sinon
     */
    private String getStockStatus(Integer currentStock) {
        if (currentStock != null && currentStock > 0) {
            return "En stock";
        }
        return "Rupture de stock";
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




