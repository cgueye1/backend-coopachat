package com.example.coopachat.controllers;

import com.example.coopachat.dtos.auth.ResetPasswordRequestDTO;
import com.example.coopachat.dtos.cart.CartResponseDTO;
import com.example.coopachat.dtos.categories.CategoryListItemDTO;
import com.example.coopachat.dtos.home.HomeResponseDTO;
import com.example.coopachat.dtos.products.ProductCatalogueListResponseDTO;
import com.example.coopachat.dtos.products.ProductMobileDetailsDTO;
import com.example.coopachat.services.Employee.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @Operation(
            summary = "Activer le compte salarié",
            description = "Active le compte d'un salarié et crée son mot de passe via le token d'invitation reçu par email. " +
                    "Le token doit être valide et non expiré."
    )
    @PostMapping("mobile/activate")
    public ResponseEntity<String> activateEmployeeAccount(@RequestBody @Valid ResetPasswordRequestDTO requestDTO) {
        employeeService.activateEmployeeAccount(
                requestDTO.getToken(),
                requestDTO.getNewPassword(),
                requestDTO.getConfirmPassword()
        );
        return ResponseEntity.ok("Compte activé avec succès. Vous pouvez maintenant vous connecter.");
    }

    @Operation(
            summary = "Accueil salarié",
            description = "Retourne les 4 derniers produits, 4 dernières catégories et une promo active si existante."
    )
    @GetMapping("/home")
    public ResponseEntity<HomeResponseDTO> getHome() {
        HomeResponseDTO response = employeeService.getHomeData();
        return ResponseEntity.ok(response);
    }

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
            description = "Retourne la liste paginée des produits du catalogue avec possibilité de filtrer par recherche et catégorie. " +
                    "Seuls les produits actifs sont affichés."
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
            description = "Retourne les informations détaillées d'un produit spécifique par son ID. " +
                    "Seuls les produits actifs sont accessibles."
    )
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductMobileDetailsDTO> getProductDetails(
            @PathVariable Long productId) {
        ProductMobileDetailsDTO productDetails = employeeService.getProductDetailsById(productId);
        return ResponseEntity.ok(productDetails);
    }
    // ============================================================================
    // 🛒 PANIER - AJOUTER PRODUIT
    // ============================================================================

    @Operation(
            summary = "Ajouter un produit au panier",
            description = "Ajoute un produit au panier de l'utilisateur connecté. " +
                    "Si le produit est déjà dans le panier, augmente sa quantité. " +
                    "La quantité par défaut est 1 "
    )
    @PostMapping("/cart/items/{productId}")
    public ResponseEntity<String> addToCart(@PathVariable Long productId){
        employeeService.addProductToCart(productId);
        return ResponseEntity.ok("Produit ajouté avec succès à votre panier");
    }

    @Operation(
            summary = "Récupérer le panier",
            description = "Retourne tous les articles du panier de l'utilisateur connecté"
    )
    @GetMapping("/cart/items")
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



}
