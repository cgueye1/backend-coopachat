package com.example.coopachat.services.commercial;

import com.example.coopachat.dtos.companies.*;
import com.example.coopachat.dtos.employees.*;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
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
        company.setIsActive(false);
        company.setCommercial(commercial);

        // Sauvegarder l'entreprise en base
        companyRepository.save(company);

        log.info("Entreprise créée avec succès: {} (code: {}) par le commercial {}",
                company.getName(), companyCode, commercial.getEmail());
    }

    @Override
    public CompanyListResponseDTO getAllCompanies(int page, int size, String search, CompanySector sector, Boolean isActive) {

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

        // Normaliser le terme de recherche (supprimer les espaces)
        //Si search != null et non vide, alors on récupère le terme de recherche sinon on met null
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer la page d'entreprises selon les filtres fournis
        Page<Company> companyPage;

        // Cas 1 : Recherche + Secteur + isActive
        if (searchTerm != null && sector != null && isActive != null) {
            companyPage = companyRepository.findByCommercialAndNameContainingIgnoreCaseAndSectorAndIsActive(
                    commercial, searchTerm, sector, isActive, pageable);
        }
        // Cas 2 : Recherche + Secteur (pas de isActive)
        else if (searchTerm != null && sector != null) {
            companyPage = companyRepository.findByCommercialAndNameContainingIgnoreCaseAndSector(
                    commercial, searchTerm, sector, pageable);
        }
        // Cas 3 : Recherche + isActive (pas de secteur)
        else if (searchTerm != null && isActive != null) {
            companyPage = companyRepository.findByCommercialAndNameContainingIgnoreCaseAndIsActive(
                    commercial, searchTerm, isActive, pageable);
        }
        // Cas 4 : Recherche seulement (pas de secteur, pas de isActive)
        else if (searchTerm != null) {
            companyPage = companyRepository.findByCommercialAndNameContainingIgnoreCase(
                    commercial, searchTerm, pageable);
        }
        // Cas 5 : Secteur + isActive (pas de recherche)
        else if (sector != null && isActive != null) {
            companyPage = companyRepository.findByCommercialAndSectorAndIsActive(
                    commercial, sector, isActive, pageable);
        }
        // Cas 6 : Secteur seulement (pas de recherche, pas de isActive)
        else if (sector != null) {
            companyPage = companyRepository.findByCommercialAndSector(commercial, sector, pageable);
        }
        // Cas 7 : isActive seulement (pas de recherche, pas de secteur)
        else if (isActive != null) {
            companyPage = companyRepository.findByCommercialAndIsActive(commercial, isActive, pageable);
        }
        // Cas 8 : Aucun filtre (toutes les entreprises)
        else {
            companyPage = companyRepository.findByCommercial(commercial, pageable);
        }

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

        log.info("Page {} de {} entreprises récupérée pour le commercial {} (total: {} entreprises, recherche: '{}', secteur: {}, isActive: {})", 
                page + 1, companyPage.getTotalPages(), commercial.getEmail(), companyPage.getTotalElements(), 
                searchTerm != null ? searchTerm : "aucune", sector != null ? sector : "tous", isActive != null ? isActive : "tous");

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

    @Override
    public CompanyStatsDTO getCompanyStats() {

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
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques de leurs entreprises");
        }

        // Calculer les statistiques
        long totalCompanies = companyRepository.countByCommercial(commercial);
        long activeCompanies = companyRepository.countByCommercialAndIsActive(commercial, true);
        long inactiveCompanies = companyRepository.countByCommercialAndIsActive(commercial, false);

        // Créer et remplir le DTO
        CompanyStatsDTO stats = new CompanyStatsDTO();
        stats.setTotalCompanies(totalCompanies);
        stats.setActiveCompanies(activeCompanies);
        stats.setInactiveCompanies(inactiveCompanies);

        log.info("Statistiques des entreprises récupérées pour le commercial {}: Total={}, Actives={}, Inactives={}",
                commercial.getEmail(), totalCompanies, activeCompanies, inactiveCompanies);

        return stats;
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

        // Générer le code unique de l'employé
        String employeeCode = generateUniqueEmployeeCode();

        // Créer l'employé
        Employee employeeEntity = new Employee();
        employeeEntity.setCompany(company);
        employeeEntity.setAddress(employee.getAddress());
        employeeEntity.setUser(userSaved);
        employeeEntity.setCreatedBy(commercial);
        employeeEntity.setEmployeeCode(employeeCode);

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

        log.info("Employé créé avec succès: {} (email: {}, code: {}) par le commercial {}",
                employee.getFirstName() + " " + employee.getLastName(), employee.getEmail(), employeeCode, commercial.getEmail());
    }

    @Override
    public EmployeeListResponseDTO getAllEmployees(int page, int size, String search, Long companyId, Boolean isActive) {

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
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs employés");
        }

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Normaliser le terme de recherche (supprimer les espaces)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer l'entreprise si companyId est fourni
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
            // Vérifier que l'entreprise appartient au commercial
            if (!company.getCommercial().getId().equals(commercial.getId())) {
                throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
            }
        }

        // Récupérer la page d'employés selon les filtres fournis
        Page<Employee> employeePage;

        // Cas 1 : Recherche + Entreprise + isActive
        if (searchTerm != null && company != null && isActive != null) {
            employeePage = employeeRepository.findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompanyAndUserIsActive(
                    commercial, searchTerm, searchTerm, company, isActive, pageable);
        }
        // Cas 2 : Recherche + Entreprise (pas de isActive)
        else if (searchTerm != null && company != null) {
            employeePage = employeeRepository.findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompany(
                    commercial, searchTerm, searchTerm, company, pageable);
        }
        // Cas 3 : Recherche + isActive (pas d'entreprise)
        else if (searchTerm != null && isActive != null) {
            employeePage = employeeRepository.findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndUserIsActive(
                    commercial, searchTerm, searchTerm, isActive, pageable);
        }
        // Cas 4 : Recherche seulement (pas d'entreprise, pas de isActive)
        else if (searchTerm != null) {
            employeePage = employeeRepository.findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCase(
                    commercial, searchTerm, searchTerm, pageable);
        }
        // Cas 5 : Entreprise + isActive (pas de recherche)
        else if (company != null && isActive != null) {
            employeePage = employeeRepository.findByCreatedByAndCompanyAndUserIsActive(
                    commercial, company, isActive, pageable);
        }
        // Cas 6 : Entreprise seulement (pas de recherche, pas de isActive)
        else if (company != null) {
            employeePage = employeeRepository.findByCreatedByAndCompany(commercial, company, pageable);
        }
        // Cas 7 : isActive seulement (pas de recherche, pas d'entreprise)
        else if (isActive != null) {
            employeePage = employeeRepository.findByCreatedByAndUserIsActive(commercial, isActive, pageable);
        }
        // Cas 8 : Aucun filtre (tous les employés)
        else {
            employeePage = employeeRepository.findByCreatedBy(commercial, pageable);
        }

        // Mapper les entités Employee vers EmployeeListItemDTO
        List<EmployeeListItemDTO> employeeList = employeePage.getContent().stream()
                .map(this::mapToEmployeeListItemDTO)
                .collect(Collectors.toList());

        // Créer la réponse avec pagination
        EmployeeListResponseDTO response = new EmployeeListResponseDTO();
        response.setContent(employeeList);
        response.setTotalElements(employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setCurrentPage(employeePage.getNumber());
        response.setPageSize(employeePage.getSize());
        response.setHasNext(employeePage.hasNext());
        response.setHasPrevious(employeePage.hasPrevious());

        log.info("Page {} de {} employés récupérée pour le commercial {} (total: {} employés, recherche: '{}', entreprise: {}, isActive: {})",
                page + 1, employeePage.getTotalPages(), commercial.getEmail(), employeePage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune", companyId != null ? company.getName() : "toutes", isActive != null ? isActive : "tous");

        return response;
    }

    @Override
    public EmployeeStatsDTO getEmployeeStats() {

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
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques de leurs employés");
        }

        // Calculer les statistiques
        long totalEmployees = employeeRepository.countByCreatedBy(commercial);
        long activeEmployees = employeeRepository.countByCreatedByAndUserIsActive(commercial, true);
        long pendingEmployees = employeeRepository.countByCreatedByAndUserIsActive(commercial, false);

        // Créer et remplir le DTO
        EmployeeStatsDTO stats = new EmployeeStatsDTO();
        stats.setTotalEmployees(totalEmployees);
        stats.setActiveEmployees(activeEmployees);
        stats.setPendingEmployees(pendingEmployees);

        log.info("Statistiques des employés récupérées pour le commercial {}: Total={}, Actifs={}, En attente={}",
                commercial.getEmail(), totalEmployees, activeEmployees, pendingEmployees);

        return stats;
    }

    @Override
    public EmployeeDetailsDTO getEmployeeById(Long id) {

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
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs employés");
        }

        // Récupérer l'employé par son ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        // Vérifier que l'employé appartient au commercial connecté
        if (!employee.getCreatedBy().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cet employé");
        }

        // Mapper l'entité Employee vers EmployeeDetailsDTO
        EmployeeDetailsDTO employeeDetails = mapToEmployeeDetailsDTO(employee);

        log.info("Détails de l'employé {} récupérés par le commercial {}",
                employee.getUser().getFirstName() + " " + employee.getUser().getLastName(), commercial.getEmail());

        return employeeDetails;
    }

    @Override
    public void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {

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
            throw new RuntimeException("Seuls les commerciaux peuvent modifier leurs employés");
        }

        // Récupérer l'employé par son ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        // Vérifier que l'employé appartient au commercial connecté
        if (!employee.getCreatedBy().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cet employé");
        }
        //Récupérer le compte lié à cet employé
        Users user = employee.getUser();

        // Vérifier et mettre à jour l'email si fourni et différent
        if (updateEmployeeDTO.getEmail() != null && !updateEmployeeDTO.getEmail().equals(user.getEmail())) {
            // Vérifier que le nouvel email n'existe pas déjà
            if (userRepository.existsByEmail(updateEmployeeDTO.getEmail())) {
                throw new EmailAlreadyExistsException ("Cet email est déjà utilisé");
            }
            user.setEmail(updateEmployeeDTO.getEmail());
        }

        // Vérifier et mettre à jour le téléphone si fourni et différent
        if (updateEmployeeDTO.getPhone() != null && !updateEmployeeDTO.getPhone().equals(user.getPhone())) {
            // Vérifier que le nouveau téléphone n'existe pas déjà
            if (userRepository.existsByPhone(updateEmployeeDTO.getPhone())) {
                throw new PhoneAlreadyExistsException("Ce numéro de téléphone est déjà utilisé");
            }
            user.setPhone(updateEmployeeDTO.getPhone());
        }

        // Mettre à jour les autres champs (seulement si non null)
        if (updateEmployeeDTO.getFirstName() != null) {
            user.setFirstName(updateEmployeeDTO.getFirstName());
        }
        if (updateEmployeeDTO.getLastName() != null) {
            user.setLastName(updateEmployeeDTO.getLastName());
        }
        if (updateEmployeeDTO.getAddress() != null) {
            employee.setAddress(updateEmployeeDTO.getAddress());
        }

        // Mettre à jour l'entreprise si companyId est fourni
        if (updateEmployeeDTO.getCompanyId() != null) {
            Company company = companyRepository.findById(updateEmployeeDTO.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
            // Vérifier que l'entreprise appartient au commercial
            if (!company.getCommercial().getId().equals(commercial.getId())) {
                throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
            }
            employee.setCompany(company);
        }

        // Sauvegarder les modifications
        userRepository.save(user);
        employeeRepository.save(employee);

        log.info("Employé {} modifié avec succès par le commercial {}",
                user.getFirstName() + " " + user.getLastName(), commercial.getEmail());
    }

    @Override
    public void updateEmployeeStatus(Long id, UpdateEmployeeStatusDTO updateEmployeeStatusDTO) {

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
            throw new RuntimeException("Seuls les commerciaux peuvent modifier le statut de leurs employés");
        }

        // Récupérer l'employé par son ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        // Vérifier que l'employé appartient au commercial connecté
        if (!employee.getCreatedBy().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cet employé");
        }

        // Sauvegarder l'ancien statut pour le log
        Boolean oldStatus = employee.getUser().getIsActive();

        // Mettre à jour le statut
        employee.getUser().setIsActive(updateEmployeeStatusDTO.getIsActive());

        // Sauvegarder les modifications
        userRepository.save(employee.getUser());

        // Log avec l'ancien et le nouveau statut
        //si le statut est actif, on affiche "activé", sinon on affiche "désactivé"
        String statusChange = updateEmployeeStatusDTO.getIsActive() ? "activé" : "désactivé";
        log.info("Employé {} {} par le commercial {} (ancien statut: {}, nouveau statut: {})",
                employee.getUser().getFirstName() + " " + employee.getUser().getLastName(),
                statusChange, commercial.getEmail(), oldStatus, updateEmployeeStatusDTO.getIsActive());
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

    /**
     * Génère un code unique pour l'employé
     * Format: SAL-YYYY-XX (ex: SAL-2025-05)
     *
     * @return Le code unique généré
     */
    private String generateUniqueEmployeeCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String baseCode = "SAL-" + year + "-";
        String employeeCode;
        int counter = 1;

        // Tant que le code existe déjà, incrémenter le compteur et générer un nouveau code
        do {
            // Formater le numéro séquentiel sur 2 chiffres (01, 02, 03...)
            employeeCode = baseCode + String.format("%02d", counter);
            counter++;
        } while (employeeRepository.existsByEmployeeCode(employeeCode));

        return employeeCode;
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

    /**
     * Mappe une entité Employee vers un EmployeeListItemDTO
     *
     * @param employee L'entité Employee à mapper
     * @return Le DTO correspondant
     */
    private EmployeeListItemDTO mapToEmployeeListItemDTO(Employee employee) {
        EmployeeListItemDTO dto = new EmployeeListItemDTO();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getUser().getFirstName());
        dto.setLastName(employee.getUser().getLastName());
        dto.setEmail(employee.getUser().getEmail());
        dto.setCompanyName(employee.getCompany().getName());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setStatus(status(employee.getUser().getIsActive())); // Convertit isActive en "Actif" ou "Inactif"
        dto.setEmployeeCode(employee.getEmployeeCode());
        return dto;
    }

    /**
     * Mappe une entité Employee vers un EmployeeDetailsDTO
     *
     * @param employee L'entité Employee à mapper
     * @return Le DTO de détails correspondant
     */
    private EmployeeDetailsDTO mapToEmployeeDetailsDTO(Employee employee) {
        EmployeeDetailsDTO dto = new EmployeeDetailsDTO();
        dto.setId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setFirstName(employee.getUser().getFirstName());
        dto.setLastName(employee.getUser().getLastName());
        dto.setEmail(employee.getUser().getEmail());
        dto.setPhone(employee.getUser().getPhone());
        dto.setAddress(employee.getAddress());
        dto.setCompanyName(employee.getCompany().getName());
        dto.setCompanyId(employee.getCompany().getId());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setStatus(status(employee.getUser().getIsActive())); // Convertit isActive en "Actif" ou "Inactif"
        return dto;
    }
}

