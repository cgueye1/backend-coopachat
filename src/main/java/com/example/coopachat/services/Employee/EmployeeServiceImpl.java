package com.example.coopachat.services.Employee;

import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.employees.DeliveryPreferenceDTO;
import com.example.coopachat.dtos.cart.CartItemDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryHomeItemDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.coupons.CouponPromoDTO;
import com.example.coopachat.dtos.employees.AddressDTO;
import com.example.coopachat.dtos.employees.EmployeePersonalInfoDTO;
import com.example.coopachat.dtos.geocoding.PlaceDetailsResult;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.claim.ClaimDetailDTO;
import com.example.coopachat.dtos.claim.ClaimListItemDTO;
import com.example.coopachat.dtos.claim.ClaimListResponseDTO;
import com.example.coopachat.dtos.claim.CreateClaimDTO;
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.dtos.products.*;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.*;
import com.example.coopachat.exceptions.ResourceNotFoundException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.fee.FeeService;
import com.example.coopachat.services.geocoding.PlacesService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.ByteArrayOutputStream;
import com.itextpdf.layout.Document;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des actions de l'employé
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CouponRepository couponRepository;
    private final CartItemRepository cartItemRepository;
    private final EmployeeDeliveryPreferenceRepository employeeDeliveryPreferenceRepository;
    private final EmployeeRepository employeeRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final PlacesService placesService;
    private final DriverReviewRepository driverReviewRepository;
    private final FeeService feeService;
    private final PaymentRepository paymentRepository;
    private final ClaimRepository claimRepository;

    // ============================================================================
    // 🏠 ACCUEIL SALARIÉ
    // ============================================================================

    /**
     * Données de la page d'accueil salarié.
     * - Sans filtre (search + categoryId vides) : 4 derniers produits, pas de pagination.
     * - Avec filtre : produits recherchés/filtrés par catégorie, avec pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public HomeResponseDTO getHomeData(String search, Long categoryId, int page, int size) {
        // --- Utilisateur connecté ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String userEmail = authentication.getName();
        if (userEmail == null) {
            throw new RuntimeException("Email utilisateur introuvable");
        }
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // --- Décider : filtre actif ou pas ? ---
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        boolean hasFilter = searchTerm != null || categoryId != null;

        List<ProductPromoItemDTO> productItems;
        if (hasFilter) {
            // ----- CAS AVEC FILTRE : recherche et/ou catégorie → pagination -----
            Pageable pageable = PageRequest.of(page, size);
            Category category = null;
            if (categoryId != null) {
                category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            }
            // Appel BDD selon combinaison search + catégorie
            Page<Product> productPage;
            boolean activeOnly = true;
            if (searchTerm != null && category != null) {
                productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(searchTerm, category, activeOnly, pageable);
            } else if (searchTerm != null) {
                productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(searchTerm, activeOnly, pageable);
            } else {
                productPage = productRepository.findByCategoryAndStatus(category, activeOnly, pageable);
            }
            productItems = productPage.getContent().stream()
                    .map(this::mapToProductPromoItemDTO)
                    .collect(Collectors.toList());

            // Catégories + liste des coupons actifs (scope non produit/catégorie : panier, livraison, etc.)
            List<Category> latestCategories = categoryRepository.findTop4ByOrderByIdDesc();
            List<CategoryHomeItemDTO> categoryItems = latestCategories.stream()
                    .map(this::mapToCategoryHomeItemDTO)
                    .collect(Collectors.toList());
            List<CouponPromoDTO> promoCoupons = couponRepository.findActiveCouponsNotProductOrCategory(LocalDateTime.now())
                    .stream()
                    .map(this::mapToCouponPromoDTO)
                    .collect(Collectors.toList());

            // Réponse avec infos de pagination
            HomeResponseDTO response = new HomeResponseDTO();
            response.setFirstName(user.getFirstName());
            response.setProducts(productItems);
            response.setCategories(categoryItems);
            response.setActiveCoupons(promoCoupons);
            response.setTotalElements(productPage.getTotalElements());
            response.setTotalPages(productPage.getTotalPages());
            response.setCurrentPage(productPage.getNumber());
            response.setPageSize(productPage.getSize());
            response.setHasNext(productPage.hasNext());
            response.setHasPrevious(productPage.hasPrevious());
            return response;
        }

        // ----- CAS SANS FILTRE : 4 derniers produits, pas de pagination -----
        List<Product> products = productRepository.findTop4ByStatusTrueOrderByCreatedAtDesc();
        productItems = products.stream()
                .map(this::mapToProductPromoItemDTO)
                .collect(Collectors.toList());

        List<Category> latestCategories = categoryRepository.findTop4ByOrderByIdDesc();
        List<CategoryHomeItemDTO> categoryItems = latestCategories.stream()
                .map(this::mapToCategoryHomeItemDTO)
                .collect(Collectors.toList());
        List<CouponPromoDTO> promoCoupons = couponRepository.findActiveCouponsNotProductOrCategory(LocalDateTime.now())
                .stream()
                .map(this::mapToCouponPromoDTO)
                .collect(Collectors.toList());

        HomeResponseDTO response = new HomeResponseDTO();
        response.setFirstName(user.getFirstName());
        response.setProducts(productItems);
        response.setCategories(categoryItems);
        response.setActiveCoupons(promoCoupons);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryListItemDTO> getAllCategories() {
        // Liste simple des catégories (id + name)
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToCategoryListItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * Catalogue produits (page dédiée catalogue) : toujours paginé, avec search et categoryId optionnels.
     */
    @Override
    @Transactional
    public ProductCatalogueListResponseDTO getCatalogueProducts(int page, int size, String search, Long categoryId) {
        Pageable pageable = PageRequest.of(page, size);
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }

        // Choix de la requête selon search et catégorie
        Page<Product> productPage;
        boolean activeOnly = true;
        if (searchTerm != null && category != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(searchTerm, category, activeOnly, pageable);
        } else if (searchTerm != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(searchTerm, activeOnly, pageable);
        } else if (category != null) {
            productPage = productRepository.findByCategoryAndStatus(category, activeOnly, pageable);
        } else {
            productPage = productRepository.findByStatus(activeOnly, pageable);
        }

        List<ProductCatalogueItemDTO> productList = productPage.getContent().stream()
                .map(this::mapToProductCatalogueItemDTO)
                .collect(Collectors.toList());

        // Construire la réponse
        ProductCatalogueListResponseDTO response = new ProductCatalogueListResponseDTO();
        response.setContent(productList);
        response.setTotalElements(productPage.getTotalElements());
        response.setTotalPages(productPage.getTotalPages());
        response.setCurrentPage(productPage.getNumber());
        response.setPageSize(productPage.getSize());
        response.setHasNext(productPage.hasNext());
        response.setHasPrevious(productPage.hasPrevious());

        log.info("Catalogue produits - Page {} de {} (total: {} produits, recherche: '{}', catégorie: {})",
                page + 1, productPage.getTotalPages(), productPage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune", categoryId != null ? categoryId : "toutes");

        return response;
    }

    @Override
    @Transactional
    public ProductMobileDetailsDTO getProductDetailsById(Long productId) {

        // Récupérer le produit par son ID
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        // Mapper vers ProductDetailsDTO
        ProductMobileDetailsDTO productDetails = mapToProductDetailsDTO(product);
        log.info("Détails produit récupérés pour: {} (ID: {})", product.getName(), productId);
        return productDetails;

    }

    // ============================================================================
    // 🛒Panier
    // ============================================================================
    @Override
    @Transactional
    public void addProductToCart(Long productId) {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));


        // Trouver le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Déterminer quantité (1 par défaut)
        Integer requestedQuantity = 1;

        //Vérifier stock
        if (product.getCurrentStock() < requestedQuantity) {
            throw new RuntimeException("Stock insuffisant");
        }

        // Chercher si déjà dans panier
        CartItem existingItem = cartItemRepository.findByEmployeeAndProduct(employee, product)
                .orElse(null);  // ← Retourne null si pas trouvé
        if (existingItem != null) {

            // CAS : Déjà dans panier → augmenter quantité

            // Calculer nouvelle quantité totale
            int newTotalQuantity = existingItem.getQuantity() + requestedQuantity;

            // Re-vérifier stock avec total
            if (product.getCurrentStock() < newTotalQuantity) {
                throw new RuntimeException("Stock insuffisant pour quantité totale");
            }

            // Mettre à jour
            existingItem.setQuantity(newTotalQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // CAS : Nouveau dans panier → créer article

            CartItem newItem = new CartItem();
            newItem.setEmployee(employee);
            newItem.setUser(currentUser);
            newItem.setProduct(product);
            newItem.setQuantity(requestedQuantity);

            // Définir prix
            BigDecimal unitPrice = product.getPrice();
            newItem.setUnitPrice(unitPrice);

            // Vérifier promo
            Coupon activeCoupon = getActiveCouponForProduct(product);
            if (activeCoupon != null) {
                // Prix avec promo
                BigDecimal promoPrice = calculatePromoPrice(unitPrice, activeCoupon.getValue());
                newItem.setPromoPrice(promoPrice);
                newItem.setHasPromo(true);
            } else {
                // Pas de promo
                newItem.setPromoPrice(null);
                newItem.setHasPromo(false);
            }

            // Sauvegarder
            cartItemRepository.save(newItem);
        }
        log.info("Produit ajouté au panier - User: {}, Produit: {}, Quantité: {}",
                employee.getUser().getEmail(), product.getName(), requestedQuantity);

    }

    @Override
    @Transactional
    public CartResponseDTO getCart() {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Récupérer les articles du panier
        List<CartItem> cartItems = cartItemRepository.findByEmployee(employee);

        // 3. Convertir en DTOs
        List<CartItemDTO> itemDTOs = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 4. Calculer le total
        BigDecimal totalPrice = cartItems.stream()
                .map(cartItem -> cartItem.getSubtotal() != null ? cartItem.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);//Commence à 0, puis ajoute chaque élément l'un après l'autre

        // 5. Retourner la réponse
        CartResponseDTO response = new CartResponseDTO();
        response.setItems(itemDTOs);
        response.setTotalPrice(totalPrice);

        return response;
    }

    @Override
    @Transactional
    public void increaseProductQuantity(Long productId) {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));


        //Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Chercher l'article existant pour le users connecté et le produit concerné
        CartItem item = cartItemRepository.findByEmployeeAndProduct(employee, product)
                .orElseThrow(() -> new RuntimeException("Produit non dans le panier"));

        // Vérifier stock pour savoir si la quantité est suffisante ajouter à nouveau un produit dans le panier
        if (product.getCurrentStock() <= item.getQuantity()) {
            throw new RuntimeException("Stock insuffisant");

        }
        item.setQuantity(item.getQuantity() + 1);
        cartItemRepository.save(item);

    }

    @Override
    @Transactional
    public void decreaseProductQuantity(Long productId) {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec ID: " + productId));

        // 3. Chercher l'article du panier pour cet utilisateur et ce produit
        CartItem item = cartItemRepository.findByEmployeeAndProduct(employee, product)
                .orElseThrow(() -> new RuntimeException("Ce produit n'est pas dans votre panier"));

        // 4. Diminuer la quantité du produit dans le panier
        if (item.getQuantity() <= 1) {
            // Quantité = 1 → Supprimer l'article du panier
            cartItemRepository.delete(item);
            log.info("Produit supprimé du panier - User: {}, Produit: {}",
                    currentUser.getEmail(), product.getName());
        } else {
            // Quantité > 1 → Diminuer de 1
            item.setQuantity(item.getQuantity() - 1);
            cartItemRepository.save(item);
            log.info("Quantité diminuée - User: {}, Produit: {}, Nouvelle quantité: {}",
                    currentUser.getEmail(), product.getName(), item.getQuantity());
        }

    }

    @Override
    @Transactional
    public void removeProductFromCart(Long productId) {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));


        // 2. Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec ID: " + productId));

        // 3. Chercher l'article du panier pour cet utilisateur et ce produit
        CartItem item = cartItemRepository.findByEmployeeAndProduct(employee, product)
                .orElseThrow(() -> new RuntimeException("Ce produit n'est pas dans votre panier"));

        //4. supprimer le produit du panier
        cartItemRepository.delete(item);

        log.info("Produit {} supprimé du panier de {}",
                product.getName(), currentUser.getEmail());
    }

    // ============================================================================
    // Préférences de Livraisons🛵
    // ============================================================================
    @Override
    @Transactional
    public void saveDeliveryPreference(DeliveryPreferenceDTO dto) {

        // Validation rapide
        if (dto == null) {
            throw new RuntimeException("Les données sont nulles");
        }

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));


        // Chercher ou créer ses préférences de livraison puisque une préférence = un user soit ça existe soit ça n'existe pas
        EmployeeDeliveryPreference pref = employeeDeliveryPreferenceRepository.findByEmployee(employee).orElse(new EmployeeDeliveryPreference());

        // Mettre à jour (jours toujours stockés en français : LUNDI, MARDI, VENDREDI...)
        pref.setEmployee(employee);
        pref.setPreferredDays(normalizePreferredDaysToFrench(dto.getPreferredDays()));
        pref.setPreferredTimeSlot(dto.getPreferredTimeSlot());
        pref.setDeliveryMode(dto.getDeliveryMode());

        // Sauvegarder
       employeeDeliveryPreferenceRepository.save(pref);
        log.info("Préférences sauvegardées pour {}",employee.getUser().getEmail());
    }

    @Override
    @Transactional
    public DeliveryPreferenceDTO getDeliveryPreference() {

        // 1. Récupérer l'employé concerné
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Chercher les préférences en base (aucune préférence = DTO vide, pas d'erreur)
        EmployeeDeliveryPreference preference = employeeDeliveryPreferenceRepository.findByEmployee(employee)
                .orElse(null);

        // 3. Convertir en DTO ou retourner un DTO vide
        DeliveryPreferenceDTO dto = preference != null ? convertPreferenceToDto(preference) : new DeliveryPreferenceDTO();

        log.info("Préférences récupérées pour {}: {} jours, créneau: {}, mode: {}",
                currentUser.getEmail(),
                dto.getPreferredDays() != null ? dto.getPreferredDays().size() : 0,
                dto.getPreferredTimeSlot(),
                dto.getDeliveryMode());
        return dto;
    }

    // ============================================================================
    // Informations Personnelles 📋
    // ============================================================================

    @Override
    public EmployeePersonalInfoDTO getPersonalInfo() {

        Users userEmployee = getCurrentUser();

        Employee employee = employeeRepository.findByUser(userEmployee)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        return new EmployeePersonalInfoDTO(
                userEmployee.getFirstName(),
                userEmployee.getLastName(),
                userEmployee.getPhone(),
                userEmployee.getEmail(),
                employee.getCompany() != null ? employee.getCompany().getName() : null
        );
    }

    @Override
    @Transactional
    public void updatePersonalInfo(EmployeePersonalInfoDTO updateRequest) {

        Users userEmployee = getCurrentUser();

        if (updateRequest.getFirstName() != null) {
            userEmployee.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            userEmployee.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null) {
            userEmployee.setPhone(updateRequest.getPhone());
        }
        userRepository.save(userEmployee);

        log.info("Mise à jour réussie pour l'employé : {}", userEmployee.getFirstName());
    }

   // Flux saisi manuellement par l'utilisateur
    @Override
    @Transactional
    public void createAddress(AddressDTO dto) {

        // 1. Récupérer le user connecté
        Users currentUser = getCurrentUser();

        //Récupérer l'employé
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Vérifier s'il existe déjà une adresse de ce type
        boolean exists = addressRepository.existsByEmployeeAndDeliveryMode(employee, dto.getDeliveryMode());

        if (exists) {
            throw new RuntimeException(
                    "Vous avez déjà une adresse de type " + dto.getDeliveryMode());
        }
        // 3. Vérifier la limite (max 3 adresses)
        long count = addressRepository.countByEmployee(employee);
        if (count >= 3) {
            throw new RuntimeException("Vous ne pouvez pas avoir plus de 3 adresses");
        }
        // 4. Gérer l'adresse principale (si l'adresse passé en paramètre,  "isPrimary" est marqué comme true alors dans ce cas :)
        if (dto.isPrimary()) {
            //on va parcourir les adresses existantes et les mettre à "false "
            employee.getAddresses().forEach((addr -> addr.setPrimary(false)));
        }
        // 5. Créer l'adresse (formattedAddress + lat/long uniquement) — id ignoré (généré par la BDD)
        Address address = new Address();
        address.setEmployee(employee);
        address.setDeliveryMode(dto.getDeliveryMode());
        address.setFormattedAddress(dto.getFormattedAddress());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        address.setPrimary(dto.isPrimary());

        // 6. Sauvegarder
        addressRepository.save(address);

        log.info("Adresse créée pour l'employé {}: {}",
                employee.getUser().getFirstName(), dto.getDeliveryMode());

    }

    @Override
    @Transactional
    public void updateAddress(Long addressId, AddressDTO dto) {

        // 1. Récupérer l'utilisateur connecté et son profil employé
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Vérifier que l'adresse existe et appartient à l'employé
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));

        if (!address.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette adresse ne vous appartient pas");
        }

        // 3. Vérifier les conflits de type d'adresse
        // Si l'utilisateur change le type (ex: DOMICILE → BUREAU)
        if (!address.getDeliveryMode().equals(dto.getDeliveryMode())) {
            // Vérifier qu'il n'a pas déjà une autre adresse avec ce nouveau type
            boolean typeAlreadyExists = addressRepository.existsByEmployeeAndDeliveryModeAndIdNot(
                    employee, dto.getDeliveryMode(), addressId);

            if (typeAlreadyExists) {
                throw new RuntimeException("Vous avez déjà une adresse de type " + dto.getDeliveryMode());
            }
        }

        // 4. Gestion de l'adresse principale
        // Si l'utilisateur veut que cette adresse devienne principale
        if (dto.isPrimary() && !address.isPrimary()) {
            // Désactiver toutes les autres adresses principales
            employee.getAddresses().forEach(addr -> addr.setPrimary(false));
        }

        // 5. Mise à jour (formattedAddress + lat/long uniquement)
        if (dto.getDeliveryMode() != null) {
            address.setDeliveryMode(dto.getDeliveryMode());
        }
        if (dto.getFormattedAddress() != null) {
            address.setFormattedAddress(dto.getFormattedAddress());
        }
        if (dto.getLatitude() != null) {
            address.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            address.setLongitude(dto.getLongitude());
        }
        address.setPrimary(dto.isPrimary());

        // Sauvegarde automatique grâce à @Transactional
        addressRepository.save(address);
    }

    @Override
    public List<AddressDTO> getMyAddresses() {

        // 1. Récupérer l'utilisateur connecté
        Users currentUser = getCurrentUser();

        // 2. Récupérer l'employé associé
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 3. Récupérer toutes les adresses de cet employé
        List<Address> addresses = addressRepository.findByEmployee(employee);

        // 4. Convertir en DTOs chaque adresse retournée (avec id pour que le front puisse appeler updateAddress(id, ...))
        return addresses.stream()
                .map(address -> {
                    AddressDTO dto = new AddressDTO();
                    dto.setId(address.getId());
                    dto.setDeliveryMode(address.getDeliveryMode());
                    dto.setFormattedAddress(address.getFormattedAddress());
                    dto.setLatitude(address.getLatitude());
                    dto.setLongitude(address.getLongitude());
                    dto.setPrimary(address.isPrimary());
                    return dto;
                }).toList();
    }

    // ============================================================================
    // Commande Salarié🛒
    // ============================================================================

    @Override
    public List<DeliveryOptionDTO> getActiveDeliveryOptions() {
        return deliveryOptionRepository.findByIsActiveTrue().stream()
                .map(opt -> new DeliveryOptionDTO(
                        opt.getId(),
                        opt.getName(),
                        opt.getDescription(),
                        opt.getIsActive()
                ))
                .toList();
    }

    // ---------- previewOrder : étape 2 (Livraison) — aucun enregistrement en base ----------
    /**
     * Aperçu de la commande sans rien écrire en base (écran récap avant "Valider ma commande").
     */
    @Override
    @Transactional(readOnly = true)  // ← rien en base
    public OrderPreviewDTO previewOrder(CreateOrderDTO dto) {

        // 1. Récupérer employé + panier
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // Liste des lignes du panier (un seul panier par employé, mais plusieurs articles = plusieurs CartItem)
        List<CartItem> cartItems = cartItemRepository.findByEmployee(employee);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Votre panier est vide");
        }

        // 2. Option de livraison
        DeliveryOption deliveryOption = deliveryOptionRepository.findById(dto.getDeliveryOptionId())
                .orElseThrow(() -> new RuntimeException("Option introuvable"));

        // 3. Calculer total (à partir des lignes du panier, sans créer de commande)
        BigDecimal total = BigDecimal.ZERO;   // Montant total du panier avant coupon
        int nbArticles = 0;                   // Nombre total d’articles (somme des quantités)
        for (CartItem item : cartItems) {
            // Prix unitaire à utiliser : promo s’il existe, sinon prix normal
            BigDecimal prix = item.getPromoPrice() != null
                    ? item.getPromoPrice()
                    : item.getUnitPrice();
            total = total.add(prix.multiply(BigDecimal.valueOf(item.getQuantity()))); // Sous-total ligne = prix × quantité
            nbArticles += item.getQuantity(); // Cumuler le nombre d’articles (somme des quantités) pour l’affichage 
        }

        // 4. Appliquer coupon si fourni (ça une réduction supplémentaire si promo price est présent sur un article)
        if (dto.getCouponCode() != null && !dto.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(dto.getCouponCode())
                    .orElseThrow(() -> new RuntimeException("Coupon invalide ou expiré"));

            // Vérifier si le coupon est encore valide (dates)
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getStartDate())) {
                throw new RuntimeException("Ce coupon n'est pas encore valide");
            }
            if (now.isAfter(coupon.getEndDate())) {
                throw new RuntimeException("Ce coupon a expiré");
            }

             // Calculer la réduction selon le type
            BigDecimal discount;

            // Si la réduction est de type pourcentage, on calcule le montant à déduire
            // en multipliant le total par le pourcentage
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

             // Pourcentage : (total × valeur) ÷ 100
                BigDecimal pourcentage = coupon.getValue()
                        .divide(BigDecimal.valueOf(100)); // 10% → 0.10

                discount = total.multiply(pourcentage);

            } else {
                // Montant fixe : on vérifie que la valeur du coupon ne dépasse pas le total de la commande
                if (coupon.getValue().compareTo(total) > 0) {
                    throw new RuntimeException(
                            "Ce coupon ne peut pas être utilisé sur une commande inférieure à "
                                    + coupon.getValue() + " F"
                    );
                }
                // On déduit directement la valeur du coupon du total
                total = total.subtract(coupon.getValue());
            }
        }

        // 5. Adresse principale (pour l’affichage du récap)
        Address primaryAddress = addressRepository
                .findByEmployeeAndIsPrimaryTrue(employee);
        String deliveryAddress = primaryAddress != null && primaryAddress.getFormattedAddress() != null && !primaryAddress.getFormattedAddress().isBlank()
                ? primaryAddress.getFormattedAddress()
                : "Adresse non définie";

        // 6. Retourner preview ← RIEN SAUVEGARDÉ
        return new OrderPreviewDTO(
                nbArticles,
                deliveryOption.getName(),
                calculateDeliveryDate(deliveryOption),
                deliveryAddress,
                total
        );
    }

    // ---------- createOrder : étape 3 (Confirmation) — sauvegarde commande + paiement + vide panier ----------
    /**
     * Crée la commande en base, associe le paiement (Impayé), vide le panier. À appeler quand le salarié clique sur "Valider ma commande".
     */
    @Override
    @Transactional
    public void createOrder(CreateOrderDTO dto) {

        // 1. Récupérer l'utilisateur et l'employé
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Récupérer le panier (liste des lignes du panier : un seul panier par employé, plusieurs CartItem = plusieurs articles)
        List<CartItem> cartItems = cartItemRepository.findByEmployee(employee);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Votre panier est vide");
        }

        // 3. Récupérer l'option de livraison (obligatoire : fréquence choisie par le salarié)
        DeliveryOption deliveryOption = deliveryOptionRepository.findById(dto.getDeliveryOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Option de livraison introuvable"));

        // 4. Créer la commande
        Order order = new Order();
        order.setOrderNumber("CMD-" + (orderRepository.count() + 1));
        order.setEmployee(employee);
        order.setUser(currentUser);
        order.setStatus(OrderStatus.EN_ATTENTE);
        order.setDeliveryOption(deliveryOption);
        order.setDeliveryDate(calculateDeliveryDate(deliveryOption));

        // 5. Traiter chaque article du panier

        BigDecimal total = BigDecimal.ZERO;//Montant total de la commande
        int nbArticles = 0;//Nombre total d'articles de la commande

        //Maintenant après avoir récupérer les articles du panier , on va  Créer les articles pour la commande
        for (CartItem cartItem : cartItems) { //on parcourt les articles  du panier de l'employé
            //pour chaque article du panier on va récupérer le produit qu'il contient et vérifier son stock
            Product product = cartItem.getProduct();

            // Vérifier si le stock est suffisant pour la quantité demandée , sinon, erreur
            if (product.getCurrentStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuffisant pour " + product.getName() +
                        ". Stock disponible: " + product.getCurrentStock());
            }

            // Si le stock est suffisant, on crée l’article de la commande  à partir de l’article du panier
            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order); // Commande à laquelle l’article est rattaché
            orderItem.setProduct(product); // Produit concerné
            orderItem.setQuantity(cartItem.getQuantity()); // Quantité commandée pour ce produit
            orderItem.setUnitPrice(cartItem.getUnitPrice()); // Prix unitaire du produit
            orderItem.setPromoPrice(cartItem.getPromoPrice()); // Prix promotionnel s’il existe
            orderItem.calculateSubtotal(); // Calcule le sous-total selon le prix applicable


            order.getItems().add(orderItem);          // Ajouter l'article à la commande
            total = total.add(orderItem.getSubtotal()); // Cumuler le montant total
            nbArticles += orderItem.getQuantity();    // Cumuler le nombre d'articles

            // Mise à jour du stock du produit  après l'ajout de chaque article à la commande
            product.setCurrentStock(product.getCurrentStock() - orderItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalPrice(total); // On définit le montant total de la commande
        order.setTotalItems(nbArticles); // On définit le nombre total d’articles de la commande


        // 6. Appliquer le coupon s’il est fourni
        // Si l’utilisateur renseigne un code promo, on vérifie qu’il est valide et actif
        if (dto.getCouponCode() != null && !dto.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(dto.getCouponCode())
                    .orElseThrow(() -> new RuntimeException("Coupon invalide ou expiré"));

            // Vérifier si le coupon est encore valide (dates)
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getStartDate())){
                throw new RuntimeException("Ce coupon n'est pas encore valide");
            }
            if (now.isAfter(coupon.getEndDate())) {
                throw new RuntimeException("Ce coupon a expiré");
            }

            // Calculer la réduction selon le type
            BigDecimal discount ;

            // Si la réduction est de type pourcentage, on calcule le montant à déduire
            // en multipliant le total par le pourcentage
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

                // Pourcentage : (total × valeur) ÷ 100
                BigDecimal pourcentage = coupon.getValue()
                        .divide(BigDecimal.valueOf(100)); // 10% → 0.10

                discount = total.multiply(pourcentage);

            } else {
                // Si le coupon est de type montant fixe,
                // on vérifie que sa valeur n’est pas supérieure au total de la commande
                if (coupon.getValue().compareTo(total) > 0) {
                    throw new RuntimeException(
                            "Ce coupon ne peut pas être utilisé sur une commande inférieure à "
                                    + coupon.getValue() + " F"
                    );
                }

                // Montant fixe : on applique directement la valeur du coupon
                discount = coupon.getValue();
            }

           // Mise à jour du montant total de la commande (total - réduction)
            order.setTotalPrice(total.subtract(discount));

           // Association du coupon à la commande
            order.setCoupon(coupon);

        }
        // 7. Sauvegarder la commande et vider le panier
        Order savedOrder = orderRepository.save(order);

        // Créer le paiement avec statut Impayé (paymentMethod et paymentTiming seront remplis au moment du paiement)
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setStatus(PaymentStatus.UNPAID);
        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.setPayment(savedPayment);

        cartItemRepository.deleteAll(cartItems);//on vide le panier

    }

    // ============================================================================
    // 📋 MES COMMANDES (profil client)
    // ============================================================================

    // ---------- getMyOrders : liste "Mes commandes" avec filtres + pagination ----------
    @Override
    @Transactional(readOnly = true)
    public ClientOrderListResponseDTO getMyOrders(String status, String search, int page, int size) {
        // 1. Employé connecté (client = salarié)
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Filtre statut : si présent, on parse (ex. "LIVREE" -> OrderStatus.LIVREE) ; invalide = ignoré
        OrderStatus statusFilter = null;
        //le but est de convertir le statut en Enum OrderStatus
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusFilter = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) { } //si le statut n'est pas valide, on ignore statusFilter=null
        }
        //Normaliser terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // 3. Récupération paginée (tri du plus récent au plus ancien)
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByEmployeeWithFilters(employee, searchTerm, statusFilter, pageable);
        List<ClientOrderListItemDTO> dtos = orderPage.getContent().stream()
                .map(this::mapOrderToClientOrderListItemDTO)
                .collect(Collectors.toList());

        // 4. Réponse : liste + pagination "X commandes" sur l’écran)
        ClientOrderListResponseDTO response = new ClientOrderListResponseDTO();
        response.setOrders(dtos);
        response.setTotalElements(orderPage.getTotalElements());
        response.setTotalPages(orderPage.getTotalPages());
        response.setCurrentPage(orderPage.getNumber());
        return response;
    }

    // ---------- getOrderDetails : détail d'une commande (client clique sur une commande) ----------
    @Override
    @Transactional(readOnly = true)
    public ClientOrderDetailsDTO getOrderDetails(Long orderId) {
        // 1. Employé connecté
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Charger la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 3. Vérifier que la commande appartient à l'employé (sécurité)
        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        // 4. Construire le DTO détail (timeline, driver si EN_COURS/ARRIVE, rating/canRate si LIVREE)
        return buildClientOrderDetailsDTO(order);
    }

    // ---------- getPaymentInfo : infos paiement pour l'écran "Payer la facture" ----------
    @Override
    @Transactional(readOnly = true)
    public PaymentInfoDTO getPaymentInfo(Long orderId) {
        Users currentUser = getCurrentUser();

        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        //on récupère le sous-total de la commande
        BigDecimal subtotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        //on récupère les frais de service
        BigDecimal serviceFee = feeService.calculateTotalFees();
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;
        //on calcule le total de la commande
        BigDecimal total = subtotal.add(serviceFee);

        //on récupère le statut de paiement
        String paymentStatus = order.getPayment() != null && order.getPayment().getStatus() != null
                ? order.getPayment().getStatus().getLabel()
                : PaymentStatus.UNPAID.getLabel();

        return new PaymentInfoDTO(
                order.getOrderNumber(),
                order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null,
                order.getItems() != null ? order.getItems().size() : 0,
                subtotal,
                serviceFee,
                total,
                paymentStatus
        );
    }

    //---------------------- Traite un paiement (simulation) pour une commande-----------
    @Override
    @Transactional
    public PaymentResponseDTO processPayment(Long orderId, ProcessPaymentDTO request) {
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Cette commande est déjà payée");
        }
        if (order.getStatus() == OrderStatus.EN_ATTENTE) {
            throw new RuntimeException("Votre commande n'est pas encore validée");
        }
        if (request.getPaymentMethod() == null || request.getPaymentTiming() == null) {
            throw new RuntimeException("La méthode et le moment de paiement sont obligatoires");
        }
        // Paiement en espèces : interface dédiée au livreur uniquement (à la livraison)
        if (request.getPaymentMethod() == PaymentMethodType.CASH) {
            throw new RuntimeException("Le paiement en espèces est géré par le livreur à la livraison. Choisissez Mobile Money ou Carte bancaire.");
        }

        // Utiliser le paiement existant (créé à la commande) ou en créer un pour les anciennes commandes
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentTiming(request.getPaymentTiming());

        // on vérifie si le paiement est par mobile money ou par carte bancaire
        if (request.getPaymentMethod() == PaymentMethodType.MOBILE_MONEY) {
            // on vérifie si l'opérateur de mobile money est choisi
            if (request.getMobileOperator() == null) {
                throw new RuntimeException("Veuillez choisir un opérateur Mobile Money");
            }
            payment.setMobileOperator(request.getMobileOperator());
            log.info("💰 SIMULATION paiement {} pour commande {}",
                    request.getMobileOperator().getLabel(), order.getOrderNumber());
        } // si le paiement est par carte bancaire
        else if (request.getPaymentMethod() == PaymentMethodType.CREDIT_CARD) {
            // on vérifie si le numéro de carte est valide
            // replaceAll("\\s", "") => remplace tous les espaces par une chaîne vide
            // matches("\\d{16}") => vérifie si le numéro de carte est composé de 16 chiffres
            if (request.getCardNumber() == null ||
                    !request.getCardNumber().replaceAll("\\s", "").matches("\\d{16}")) {
                throw new RuntimeException("Numéro de carte invalide (16 chiffres requis)");
            }
            // on vérifie si la date d'expiration est valide
            // matches("(0[1-9]|1[0-2])/\\d{2}") => vérifie si la date d'expiration est composée d'un mois (01-12) et d'une année (2 chiffres)
            if (request.getCardExpiry() == null ||
                    !request.getCardExpiry().matches("(0[1-9]|1[0-2])/\\d{2}")) {
                throw new RuntimeException("Date d'expiration invalide (format MM/AA)");
            }
            // on vérifie si le CVV est valide
            // matches("\\d{3}") => vérifie si le CVV est composé de 3 chiffres
            if (request.getCardCvv() == null || !request.getCardCvv().matches("\\d{3}")) {
                throw new RuntimeException("CVV invalide (3 chiffres requis)");
            }
            log.info("💳 SIMULATION paiement carte pour commande {}", order.getOrderNumber());
        }

        String transactionRef = generateTransactionReference();
        payment.setTransactionReference(transactionRef);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);

        BigDecimal subtotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal serviceFee = feeService.calculateTotalFees();
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;
        BigDecimal totalPaid = subtotal.add(serviceFee);

        log.info("✅ Paiement simulé avec succès - Ref: {}", transactionRef);
        return new PaymentResponseDTO(
                true,
                "Paiement simulé avec succès !",
                transactionRef,
                savedPayment.getPaidAt(),
                totalPaid
        );
    }
    // ----------"Historique Paiement" ----------
    @Override
    public List<PaymentHistoryItemDTO> getPaymentHistory() {
        // 1. Récupérer l'utilisateur connecté
        Users currentUser = getCurrentUser();

        // 2. Trouver l'employé associé à cet utilisateur
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 3. Récupérer toutes les commandes de l'employé dont le paiement a le statut PAID
        List<Order> paidOrders = orderRepository.findPaidOrdersByEmployee(employee, PaymentStatus.PAID);

        // 4. Calculer les frais de service
        BigDecimal serviceFee = feeService.calculateTotalFees();
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;

        // 5. Transformer chaque commande en DTO pour l'historique des paiements
        List<PaymentHistoryItemDTO> result = new ArrayList<>();
        for (Order order : paidOrders) {
            Payment payment = order.getPayment(); //  pour chaque commande payé de l'employé  on recupére son paiement

            // Calculer le montant total payé = total de la commande + frais de service
            BigDecimal subtotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal amountPaid = subtotal.add(serviceFee);

            // Construire le DTO
            PaymentHistoryItemDTO dto = new PaymentHistoryItemDTO();
            dto.setOrderNumber(order.getOrderNumber());
            dto.setPaidAt(payment.getPaidAt());
            dto.setAmountPaid(amountPaid);
            dto.setPaymentMethodLabel(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getLabel() : null);
            dto.setMobileOperatorLabel(payment.getMobileOperator() != null ? payment.getMobileOperator().getLabel() : null);

            result.add(dto);
        }

        // 6. Retourner la liste des DTO
        return result;
    }

    // ---------- Génération  Facture ----------
    @Override
    public byte[] generateInvoicePdf(Long orderId) {

        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        // Vérifier que la commande est payée
        if (order.getPayment() == null ||
                order.getPayment().getStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Impossible de générer une facture pour une commande non payée");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // ═══════════════════════════════════
            // EN-TÊTE
            // ═══════════════════════════════════

            Paragraph title = new Paragraph("FACTURE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            document.add(new Paragraph("Coop Achat Salarié")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Dakar, Sénégal")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // INFOS FACTURE
            // ═══════════════════════════════════

            document.add(new Paragraph("Facture N° : " + order.getOrderNumber())
                    .setFontSize(12)
                    .setBold());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            document.add(new Paragraph("Date : " +
                    order.getCreatedAt().format(dateFormatter)));

            document.add(new Paragraph("Client : " +
                    employee.getUser().getFirstName() + " " +
                    employee.getUser().getLastName()));

            if (employee.getCompany() != null) {
                document.add(new Paragraph("Entreprise : " +
                        employee.getCompany().getName()));
            }

            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // TABLEAU DES PRODUITS
            // ═══════════════════════════════════

            document.add(new Paragraph("DÉTAIL DES ARTICLES")
                    .setFontSize(14)
                    .setBold());

            Table table = new Table(4);
            table.addHeaderCell("Produit");
            table.addHeaderCell("Qté");
            table.addHeaderCell("Prix unitaire");
            table.addHeaderCell("Total");


            for (OrderItem item : order.getItems()) {
                BigDecimal effectiveUnitPrice = item.getPromoPrice() != null
                        ? item.getPromoPrice()
                        : item.getUnitPrice();

                table.addCell(item.getProduct().getName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(effectiveUnitPrice + " F CFA");
                table.addCell(effectiveUnitPrice
                        .multiply(new BigDecimal(item.getQuantity())) + " F CFA");
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // TOTAUX
            // ═══════════════════════════════════

            BigDecimal subtotalOrder = order.getTotalPrice() != null
                    ? order.getTotalPrice()
                    : BigDecimal.ZERO;

            BigDecimal serviceFee = feeService.calculateTotalFees();
            if (serviceFee == null) serviceFee = BigDecimal.ZERO;

            BigDecimal totalPaid = subtotalOrder.add(serviceFee);

            document.add(new Paragraph("Sous-total : " + subtotalOrder + " F CFA")
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("Frais de service : " + serviceFee + " F CFA")
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("TOTAL : " + totalPaid + " F CFA")
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // PAIEMENT
            // ═══════════════════════════════════

            Payment payment = order.getPayment();

            document.add(new Paragraph("PAIEMENT")
                    .setFontSize(14)
                    .setBold());

            document.add(new Paragraph("Mode : " +
                    (payment.getPaymentMethod() != null ? payment.getPaymentMethod().getLabel() : "Non défini")));

            if (payment.getMobileOperator() != null) {
                document.add(new Paragraph("Opérateur : " +
                        payment.getMobileOperator().getLabel()));
            }

            document.add(new Paragraph("Statut : " +
                    payment.getStatus().getLabel()));

            document.add(new Paragraph("Référence : " +
                    payment.getTransactionReference()));

            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Date de paiement : " +
                    payment.getPaidAt().format(dateTimeFormatter)));

            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // LIVRAISON
            // ═══════════════════════════════════

            if (order.getStatus() == OrderStatus.LIVREE) {

                document.add(new Paragraph("LIVRAISON")
                        .setFontSize(14)
                        .setBold());

                Address address = employee.getAddresses().stream()
                        .filter(Address::isPrimary)
                        .findFirst()
                        .orElse(null);

                if (address != null) {
                    document.add(new Paragraph("Adresse : " +
                            address.getFormattedAddress()));
                }

                if (order.getDeliveryCompletedAt() != null) {
                    document.add(new Paragraph("Date de livraison : " +
                            order.getDeliveryCompletedAt().format(dateFormatter)));
                }

                if (order.getDeliveryTour() != null &&
                        order.getDeliveryTour().getDriver() != null) {
                    Driver driver = order.getDeliveryTour().getDriver();
                    document.add(new Paragraph("Livreur : " +
                            driver.getUser().getFirstName() + " " +
                            driver.getUser().getLastName()));
                }
            }

            document.add(new Paragraph(" "));

            // ═══════════════════════════════════
            // PIED DE PAGE
            // ═══════════════════════════════════

            document.add(new Paragraph("Merci pour votre confiance !")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));
            document.close();

            log.info("Facture générée pour commande {}", order.getOrderNumber());

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération facture", e);
            throw new RuntimeException("Erreur lors de la génération de la facture");
        }
    }

    // ----------"Noter le livreur" (bouton après livraison) ----------
    @Override
    @Transactional
    public void submitReview(Long orderId, SubmitReviewDTO reviewDTO) {
        // 1. Employé connecté
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. La commande doit appartenir au client
        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        // 3. On ne note que si la commande est livrée (pas en cours de livraison)
        if (order.getStatus() != OrderStatus.LIVREE) {
            throw new RuntimeException("Seule une commande livrée peut être notée");
        }

        // 4. Une seule note par commande : si déjà noté, on refuse (le client ne peut plus modifier)
        if (driverReviewRepository.existsByOrderId(orderId)) {
            throw new RuntimeException("Vous avez déjà noté cette livraison");
        }

        // 5. Note obligatoire entre 1 et 5
        if (reviewDTO.getRating() == null || reviewDTO.getRating() < 1 || reviewDTO.getRating() > 5) {
            throw new RuntimeException("La note doit être entre 1 et 5");
        }

        // 6. Livreur obligatoire pour lier l'avis à la commande
        if (order.getDeliveryTour() == null || order.getDeliveryTour().getDriver() == null) {
            throw new RuntimeException("Impossible d'associer un livreur à cette commande");
        }
        Driver driver = order.getDeliveryTour().getDriver();

        // 7. Créer l'avis (tags et comment optionnels)
        DriverReview review = new DriverReview();
        review.setOrder(order);
        review.setDriver(driver);
        review.setRating(reviewDTO.getRating());//Note donnée par l'employé (Les étoiles)
        review.setTags(reviewDTO.getTags() != null ? new ArrayList<>(reviewDTO.getTags()) : new ArrayList<>());//Tags donnés par l'employé(Les boutons qu’ils cliquent remplissent la liste)
        review.setComment(reviewDTO.getComment());//Commentaire donné par l'employé 
        driverReviewRepository.save(review);
        log.info("Avis enregistré pour la commande {} (note {}/5)", order.getOrderNumber(), reviewDTO.getRating());
    }

    // ---------- submitClaim : soumettre une réclamation sur une commande ----------
    @Override
    @Transactional
    public void submitClaim(Long orderId, CreateClaimDTO dto) {

        // 1. Récupérer l'utilisateur connecté
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Récupérer la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 3. Vérifier la propriété
        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        // 4. Vérifier le statut
        if (order.getStatus() != OrderStatus.LIVREE) {
            throw new RuntimeException("Seule une commande livrée peut faire l'objet d'une réclamation");
        }

        // 5. ⭐ RÉCUPÉRER LE PRODUIT CONCERNÉ
        OrderItem concernedItem = order.getItems().stream()
                .filter(item -> item.getId() != null &&
                        item.getId().equals(dto.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Le produit sélectionné n'appartient pas à cette commande"
                ));

        // 6. Créer la réclamation
        Claim claim = new Claim();
        claim.setOrder(order);
        claim.setEmployee(employee);
        claim.setOrderItem(concernedItem);  // ← UN seul produit
        claim.setProblemType(dto.getProblemType());
        claim.setComment(dto.getComment());
        claim.setStatus(ClaimStatus.EN_ATTENTE);

        // 7. Sauvegarder
        claimRepository.save(claim);

        log.info("Réclamation créée pour la commande {} - Produit: {} (nature: {})",
                order.getOrderNumber(),
                concernedItem.getProduct().getName(),
                dto.getProblemType().getLabel());
    }

    // ---------- getMyClaims : historique des réclamations du salarié connecté ----------
    @Override
    public ClaimListResponseDTO getMyClaims(int page, int size, ClaimStatus status) {
        // Récupérer l'employé connecté (propriétaire des réclamations)
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        // Liste paginée : uniquement les réclamations de cet employé, tri par date décroissante
        Pageable pageable = PageRequest.of(page, size);
        Page<Claim> claimPage = claimRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), status, pageable);
        // Mapper chaque réclamation en DTO de liste (vue globale / historique)
        List<ClaimListItemDTO> content = claimPage.getContent().stream()
                .map(this::mapClaimToListItemDTO)
                .collect(Collectors.toList());
        return new ClaimListResponseDTO(
                content,
                claimPage.getTotalElements(),
                claimPage.getTotalPages(),
                claimPage.getNumber(),
                claimPage.getSize(),
                claimPage.hasNext(),
                claimPage.hasPrevious()
        );
    }

    // ---------- getMyClaimById : détail d'une réclamation (uniquement si elle appartient au salarié) ----------
    @Override
    public ClaimDetailDTO getMyClaimById(Long id) {
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        // Sécurité : on ne retourne le détail que si la réclamation a été créée par cet employé
        if (!claim.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette réclamation ne vous appartient pas");
        }
        return mapClaimToDetailDTO(claim);
    }

      // ---------- cancelOrder : annuler une commande (uniquement si EN_ATTENTE) ----------
    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (!order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }
        if (order.getStatus() != OrderStatus.EN_ATTENTE) {
            throw new RuntimeException("Annulation impossible : seules les commandes en attente peuvent être annulées");
        }
        order.setStatus(OrderStatus.ANNULEE);
        orderRepository.save(order);
        log.info("Commande {} annulée par le salarié", order.getOrderNumber());
    }



    // ============================================================================
    // 🔧 MÉTHODES UTILITAIRES
    // ============================================================================

    /**
     * Mappe une réclamation vers le DTO de liste (une ligne de l'historique des retours).
     */
    private ClaimListItemDTO mapClaimToListItemDTO(Claim c) {
        ClaimListItemDTO dto = new ClaimListItemDTO();
        dto.setClaimId(c.getId());
        dto.setOrderNumber(c.getOrder() != null ? c.getOrder().getOrderNumber() : null);
        // Nom du salarié (déclarant)
        if (c.getEmployee() != null && c.getEmployee().getUser() != null) {
            Users u = c.getEmployee().getUser();
            dto.setEmployeeName(u.getFirstName() + " " + u.getLastName());
        } else {
            dto.setEmployeeName(null);
        }
        // Produit concerné : nom, image, quantité
        if (c.getOrderItem() != null && c.getOrderItem().getProduct() != null) {
            dto.setProductName(c.getOrderItem().getProduct().getName());
            dto.setProductImage(c.getOrderItem().getProduct().getImage());
            dto.setQuantity(c.getOrderItem().getQuantity());
        } else {
            dto.setProductName(null);
            dto.setProductImage(null);
            dto.setQuantity(null);
        }
        dto.setProblemTypeLabel(c.getProblemType() != null ? c.getProblemType().getLabel() : null);
        dto.setStatus(c.getStatus() != null ? c.getStatus().getLabel() : null);
        dto.setCreatedAt(c.getCreatedAt());
        // Décision et montant remboursé (si la réclamation a été traitée)
        dto.setDecisionLabel(c.getDecisionType() != null ? c.getDecisionType().getLabel() : null);
        dto.setRefundAmount(c.getRefundAmount());
        return dto;
    }

    /**
     * Mappe une réclamation vers le DTO de détail (écran « Voir détails » au clic).
     */
    private ClaimDetailDTO mapClaimToDetailDTO(Claim c) {
        ClaimDetailDTO dto = new ClaimDetailDTO();
        dto.setClaimId(c.getId());
        dto.setOrderNumber(c.getOrder() != null ? c.getOrder().getOrderNumber() : null);
        dto.setStatus(c.getStatus() != null ? c.getStatus().getLabel() : null);
        dto.setCreatedAt(c.getCreatedAt());
        // Infos du salarié (nom, téléphone)
        if (c.getEmployee() != null && c.getEmployee().getUser() != null) {
            Users u = c.getEmployee().getUser();
            dto.setEmployeeName(u.getFirstName() + " " + u.getLastName());
            dto.setEmployeePhone(u.getPhone());
        }
        // Produit concerné : quantité, sous-total, id / nom / image
        if (c.getOrderItem() != null) {
            OrderItem oi = c.getOrderItem();
            dto.setQuantityOrdered(oi.getQuantity());
            dto.setSubtotalProduct(oi.getSubtotal());
            if (oi.getProduct() != null) {
                dto.setProductId(oi.getProduct().getId());
                dto.setProductName(oi.getProduct().getName());
                dto.setProductImage(oi.getProduct().getImage());
            }
        }
        dto.setProblemTypeLabel(c.getProblemType() != null ? c.getProblemType().getLabel() : null);
        dto.setComment(c.getComment());
        dto.setPhotoUrls(c.getPhotoUrls());
        // Décision (réintégration / remboursement) et motif de rejet si rejeté
        dto.setDecisionTypeLabel(c.getDecisionType() != null ? c.getDecisionType().getLabel() : null);
        dto.setRefundAmount(c.getRefundAmount());
        dto.setRejectionReason(c.getRejectionReason());
        return dto;
    }

    // ════════════════════════════════════════════
    // Génération Référence de  paiement 
    // ════════════════════════════════════════════

    private String generateTransactionReference() { 
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        //on génère un UUID unique en faisant un substring de 6 caractères et en mettant en majuscule
        String unique = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
        return "TXN-" + date + "-" + unique;
    }
    // ---------- buildClientOrderDetailsDTO : détail d'une commande (client clique sur une commande) ----------
    private ClientOrderDetailsDTO buildClientOrderDetailsDTO(Order order) {
        ClientOrderDetailsDTO dto = new ClientOrderDetailsDTO();
        dto.setOrderId(order.getId());//ID de la commande
        dto.setOrderNumber(order.getOrderNumber());//Numéro de la commande
        dto.setOrderDate(order.getDeliveryDate() != null ? order.getDeliveryDate() : (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null));//Date de la commande
        dto.setStatusLabel(order.getStatus() != null ? order.getStatus().getLabel() : "");//Statut de la commande
        dto.setProductCount(order.getItems() != null ? order.getItems().size() : 0);//Nombre total d'articles commandés
        dto.setTotalAmount(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);//Montant total de la commande
        dto.setDeliveryAddress(getDeliveryAddressFromOrder(order));//Adresse de livraison
        dto.setDeliveryDate(order.getDeliveryDate());//Date de livraison
        dto.setCreatedAt(order.getCreatedAt());//Date de création de la commande
        dto.setValidatedAt(order.getValidatedAt());//Date de validation de la commande
        dto.setDeliveryStartedAt(order.getDeliveryStartedAt());//Date de début de livraison
        dto.setDeliveryArrivedAt(order.getDeliveryArrivedAt());//Date d'arrivée de la commande
        dto.setDeliveryCompletedAt(order.getDeliveryCompletedAt());//Date de fin de livraison
        //Liste des articles commandés
        List<ClientOrderItemDTO> items = new ArrayList<>();
        //On parcourt la liste des articles commandés et on crée un DTO pour chaque article
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                ClientOrderItemDTO itemDto = new ClientOrderItemDTO();
                itemDto.setProductName(oi.getProduct() != null ? oi.getProduct().getName() : "");
                itemDto.setQuantity(oi.getQuantity());
                itemDto.setUnitPrice(oi.getUnitPrice());
                itemDto.setImageUrl(oi.getProduct() != null ? oi.getProduct().getImage() : null);
                items.add(itemDto);
            }
        }
        dto.setItems(items);//Liste des articles commandés
        // Infos livreur (nom, téléphone) : uniquement si EN_COURS ou ARRIVE. Si statut = Validé seulement → pas d’infos livreur (null).
        // Les infos de livraison (adresse, date) sont toujours dans le DTO. Les boutons (Noter, Télécharger facture, Réclamation) s’affichent côté UI si statut = Livrée.
        if (order.getStatus() == OrderStatus.EN_COURS || order.getStatus() == OrderStatus.ARRIVE) {
            dto.setDriver(buildDriverInfoForClient(order));
        } else {
            dto.setDriver(null);
        }
        //Les infos du paiement
        dto.setPaymentTimingType(order.getPayment().getPaymentTiming() != null ? order.getPayment().getPaymentTiming().getLabel() : null);
        dto.setPaymentStatusLabel(order.getPayment().getStatus().getLabel());
        return dto;
    }

    /**
     * Mappe une commande vers un item de la liste "Mes commandes".
     * - driver : renseigné uniquement si statut = EN_COURS ou ARRIVE (en cours de livraison → afficher nom + téléphone).
     * - Noter / canRate : si LIVREE, afficher la note si déjà noté, sinon canRate = true (bouton "Noter").
     */
    private ClientOrderListItemDTO mapOrderToClientOrderListItemDTO(Order order) {
        ClientOrderListItemDTO dto = new ClientOrderListItemDTO();
        dto.setOrderId(order.getId());//ID de la commande
        dto.setOrderNumber(order.getOrderNumber());//Numéro de la commande
        dto.setDeliveryAddress(getDeliveryAddressFromOrder(order));//Adresse de livraison
        dto.setOrderDate(order.getDeliveryDate() != null ? order.getDeliveryDate() : (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null));//Date de la commande
        dto.setItemCount(order.getTotalItems() != null ? order.getTotalItems() : 0);//Nombre total d'articles commandés
        dto.setStatusLabel(order.getStatus() != null ? order.getStatus().getLabel() : "");//Statut de la commande

        // Infos livreur : seulement si En cours de livraison ou Arrivé (sinon null → pas d'affichage)
        if (order.getStatus() == OrderStatus.EN_COURS || order.getStatus() == OrderStatus.ARRIVE) {
            dto.setDriver(buildDriverInfoForClient(order));//Informations du livreur
        } else {
            dto.setDriver(null);//Pas de livreur
        }

        // Si commande livrée : afficher les étoiles si déjà noté, sinon canRate = true (bouton "Noter")
        if (order.getStatus() == OrderStatus.LIVREE) {
            Optional<DriverReview> existingReview = driverReviewRepository.findByOrder(order);// Récupérer l'avis si déjà noté
            if (existingReview.isPresent()) {
                dto.setRating(existingReview.get().getRating());//Note donnée par l'employé
                dto.setCanRate(false);//Pas de bouton "Noter"
            } else {
                dto.setRating(null);//Pas de note
                dto.setCanRate(true);//Bouton "Noter"
            }
        } else {
            dto.setRating(null);//Pas de note
            dto.setCanRate(false);//Pas de bouton "Noter"
        }
        return dto;
    }

    private DriverInfoForClientDTO buildDriverInfoForClient(Order order) {
        if (order.getDeliveryTour() == null || order.getDeliveryTour().getDriver() == null) return null;
        Driver driver = order.getDeliveryTour().getDriver();
        Users user = driver.getUser();
        if (user == null) return null;
        String name = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        return new DriverInfoForClientDTO(name.trim(), user.getPhone(), null);
    }

    /** Adresse de livraison : on utilise uniquement formattedAddress s'il est présent, sinon null. */
    private String getDeliveryAddressFromOrder(Order order) {
        if (order.getEmployee() == null || order.getEmployee().getAddresses() == null || order.getEmployee().getAddresses().isEmpty()) {
            return null;
        }
        Address addr = order.getEmployee().getAddresses().stream()
                .filter(Address::isPrimary)
                .findFirst()
                .orElse(null);
        if (addr == null) return null;
        return (addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank()) ? addr.getFormattedAddress() : null;
    }

    /**
     * Calcule la date de livraison
     */

    private  LocalDate calculateDeliveryDate (DeliveryOption deliveryOption) {
        LocalDate today = LocalDate.now();

        switch (deliveryOption.getName().toLowerCase()){
            case "hebdomadaire":
                return today.plusWeeks(1);// Même jour, semaine suivante

            case "bimensuelle":
                return today.plusWeeks(2); // Même jour, dans 2 semaines

            case "mensuelle":
                return today.plusMonths(1); // Même jour, mois suivant

            default:
                return today.plusWeeks(1);
        }
    }

    /**
     * Convertit une entité EmployeeDeliveryPreference en DeliveryPreferenceDTO
     */
    private DeliveryPreferenceDTO convertPreferenceToDto(EmployeeDeliveryPreference entity) {
        DeliveryPreferenceDTO dto = new DeliveryPreferenceDTO();
        dto.setId(entity.getId());
        dto.setPreferredDays(entity.getPreferredDays());
        dto.setPreferredTimeSlot(entity.getPreferredTimeSlot());
        dto.setDeliveryMode(entity.getDeliveryMode());
        return dto;
    }

    /**
     * Normalise les jours en français (LUNDI, MARDI, VENDREDI...).
     * Accepte en entrée anglais (MONDAY, FRIDAY) ou déjà français ; stockage toujours en français.
     */
    private static java.util.Set<String> normalizePreferredDaysToFrench(java.util.Set<String> days) {
        if (days == null || days.isEmpty()) return days;
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String day : days) {
            if (day != null && !day.isBlank()) {
                out.add(dayOfWeekToFrench(day.trim().toUpperCase()));
            }
        }
        return out;
    }

    private static String dayOfWeekToFrench(String day) {
        return switch (day) {
            case "MONDAY" -> "LUNDI";
            case "TUESDAY" -> "MARDI";
            case "WEDNESDAY" -> "MERCREDI";
            case "THURSDAY" -> "JEUDI";
            case "FRIDAY" -> "VENDREDI";
            case "SATURDAY" -> "SAMEDI";
            case "SUNDAY" -> "DIMANCHE";
            default -> day; // déjà en français (LUNDI, etc.) ou inchangé
        };
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();

        // IDs et quantité (toujours présents)
        dto.setId(cartItem.getId());
        dto.setQuantity(cartItem.getQuantity());

        // Prix (peuvent être null)
        dto.setUnitPrice(cartItem.getUnitPrice());
        dto.setPromoPrice(cartItem.getPromoPrice());
        dto.setHasPromo(cartItem.getHasPromo());
        dto.setSubtotal(cartItem.getSubtotal());

        // Infos produit
        Product product = cartItem.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setImageUrl(product.getImage());

            // Catégorie (peut être null)
            if (product.getCategory() != null) {
                dto.setCategoryName(product.getCategory().getName());
            }
        }

        return dto;

    }

    /**
     * Mapper un Product vers ProductCatalogueItemDTO
     */
    private ProductCatalogueItemDTO mapToProductCatalogueItemDTO(Product product) {
        ProductCatalogueItemDTO dto = new ProductCatalogueItemDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setImage(product.getImage());
        dto.setOriginalPrice(product.getPrice());

        // Calculer les promotions
        Coupon coupon = getActiveCouponForProduct(product);
        if (coupon != null) {
            BigDecimal promoPrice = calculatePromoPrice(product.getPrice(), coupon.getValue());
            dto.setPromoPrice(promoPrice);
            dto.setDiscountPercent(coupon.getValue().setScale(0, RoundingMode.HALF_UP).intValue());
            dto.setHasPromo(true);
        } else {
            dto.setPromoPrice(null);
            dto.setDiscountPercent(null);
            dto.setHasPromo(false);
        }

        return dto;
    }

    private ProductPromoItemDTO mapToProductPromoItemDTO(Product product) {
        ProductPromoItemDTO dto = new ProductPromoItemDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setImage(product.getImage());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        dto.setOriginalPrice(product.getPrice());

        // Calcul du prix promo si coupon actif
        Coupon coupon = getActiveCouponForProduct(product);
        if (coupon != null) {
            BigDecimal promoPrice = calculatePromoPrice(product.getPrice(), coupon.getValue());
            dto.setPromoPrice(promoPrice);
            dto.setDiscountPercent(coupon.getValue().setScale(0, RoundingMode.HALF_UP).intValue());
            dto.setHasPromo(true);
        } else {
            dto.setPromoPrice(null);
            dto.setDiscountPercent(null);
            dto.setHasPromo(false);
        }

        return dto;
    }

    private CategoryHomeItemDTO mapToCategoryHomeItemDTO(Category category) {
        CategoryHomeItemDTO dto = new CategoryHomeItemDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        return dto;
    }

    private CategoryListItemDTO mapToCategoryListItemDTO(Category category) {
        CategoryListItemDTO dto = new CategoryListItemDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    private CouponPromoDTO mapToCouponPromoDTO(Coupon coupon) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        CouponPromoDTO dto = new CouponPromoDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setValue(coupon.getValue());
        dto.setValidFrom(coupon.getStartDate() != null ? coupon.getStartDate().format(formatter) : null);
        dto.setValidTo(coupon.getEndDate() != null ? coupon.getEndDate().format(formatter) : null);
        return dto;
    }


    private Coupon getActiveCouponForProduct(Product product) {
        LocalDateTime now = LocalDateTime.now();
        // Priorité coupon produit, sinon coupon catégorie
        Coupon productCoupon = product.getCoupon();
        if (isCouponActive(productCoupon, now)) {
            return productCoupon;
        }

        Coupon categoryCoupon = product.getCategory() != null ? product.getCategory().getCoupon() : null;
        if (isCouponActive(categoryCoupon, now)) {
            return categoryCoupon;
        }

        return null;
    }

    private boolean isCouponActive(Coupon coupon, LocalDateTime now) {
        if (coupon == null) {
            return false;
        }
        // Coupon actif et période valide
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            return false;
        }
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            return false;
        }
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            return false;
        }
        return coupon.getStatus() == CouponStatus.ACTIVE;
    }

    private BigDecimal calculatePromoPrice(BigDecimal originalPrice, BigDecimal percent) {
        if (originalPrice == null || percent == null) {
            return null;
        }
        // Prix promo = prix - (prix * pourcentage)
        BigDecimal discount = originalPrice
                .multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal result = originalPrice.subtract(discount);
        return result.max(BigDecimal.ZERO);
    }

    /**
     * Mappe une entité Product vers un ProductDetailsDTO
     */
    private ProductMobileDetailsDTO mapToProductDetailsDTO(Product product) {
        ProductMobileDetailsDTO dto = new ProductMobileDetailsDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImage(product.getImage());
        dto.setBrand(product.getBrand());

        // Catégorie
        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setOriginalPrice(product.getPrice());
        Coupon coupon = getActiveCouponForProduct (product);
        if (coupon != null) {
            // PROMO EXISTE
            BigDecimal promoPrice = calculatePromoPrice(product.getPrice(), coupon.getValue());
            dto.setPromoPrice(promoPrice);
            dto.setDiscountPercent(coupon.getValue().setScale(0, RoundingMode.HALF_UP).intValue());
            dto.setHasPromo(true);
        } else {
            // PAS DE PROMO
            dto.setPromoPrice(null);
            dto.setDiscountPercent(null);
            dto.setHasPromo(false);
        }
        // Stock
        Integer stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        dto.setCurrentStock(stock);
        dto.setCurrentStockStatus(stock != null && stock > 0 ? "En stock" : EtatStock.RUPTURE.getLabel()); // "Rupture" si stock = 0
        dto.setStatus(product.getStatus());


        return dto;
    }

    /**
     * Récupère l'utilisateur actuellement connecté
     * @return Users l'utilisateur connecté
     * @throws RuntimeException si aucun utilisateur n'est authentifié
     */
    private Users getCurrentUser() {
        // 1. Récupérer l'authentification Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Vérifier que l'utilisateur est bien authentifié
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        // 3. Récupérer l'email (username) de l'utilisateur
        String userEmail = authentication.getName();

        // 4. Chercher l'utilisateur dans la base de données
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable avec email: " + userEmail
                ));
    }
}