package com.example.coopachat.services.Employee;

import com.example.coopachat.dtos.UserDeliveryPrefererence.DeliveryPreferenceDTO;
import com.example.coopachat.dtos.cart.CartItemDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryHomeItemDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.coupons.CouponPromoDTO;
import com.example.coopachat.dtos.employees.EmployeePersonalInfoDTO;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.products.*;
import com.example.coopachat.entities.*;
import com.example.coopachat.entities.auth.ActivationCode;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.repositories.*;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Implémentation du service de gestion des salariés
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
    private final UserDeliveryPreferenceRepository userDeliveryPreferenceRepository;
    private final EmployeeRepository employeeRepository;

    // ============================================================================
    // 🔐 ACTIVATION D'UN COMPTE SALARIE
    // ============================================================================

    /**
     * Active le compte d'un salarié et crée son mot de passe via le token d'invitation
     */
    @Override
    @Transactional
    public void activateEmployeeAccount(String token, String newPassword, String confirmPassword) {
        // Validation simple des mots de passe
        // Vérifier que les deux mots de passe sont identiques
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Récupération et validation du token
        // Récupérer le token depuis la base de données (type EMPLOYEE_INVITATION)
        ActivationCode activationCode = activationCodeRepository.findByCodeAndTypeAndUsedFalse(token, CodeType.EMPLOYEE_INVITATION)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        // Vérifier que le token n'est pas expiré
        if (activationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        // Charger l'utilisateur concerné
        // Récupérer l'email depuis le token
        String email = activationCode.getEmail();

        // Récupérer l'utilisateur associé au token
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Sauvegarder le mot de passe et activer le compte
        // Encoder et sauvegarder le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));

        // Activer le compte
        user.setIsActive(true);

        userRepository.save(user);

        // Marquer le token comme consommé
        // Marquer le token comme utilisé pour éviter la réutilisation
        activationCode.setUsed(true);
        activationCodeRepository.save(activationCode);

        log.info("Compte salarié activé avec succès pour: {}", email);
    }

    // ============================================================================
    // 🏠 ACCUEIL SALARIÉ
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public HomeResponseDTO getHomeData() {
        // Authentification du salarié
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


        // Chargement des 4 derniers produits et catégories
        List<Product> latestProducts = productRepository.findTop4ByStatusTrueOrderByCreatedAtDesc();
        List<Category> latestCategories = categoryRepository.findTop4ByOrderByIdDesc();

        List<ProductPromoItemDTO> productItems = latestProducts.stream()
                .map(this::mapToProductPromoItemDTO)
                .collect(Collectors.toList());

        List<CategoryHomeItemDTO> categoryItems = latestCategories.stream()
                .map(this::mapToCategoryHomeItemDTO)
                .collect(Collectors.toList());

        // Promo active si disponible
        CouponPromoDTO promoDTO = couponRepository.findLatestActiveCoupon(LocalDateTime.now())
                .map(this::mapToCouponPromoDTO)
                .orElse(null);

        HomeResponseDTO response = new HomeResponseDTO();
        response.setFirstName(user.getFirstName());
        response.setProducts(productItems);
        response.setCategories(categoryItems);
        response.setActiveCoupon(promoDTO);

        log.info("Accueil salarié chargé pour {}", userEmail);
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

    @Override
    @Transactional
    public ProductCatalogueListResponseDTO getCatalogueProducts(int page, int size, String search, Long categoryId) {

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Normaliser le terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer la catégorie si categoryId est fourni
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        };
        // Récupérer la page de produits selon les filtres
        Page<Product> productPage;
        boolean activeOnly = true; // Pour les salariés, on montre seulement les produits actifs

        if(searchTerm != null && category != null){
            productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(searchTerm,category,activeOnly,pageable);
        }
        //Recherche seulement
        else if (searchTerm != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                    searchTerm, activeOnly, pageable);
        }
        // Catégorie seulement
        else if (category != null) {
            productPage = productRepository.findByCategoryAndStatus(category, activeOnly, pageable);
        }
        //Aucun filtre (tous les produits ACTIFS)
        else {
            productPage = productRepository.findByStatus(activeOnly, pageable);
        }
        // Mapper les produits vers ProductCatalogueItemDTO
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

            // Récupérer l'email de l'utilisateur connecté depuis le contexte Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                throw new RuntimeException("Utilisateur non authentifié");
            }

            String userEmail = authentication.getName();

            if (userEmail == null) {
                throw new RuntimeException("Email utilisateur introuvable");
            }

            Users employee = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Client introuvable"));

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
             CartItem existingItem = cartItemRepository.findByUserAndProduct(employee, product)
                    .orElse(null);  // ← Retourne null si pas trouvé
            if (existingItem != null){

                // CAS : Déjà dans panier → augmenter quantité

                // Calculer nouvelle quantité totale
                int newTotalQuantity = existingItem.getQuantity()+ requestedQuantity;

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
                newItem.setUser(employee);
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
                    employee.getEmail(), product.getName(), requestedQuantity);

        }

    @Override
    @Transactional
    public CartResponseDTO getCart() {

        // 1. Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Récupérer les articles du panier
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

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

        Users users = getCurrentUser();

        //Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        // Chercher l'article existant pour le users connecté et le produit concerné
        CartItem item = cartItemRepository.findByUserAndProduct(users, product)
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

        // 1. Récupérer l'utilisateur connecté
        Users user = getCurrentUser();

        // 2. Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec ID: " + productId));

        // 3. Chercher l'article du panier pour cet utilisateur et ce produit
        CartItem item = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Ce produit n'est pas dans votre panier"));

        // 4. Diminuer la quantité du produit dans le panier
        if (item.getQuantity() <= 1) {
            // Quantité = 1 → Supprimer l'article du panier
            cartItemRepository.delete(item);
            log.info("Produit supprimé du panier - User: {}, Produit: {}",
                    user.getEmail(), product.getName());
        }else {
            // Quantité > 1 → Diminuer de 1
            item.setQuantity(item.getQuantity()-1);
            cartItemRepository.save(item);
            log.info("Quantité diminuée - User: {}, Produit: {}, Nouvelle quantité: {}",
                    user.getEmail(), product.getName(), item.getQuantity());
        }

    }

    @Override
    @Transactional
    public void removeProductFromCart(Long productId) {
        // 1. Récupérer l'utilisateur connecté
        Users user = getCurrentUser();

        // 2. Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec ID: " + productId));

        // 3. Chercher l'article du panier pour cet utilisateur et ce produit
        CartItem item = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Ce produit n'est pas dans votre panier"));

        //4. supprimer le produit du panier
        cartItemRepository.delete(item);

        log.info("Produit {} supprimé du panier de {}",
                product.getName(), user.getEmail());
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

        // Récupérer user
        Users user = getCurrentUser();

        // Chercher ou créer ses préférences de livraison
        UserDeliveryPreference pref =userDeliveryPreferenceRepository.findByUser(user).orElse(new UserDeliveryPreference());

        // Mettre à jour
        pref.setUser(user);
        pref.setPreferredDays(dto.getPreferredDays());
        pref.setPreferredTimeSlot(dto.getPreferredTimeSlot());
        pref.setDeliveryMode(dto.getDeliveryMode());

        // Sauvegarder
        userDeliveryPreferenceRepository.save(pref);
        log.info("Préférences sauvegardées pour {}", user.getEmail());
    }

    @Override
    @Transactional
    public DeliveryPreferenceDTO getDeliveryPreference() {

        // 1. Récupérer l'utilisateur connecté
        Users user = getCurrentUser();

        // 2. Chercher les préférences en base
        UserDeliveryPreference preference = userDeliveryPreferenceRepository.findByUser(user)
                .orElseThrow(()-> new RuntimeException("Aucune préférence de livraison trouvée"));

        // 3. Convertir en DTO
        DeliveryPreferenceDTO dto = convertPreferenceToDto(preference);

      log.info("Préférences récupérées pour {}: {} jours, créneau: {}, mode: {}",
              user.getEmail(),
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

        Users userEmployee  = getCurrentUser();

        Employee employee = employeeRepository.findByUser(userEmployee)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        return new EmployeePersonalInfoDTO(
                employee.getId(),
                userEmployee.getFirstName(),
                userEmployee.getLastName(),
                userEmployee.getPhone(),
                userEmployee.getEmail(),
                employee.getCompany() != null? employee.getCompany().getName(): null
        );
    }
    @Override
    @Transactional
    public void updatePersonalInfo(EmployeePersonalInfoDTO updateRequest) {

        Users userEmployee = getCurrentUser();

        if (updateRequest.getFirstName() != null){
            userEmployee.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null){
            userEmployee.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null){
            userEmployee.setPhone(updateRequest.getPhone());
        }
        userRepository.save(userEmployee);

        log.info("Mise à jour réussie pour l'employé : {}", userEmployee.getFirstName());
    }



    /**
     * Convertit une entité UserDeliveryPreference en DeliveryPreferenceDTO
     */
    private DeliveryPreferenceDTO convertPreferenceToDto(UserDeliveryPreference entity) {
        DeliveryPreferenceDTO dto = new DeliveryPreferenceDTO();

        dto.setPreferredDays(entity.getPreferredDays());
        dto.setPreferredTimeSlot(entity.getPreferredTimeSlot());
        dto.setDeliveryMode(entity.getDeliveryMode());

        return dto;
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
     *
     * @param product L'entité Product à mapper
     * @return Le DTO de détails correspondant
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
        dto.setCurrentStock(product.getCurrentStock() != null ? product.getCurrentStock() : 0);
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