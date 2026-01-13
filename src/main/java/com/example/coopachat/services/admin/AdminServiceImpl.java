package com.example.coopachat.services.admin;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.dtos.products.CreateProductDTO;
import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Product;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.CategoryRepository;
import com.example.coopachat.repositories.ProductRepository;
import com.example.coopachat.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}
