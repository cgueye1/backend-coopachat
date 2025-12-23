package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Users;
import com.example.coopachat.repositories.CompanyRepository;
import com.example.coopachat.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implémentation du service de gestion des entreprises
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    public void createCompany(CreateCompanyDTO createCompanyDTO) {

        // Récupérer l'email de l'utilisateur connecté depuis le contexte Spring Security
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        
        String userEmail = authentication.getName();
        
        if (userEmail == null) {
            throw new RuntimeException("Email utilisateur introuvable");
        }
        
        // Récupérer le commercial connecté
        Users commercial = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Commercial introuvable"));
        
        // Générer le code unique de l'entreprise
        String companyCode = generateUniqueCompanyCode();
        
        // Créer l'entité Company à partir du DTO
        Company company = new Company();
        company.setCompanyCode(companyCode);
        company.setName(createCompanyDTO.getName());
        company.setSector(createCompanyDTO.getSector());
        company.setLocation(createCompanyDTO.getLocation());
        company.setContactName(createCompanyDTO.getContactName());
        company.setContactEmail(createCompanyDTO.getContactEmail());
        company.setContactPhone(createCompanyDTO.getContactPhone());
        company.setStatus(createCompanyDTO.getStatus());
        company.setNote(createCompanyDTO.getNote());
        company.setIsActive(true);
        company.setCommercial(commercial);
        
        // Sauvegarder l'entreprise en base
        companyRepository.save(company);
        
        log.info("Entreprise créée avec succès: {} (code: {}) par le commercial {}", 
                company.getName(), companyCode, commercial.getEmail());
    }

    /**
     * Génère un code unique pour l'entreprise
     * Format: ENT-YYYYMMDD-HHMMSS-XXX (ex: ENT-20250117-143025-001)
     *
     * @return Le code unique généré
     */
    private String generateUniqueCompanyCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String baseCode = "ENT-" + timestamp;
        String companyCode = baseCode;
        int counter = 1;
        
        // Vérifier l'unicité et incrémenter si nécessaire
        while (companyRepository.existsByCompanyCode(companyCode)) {
            // Ajouter un suffixe numérique formaté sur 3 chiffres (001, 002, 003...) pour garantir l'unicité
            companyCode = baseCode + "-" + String.format("%03d", counter);
            counter++;
        }
        
        return companyCode;
    }
}

