package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductListItemDTO;
import com.example.coopachat.dtos.products.ProductListResponseDTO;
import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Product;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.CategoryRepository;
import com.example.coopachat.repositories.ProductRepository;
import com.example.coopachat.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // ============================================================================
    // 📁 GESTION DES CATÉGORIES
    // ============================================================================

    @Override
    @Transactional
    public void createCategory(CreateCategoryDTO createCategoryDTO) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users admin = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

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

    // ============================================================================
    // 📦 GESTION DES PRODUITS
    // ============================================================================

    @Override
    @Transactional
    public void createProduct(CreateProductDTO createProductDTO) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users admin = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

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

    @Override
    public ProductListResponseDTO getAllProducts(int page, int size, String search, Long categoryId, Boolean status) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users admin = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

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

    @Override
    public ProductDetailsDTO getProductById(Long id) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users admin = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

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
}
