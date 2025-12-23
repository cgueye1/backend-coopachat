package com.example.coopachat.controllers;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.services.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour la gestion des entreprises
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "API pour la gestion des entreprises")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "Créer une entreprise",
            description = "Permet à un commercial de créer une nouvelle entreprise. " +
                         "L'entreprise est automatiquement associée au commercial connecté."
    )
    @PostMapping
    public ResponseEntity<String> createCompany(@RequestBody @Valid CreateCompanyDTO createCompanyDTO) {
        companyService.createCompany(createCompanyDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Entreprise créée avec succès");
    }
}

