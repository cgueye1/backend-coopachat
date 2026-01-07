package com.example.coopachat.services;

import com.example.coopachat.dtos.CompanyListResponseDTO;
import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.dtos.CompanyListItemDTO;
import com.example.coopachat.dtos.CompanyDetailsDTO;
import com.example.coopachat.dtos.UpdateCompanyDTO;
import com.example.coopachat.dtos.UpdateCompanyStatusDTO;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.CodeType;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.CompanyRepository;
import com.example.coopachat.repositories.EmployeeRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des actions du commercial
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommercialServiceImpl implements CommercialService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ActivationCodeService activationCodeService;
    private final PasswordEncoder passwordEncoder;

    // ============================================================================
    // 🏢 GESTION DES ENTREPRISES
    // ============================================================================

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

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent créer des entreprises");
        }

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

    @Override
    public CompanyListResponseDTO getAllCompanies(int page, int size) {

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

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs entreprises");
        }

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Récupérer la page d'entreprises du commercial
        Page<Company> companyPage = companyRepository.findByCommercial(commercial, pageable);

        // Mapper les entités Company vers CompanyListItemDTO
        List<CompanyListItemDTO> companyList = companyPage.getContent().stream()
                .map(this::mapToCompanyListItemDTO)
                .collect(Collectors.toList());

        // Créer la réponse avec pagination
        CompanyListResponseDTO response = new CompanyListResponseDTO();
        response.setContent(companyList);
        response.setTotalElements(companyPage.getTotalElements());
        response.setTotalPages(companyPage.getTotalPages());
        response.setCurrentPage(companyPage.getNumber());
        response.setPageSize(companyPage.getSize());
        response.setHasNext(companyPage.hasNext());
        response.setHasPrevious(companyPage.hasPrevious());

        log.info("Page {} de {} entreprises récupérée pour le commercial {} (total: {} entreprises)", 
                page + 1, companyPage.getTotalPages(), commercial.getEmail(), companyPage.getTotalElements());

        return response;
    }

    @Override
    public CompanyDetailsDTO getCompanyById(Long id) {

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

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs entreprises");
        }

        // Récupérer l'entreprise par son ID
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        // Vérifier que l'entreprise appartient au commercial connecté
        if (!company.getCommercial().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
        }

        // Mapper l'entité Company vers CompanyDetailsDTO
        CompanyDetailsDTO companyDetails = mapToCompanyDetailsDTO(company);

        log.info("Détails de l'entreprise {} récupérés par le commercial {}", 
                company.getName(), commercial.getEmail());

        return companyDetails;
    }

    @Override
    public void updateCompany(Long id, UpdateCompanyDTO updateCompanyDTO) {

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

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent modifier leurs entreprises");
        }

        // Récupérer l'entreprise par son ID
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        // Vérifier que l'entreprise appartient au commercial connecté
        if (!company.getCommercial().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
        }

        // Mettre à jour les champs de l'entreprise (seulement si non null)
        if (updateCompanyDTO.getName() != null) {
            company.setName(updateCompanyDTO.getName());
        }
        if (updateCompanyDTO.getSector() != null) {
            company.setSector(updateCompanyDTO.getSector());
        }
        if (updateCompanyDTO.getLocation() != null) {
            company.setLocation(updateCompanyDTO.getLocation());
        }
        if (updateCompanyDTO.getContactName() != null) {
            company.setContactName(updateCompanyDTO.getContactName());
        }
        if (updateCompanyDTO.getContactEmail() != null) {
            company.setContactEmail(updateCompanyDTO.getContactEmail());
        }
        if (updateCompanyDTO.getContactPhone() != null) {
            company.setContactPhone(updateCompanyDTO.getContactPhone());
        }
        if (updateCompanyDTO.getStatus() != null) {
            company.setStatus(updateCompanyDTO.getStatus());
        }
        if (updateCompanyDTO.getNote() != null) {
            company.setNote(updateCompanyDTO.getNote());
        }

        // Sauvegarder les modifications
        companyRepository.save(company);

        log.info("Entreprise {} modifiée avec succès par le commercial {}", 
                company.getName(), commercial.getEmail());
    }

    @Override
    public void updateCompanyStatus(Long id, UpdateCompanyStatusDTO updateCompanyStatusDTO) {

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

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent modifier le statut de leurs entreprises");
        }

        // Récupérer l'entreprise par son ID
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        // Vérifier que l'entreprise appartient au commercial connecté
        if (!company.getCommercial().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
        }

        // Sauvegarder l'ancien statut pour le log
        Boolean oldStatus = company.getIsActive();

        // Mettre à jour le statut
        company.setIsActive(updateCompanyStatusDTO.getIsActive());

        // Sauvegarder les modifications
        companyRepository.save(company);

        // Log avec l'ancien et le nouveau statut
        //statusChange = activée si true, désactivée si false 
        String statusChange = updateCompanyStatusDTO.getIsActive() ? "activée" : "désactivée";
        log.info("Entreprise {} {} par le commercial {} (ancien statut: {}, nouveau statut: {})", 
                company.getName(), statusChange, commercial.getEmail(), oldStatus, updateCompanyStatusDTO.getIsActive());
    }

    /**
     * Convertit le booléen isActive en statut textuel pour l'affichage
     *
     * @param isActive L'état actif/inactif de l'entreprise
     * @return "Actif" si true, "Inactif" si false
     */
    private String status(Boolean isActive) {
        if (isActive != null && isActive) {
            return "Actif";
        }
        return "Inactif";
    }

    /**
     * Mappe une entité Company vers un CompanyListItemDTO
     *
     * @param company L'entité Company à mapper
     * @return Le DTO correspondant
     */
    private CompanyListItemDTO mapToCompanyListItemDTO(Company company) {
        CompanyListItemDTO dto = new CompanyListItemDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setLocation(company.getLocation());
        dto.setContactName(company.getContactName());
        dto.setContactPhone(company.getContactPhone());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setStatus(status(company.getIsActive())); // Convertit isActive en "Actif" ou "Inactif"
        return dto;
    }

    /**
     * Mappe une entité Company vers un CompanyDetailsDTO
     *
     * @param company L'entité Company à mapper
     * @return Le DTO de détails correspondant
     */
    private CompanyDetailsDTO mapToCompanyDetailsDTO(Company company) {
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setLocation(company.getLocation());
        dto.setContactName(company.getContactName());
        dto.setContactPhone(company.getContactPhone());
        dto.setContactEmail(company.getContactEmail());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setStatus(status(company.getIsActive())); // Convertit isActive en "Actif" ou "Inactif"
        dto.setCompanyCode(company.getCompanyCode());
        dto.setSector(company.getSector());
        dto.setNote(company.getNote());
        return dto;
    }

    // ============================================================================
    // 👤 GESTION DES EMPLOYÉS
    // ============================================================================

    @Override
    public void createEmployee(CreateEmployeeDTO employee) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String username = authentication.getName();

        Users commercial = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Commercial introuvable"));

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent créer des employés");
        }

        // Vérifier que l'email n'existe pas déjà
        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Récupérer l'entreprise associée
        Company company = companyRepository.findById(employee.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));


        // Créer l'utilisateur (employé)
        Users user = new Users();
        user.setEmail(employee.getEmail());
        user.setFirstName(employee.getFirstName());
        user.setLastName(employee.getLastName());
        user.setPhone(employee.getPhone());
        user.setRole(UserRole.EMPLOYEE);
        user.setIsActive(false); // Inactif jusqu'à activation

        Users userSaved = userRepository.save(user);

        // Créer l'employé
        Employee employeeEntity = new Employee();
        employeeEntity.setCompany(company);
        employeeEntity.setAddress(employee.getAddress());
        employeeEntity.setUser(userSaved);
        employeeEntity.setCreatedBy(commercial);

        employeeRepository.save(employeeEntity);

        // Générer un token d'invitation unique (UUID)
        String invitationToken = UUID.randomUUID().toString();

        // Stocker le token d'invitation dans ActivationCode
        activationCodeService.generateAndStoreCodeMobile(employee.getEmail());

        // Envoyer l'email d'invitation
        String commercialFullName = commercial.getFirstName() + " " + commercial.getLastName();
        emailService.sendEmployeeInvitation(
                employee.getEmail(),
                invitationToken,
                employee.getFirstName(),
                commercialFullName,
                company.getName()
        );

        log.info("Employé créé avec succès: {} (email: {}) par le commercial {}",
                employee.getFirstName() + " " + employee.getLastName(), employee.getEmail(), commercial.getEmail());
    }


    // ============================================================================
    // 🔧 MÉTHODES UTILITAIRES
    // ============================================================================

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

