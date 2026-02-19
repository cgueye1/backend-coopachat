package com.example.coopachat.controllers;

import com.example.coopachat.dtos.employees.DeliveryPreferenceDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.employees.AddressDTO;
import com.example.coopachat.dtos.employees.EmployeePersonalInfoDTO;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.delivery.DeliveryOptionDTO;
import com.example.coopachat.dtos.claim.CreateClaimDTO;
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.dtos.products.ProductCatalogueListResponseDTO;
import com.example.coopachat.dtos.products.ProductMobileDetailsDTO;
import com.example.coopachat.services.Employee.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour les actions du salarié.
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "API pour les actions du salarié")
public class EmployeeController {

    private final EmployeeService employeeService;

    // ============================================================================
    // ACCUEIL
    // ============================================================================

    @Operation(
            summary = "Accueil salarié",
            description = "Retourne les 4 derniers produits, 4 dernières catégories et une promo active si existante."
    )
    @GetMapping("/home")
    public ResponseEntity<HomeResponseDTO> getHome() {
        HomeResponseDTO response = employeeService.getHomeData();
        return ResponseEntity.ok(response);
    }

   // ============================================================================
    //CATALOGUE
    // ============================================================================

    @Operation(
            summary = "Lister les catégories (catalogue)",
            description = "Retourne la liste des catégories (id + name) pour le catalogue."
    )
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryListItemDTO>> getCatalogueCategories() {
        List<CategoryListItemDTO> categories = employeeService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
            summary = "Lister les produits du catalogue",
            description = "Retourne la liste paginée des produits du catalogue avec possibilité de filtrer par recherche et catégorie."
    )
    @GetMapping("/products")
    public ResponseEntity<ProductCatalogueListResponseDTO> getCatalogueProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId) {
        ProductCatalogueListResponseDTO response = employeeService.getCatalogueProducts(page, size, search, categoryId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les détails d'un produit",
            description = "Retourne les informations détaillées d'un produit spécifique par son ID."
    )
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductMobileDetailsDTO> getProductDetails(
            @PathVariable Long productId) {
        ProductMobileDetailsDTO productDetails = employeeService.getProductDetailsById(productId);
        return ResponseEntity.ok(productDetails);
    }

    // ============================================================================
    // 🛒 PANIER
    // ============================================================================

    @Operation(
            summary = "Ajouter un produit au panier",
            description = "Ajoute un produit au panier de l'utilisateur connecté. " +
                    "Si le produit est déjà dans le panier, augmente sa quantité."
    )
    @PostMapping("/cart/items/{productId}")
    public ResponseEntity<String> addToCart(@PathVariable Long productId) {
        employeeService.addProductToCart(productId);
        return ResponseEntity.ok("Produit ajouté avec succès à votre panier");
    }

    @Operation(
            summary = "Récupérer le panier",
            description = "Retourne tous les articles du panier de l'utilisateur connecté"
    )
    @GetMapping("/cart")
    public ResponseEntity<CartResponseDTO> getCart() {
        CartResponseDTO cart = employeeService.getCart();
        return ResponseEntity.ok(cart);
    }

    @Operation(
            summary = "Augmenter la quantité d'un produit",
            description = "Augmente de 1 la quantité d'un produit déjà présent dans le panier."
    )
    @PostMapping("/cart/items/{productId}/increase")
    public ResponseEntity<String> increaseProductQuantity(@PathVariable Long productId) {
        employeeService.increaseProductQuantity(productId);
        return ResponseEntity.ok("Quantité augmentée avec succès");
    }

    @Operation(
            summary = "Diminuer la quantité d'un produit",
            description = "Diminue de 1 la quantité d'un produit déjà présent dans le panier. " +
                    "Si la quantité atteint 0, l'article est supprimé du panier."
    )
    @PostMapping("/cart/items/{productId}/decrease")
    public ResponseEntity<String> decreaseProductQuantity(@PathVariable Long productId) {
        employeeService.decreaseProductQuantity(productId);
        return ResponseEntity.ok("Quantité diminuée avec succès");
    }

    @Operation(
            summary = "Supprimer un produit du panier",
            description = "Supprime complètement un produit du panier."
    )
    @DeleteMapping("/cart/items/{productId}")
    public ResponseEntity<String> removeProductFromCart(@PathVariable Long productId) {
        employeeService.removeProductFromCart(productId);
        return ResponseEntity.ok("Produit supprimé du panier");
    }

    // ============================================================================
    // 🛵 PRÉFÉRENCES DE LIVRAISON
    // ============================================================================

    @Operation(
            summary = "Enregistrer/modifier mes préférences de livraison",
            description = "Crée ou met à jour les préférences de livraison de l'utilisateur connecté " +
                    "(jours, créneaux horaires, mode de réception)"
    )
    @PostMapping("/delivery-preferences")
    public ResponseEntity<String> saveDeliveryPreference(@RequestBody DeliveryPreferenceDTO dto) {
        employeeService.saveDeliveryPreference(dto);
        return ResponseEntity.ok("Préférences de livraison enregistrées avec succès");
    }

    @Operation(
            summary = "Récupérer mes préférences de livraison",
            description = "Retourne les préférences de livraison de l'utilisateur connecté"
    )
    @GetMapping("/delivery-preferences")
    public ResponseEntity<DeliveryPreferenceDTO> getDeliveryPreference() {
        DeliveryPreferenceDTO preferences = employeeService.getDeliveryPreference();
        return ResponseEntity.ok(preferences);
    }

    // ============================================================================
    // INFOS USER
    // ============================================================================

    @Operation(
            summary = "Récupérer mes informations personnelles",
            description = "Retourne les informations personnelles de l'employé connecté " +
                    "(nom, prénom, téléphone, email, entreprise)"
    )
    @GetMapping("/personal-info")
    public ResponseEntity<EmployeePersonalInfoDTO> getPersonalInfo() {
        EmployeePersonalInfoDTO info = employeeService.getPersonalInfo();
        return ResponseEntity.ok(info);
    }

    @Operation(
            summary = "Modifier mes informations personnelles",
            description = "Met à jour uniquement le nom, prénom et téléphone (les autres champs sont ignorés)"
    )
    @PutMapping("/personal-info")
    public ResponseEntity<String> updatePersonalInfo(@RequestBody EmployeePersonalInfoDTO dto) {
        employeeService.updatePersonalInfo(dto);
        return ResponseEntity.ok("Informations personnelles mises à jour avec succès");
    }

    @Operation(
            summary = "Ajouter une adresse de livraison ",
            description = "Ajoute une nouvelle adresse de livraison  (max 3: Domicile/Bureau/Autre)"
    )
    @PostMapping("/adresses ")
    public ResponseEntity<String> createAddress(@RequestBody AddressDTO dto) {
       employeeService.createAddress(dto);
        return ResponseEntity.ok("Adresse ajoutée");
    }

    @Operation(summary = "Modifier une adresse de livraison", description = "Met à jour une adresse de livraison existante")
    @PutMapping("/adresses/{addressId}")
    public ResponseEntity<String> updateAddress(
            @PathVariable Long addressId,
            @RequestBody AddressDTO dto) {
        employeeService.updateAddress(addressId, dto);
        return ResponseEntity.ok("Adresse modifiée");
    }

    @Operation(summary = "Mes adresses de livraison ", description = "Liste les différentes adresses du salarié")
    @GetMapping("/adresses")
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        return ResponseEntity.ok(employeeService.getMyAddresses());
    }

    @Operation(
            summary = "Lister les options de livraison",
            description = "Retourne les options de livraison actives (fréquence : Hebdomadaire, etc.) à envoyer comme deliveryOptionId dans POST /orders."
    )
    @GetMapping("/delivery-options")
    public ResponseEntity<List<DeliveryOptionDTO>> getDeliveryOptions() {
        return ResponseEntity.ok(employeeService.getActiveDeliveryOptions());
    }

    // ============================================================================
    // Commande Salarié🛒
    // ============================================================================
    @Operation(
            summary = "Passer une commande",
            description = "Finalise la commande à partir du panier. Requiert deliveryOptionId (id d'une option renvoyée par GET /delivery-options). couponCode optionnel."
    )
    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderDTO dto) {
        OrderResponseDTO order = employeeService.createOrder(dto);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "Mes commandes",
            description = "Liste les commandes du client avec pagination. Filtres optionnels : status (ex. LIVREE), search (numéro). " +
                    "Infos livreur si En cours/Arrivé, rating/canRate si Livrée."
    )
    @GetMapping("/orders")
    public ResponseEntity<ClientOrderListResponseDTO> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(employeeService.getMyOrders(status, search, page, size));
    }

    @Operation(
            summary = "Détail d'une commande",
            description = "Détails d'une commande (client clique sur une commande). Inclut timeline, infos livreur si En cours de livraison, rating/canRate si Livrée."
    )
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ClientOrderDetailsDTO> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(employeeService.getOrderDetails(orderId));
    }

    @Operation(
            summary = "Infos de paiement pour une commande",
            description = "Retourne sous-total, frais de service, total et statut de paiement pour l'écran \"Payer la facture\"."
    )
    @GetMapping("/orders/{orderId}/payment-info")
    public ResponseEntity<PaymentInfoDTO> getPaymentInfo(@PathVariable Long orderId) {
        return ResponseEntity.ok(employeeService.getPaymentInfo(orderId));
    }

    @Operation(
            summary = "Payer une commande (simulation)",
            description = "Traite un paiement simulé : Mobile Money (opérateur requis) ou Carte bancaire (numéro 16 chiffres, MM/AA, CVV 3 chiffres). Commande doit être validée, pas encore payée."
    )
    @PostMapping("/orders/{orderId}/pay")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @PathVariable Long orderId,
            @RequestBody @Valid ProcessPaymentDTO dto) {
        return ResponseEntity.ok(employeeService.processPayment(orderId, dto));
    }

    @Operation(
            summary = "Historique des paiements",
            description = "Retourne la liste des paiements du salarié connecté (commandes payées), triée par date de paiement décroissante."
    )
    @GetMapping("/payment-history")
    public ResponseEntity<List<PaymentHistoryItemDTO>> getPaymentHistory() {
        return ResponseEntity.ok(employeeService.getPaymentHistory());
    }

    @Operation(
            summary = "Noter le livreur",
            description = "Envoie une note pour une commande livrée (bouton \"Noter le livreur\"). Possible uniquement si statut = LIVREE, pas déjà noté, note 1 à 5."
    )
    @PostMapping("/orders/{orderId}/review")
    public ResponseEntity<String> submitReview(
            @PathVariable Long orderId,
            @RequestBody @Valid SubmitReviewDTO dto) {
        employeeService.submitReview(orderId, dto);
        return ResponseEntity.ok("Avis enregistré avec succès");
    }

    @Operation(
            summary = "Soumettre une réclamation",
            description = "Soumet une réclamation sur une commande livrée : produits concernés, nature du problème, commentaire (optionnel)"
    )
    @PostMapping("/orders/{orderId}/claims")
    public ResponseEntity<String> submitClaim(
            @PathVariable Long orderId,
            @RequestBody @Valid CreateClaimDTO dto) {
        employeeService.submitClaim(orderId, dto);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body("Réclamation enregistrée");
    }

    @Operation(
            summary = "Annuler une commande",
            description = "Annule une commande. Uniquement si elle appartient au salarié connecté et si son statut est En attente (pas encore validée / dans une tournée)."
    )
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId) {
        employeeService.cancelOrder(orderId);
        return ResponseEntity.ok("Commande annulée");
    }

}