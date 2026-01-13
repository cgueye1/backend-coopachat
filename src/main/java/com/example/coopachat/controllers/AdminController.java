package com.example.coopachat.controllers;

import com.example.coopachat.dtos.categories.CreateCategoryDTO;
import com.example.coopachat.services.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion des actions de l'administrateur
 * Regroupe toutes les fonctionnalités liées au rôle Administrateur
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administrateur", description = "API pour la gestion des actions de l'administrateur (catalogue, catégories, fournisseurs)")
public class AdminController {

    private final AdminService adminService;

    // ============================================================================
    // 📁 GESTION DES CATÉGORIES
    // ============================================================================

    @Operation(
            summary = "Créer une nouvelle catégorie",
            description = "Permet à un administrateur de créer une nouvelle catégorie de produit. " +
                         "Le nom de la catégorie doit être unique."
    )
    @PostMapping("/categories")
    public ResponseEntity<String> createCategory(@RequestBody @Valid CreateCategoryDTO createCategoryDTO) {
        adminService.createCategory(createCategoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Catégorie créée avec succès");
    }
}
