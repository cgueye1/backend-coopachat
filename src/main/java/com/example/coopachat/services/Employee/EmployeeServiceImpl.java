package com.example.coopachat.services.Employee;

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
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.dtos.products.*;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.*;
import com.example.coopachat.exceptions.ResourceNotFoundException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.fee.FeeService;
import com.example.coopachat.services.geocoding.PlacesService;
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
        }
        ;
        // Récupérer la page de produits selon les filtres
        Page<Product> productPage;
        boolean activeOnly = true; // Pour les salariés, on montre seulement les produits actifs

        if (searchTerm != null && category != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(searchTerm, category, activeOnly, pageable);
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

        // Mettre à jour
        pref.setEmployee(employee);
        pref.setPreferredDays(dto.getPreferredDays());
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

        // 2. Chercher les préférences en base
        EmployeeDeliveryPreference preference = employeeDeliveryPreferenceRepository.findByEmployee(employee)
                .orElseThrow(() -> new RuntimeException("Aucune préférence de livraison trouvée"));

        // 3. Convertir en DTO
        DeliveryPreferenceDTO dto = convertPreferenceToDto(preference);

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
        // 5. Créer l'adresse (formattedAddress + lat/long uniquement)
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

        // 4. Convertir en DTOs chaque adresse retournée (avec GPS)
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
    public List<com.example.coopachat.dtos.delivery.DeliveryOptionDTO> getActiveDeliveryOptions() {
        return deliveryOptionRepository.findByIsActiveTrue().stream()
                .map(opt -> new com.example.coopachat.dtos.delivery.DeliveryOptionDTO(
                        opt.getId(),
                        opt.getName(),
                        opt.getDescription(),
                        opt.getIsActive()
                ))
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderDTO dto) {

        // 1. Récupérer l'utilisateur et l'employé
        Users currentUser = getCurrentUser();
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // 2. Récupérer le panier
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
        cartItemRepository.deleteAll(cartItems);

        // 8. Préparer la réponse

        // Récupérer l’adresse de livraison principale de l’employé
        Address primaryAddress = addressRepository.findByEmployeeAndIsPrimaryTrue(employee);

        String deliveryAddress = primaryAddress != null && primaryAddress.getFormattedAddress() != null && !primaryAddress.getFormattedAddress().isBlank()
                ? primaryAddress.getFormattedAddress()
                : "Adresse non définie";

        return new OrderResponseDTO(
                nbArticles, // Nombre total d’articles commandés
                order.getDeliveryOption().getName(), // Option de livraison choisie
                order.getDeliveryDate(), // Date de livraison prévue
                deliveryAddress, // Adresse de livraison
                order.getTotalPrice() // Montant total de la commande
        );

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

        Payment payment = new Payment();
        payment.setOrder(order);
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
        dto.setPaymentTimingType(order.getPayment().getPaymentTiming().getLabel());
        dto.setPaymentStatusLabel(order.getPayment().getStatus().getLabel());
        return dto;
    }

    // ============================================================================
    // 🔧 MÉTHODES UTILITAIRES
    // ============================================================================

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