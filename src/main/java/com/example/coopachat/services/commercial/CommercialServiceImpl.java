package com.example.coopachat.services.commercial;

import com.example.coopachat.dtos.companies.*;
import com.example.coopachat.dtos.coupons.CartTotalCouponStatsDTO;
import com.example.coopachat.dtos.coupons.CouponDetailsDTO;
import com.example.coopachat.dtos.coupons.CouponListItemDTO;
import com.example.coopachat.dtos.coupons.CouponListResponseDTO;
import com.example.coopachat.dtos.coupons.CouponProductItemDTO;
import com.example.coopachat.dtos.coupons.CreateCouponDTO;
import com.example.coopachat.dtos.coupons.UpdateCouponStatusDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.commercial.CommandesParMoisDTO;
import com.example.coopachat.dtos.dashboard.commercial.CommercialDashboardKpisDTO;
import com.example.coopachat.dtos.dashboard.commercial.VentesParMoisDTO;
import com.example.coopachat.dtos.employees.*;
import com.example.coopachat.entities.Address;
import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Coupon;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Product;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.DeliveryMode;
import com.example.coopachat.enums.*;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final MinioService minioService;

    // ============================================================================
    // 🏢 GESTION DES ENTREPRISES
    // ============================================================================

    @Override
    @Transactional
    public void createCompany(CreateCompanyDTO createCompanyDTO) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent créer des entreprises");
        }

        // Vérifier que l'email du contact n'existe pas déjà
        if (createCompanyDTO.getContactEmail() != null && !createCompanyDTO.getContactEmail().trim().isEmpty()
                && companyRepository.existsByContactEmailIgnoreCase(createCompanyDTO.getContactEmail().trim())) {
            throw new EmailAlreadyExistsException("Cet email de contact existe déjà. Utilisez un autre email.");
        }

        // Vérifier que le téléphone du contact n'existe pas déjà
        if (createCompanyDTO.getContactPhone() != null && !createCompanyDTO.getContactPhone().trim().isEmpty()
                && companyRepository.existsByContactPhone(createCompanyDTO.getContactPhone().trim())) {
            throw new PhoneAlreadyExistsException("Ce numéro de téléphone existe déjà. Utilisez un autre numéro.");
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
        if (createCompanyDTO.getLogo() != null && !createCompanyDTO.getLogo().isBlank()) {
            company.setLogo(createCompanyDTO.getLogo());
        }

        // Sauvegarder l'entreprise en base
        companyRepository.save(company);

        log.info("Entreprise créée avec succès: {} (code: {}) par le commercial {}",
                company.getName(), companyCode, commercial.getEmail());
    }

    @Override
    @Transactional
    public CompanyListResponseDTO getAllCompanies(int page, int size, String search, CompanySector sector, Boolean isActive,
                                                  Boolean partnerOnly, Boolean prospectOnly, CompanyStatus prospectionStatus) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs entreprises");
        }

        // Créer l'objet Pageable pour la pagination (les plus récents en premier)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // Normaliser le terme de recherche (supprimer les espaces)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Filtre par type : partenaires uniquement ou prospects uniquement (avec option statut prospection)
        String companyType = null;
        if (Boolean.TRUE.equals(partnerOnly)) {
            companyType = "partenaires";
        } else if (Boolean.TRUE.equals(prospectOnly)) {
            companyType = "prospects";
        }

        Page<Company> companyPage;
        if (companyType != null) {
            companyPage = companyRepository.findByCommercialAndOptionalFilters(
                    commercial, companyType, prospectionStatus, searchTerm, sector, isActive, pageable);
        } else {
        // Récupérer la page d'entreprises selon les filtres fournis (sans filtre partenaire/prospect)
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

        log.info("Page {} de {} entreprises récupérée pour le commercial {} (total: {} entreprises, recherche: '{}', secteur: {}, isActive: {}, type: {})", 
                page + 1, companyPage.getTotalPages(), commercial.getEmail(), companyPage.getTotalElements(), 
                searchTerm != null ? searchTerm : "aucune", sector != null ? sector : "tous", isActive != null ? isActive : "tous", companyType != null ? companyType : "tous");

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyListResponseDTO getCompaniesOnly(int page, int size, String search, CompanySector sector, Boolean isActive) {
        return getAllCompanies(page, size, search, sector, isActive, true, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyListResponseDTO getProspectsOnly(int page, int size, String search, CompanySector sector, CompanyStatus prospectionStatus) {
        return getAllCompanies(page, size, search, sector, null, false, true, prospectionStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyListItemDTO> getLastProspects(int limit) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs prospects");
        }
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        List<Company> prospects = companyRepository.findByCommercialAndStatusNotOrderByIdDesc(commercial, pageable);
        return prospects.stream().map(this::mapToCompanyListItemDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanyDetailsDTO getCompanyById(Long id) {

        Users commercial = getCurrentUser();

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
    @Transactional
    public void updateCompany(Long id, UpdateCompanyDTO updateCompanyDTO) {

        Users commercial = getCurrentUser();

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
            String newEmail = updateCompanyDTO.getContactEmail().trim();
            if (!newEmail.isEmpty()
                // Vérifier que l'email du contact n'existe pas déjà pour une autre entreprise (exclut l'id donné)
                    && companyRepository.existsByContactEmailIgnoreCaseAndIdNot(newEmail, id)) {
                throw new EmailAlreadyExistsException("Cet email de contact existe déjà. Utilisez un autre email.");
            }
            company.setContactEmail(updateCompanyDTO.getContactEmail());
        }
        if (updateCompanyDTO.getContactPhone() != null) {
            String newPhone = updateCompanyDTO.getContactPhone().trim();
            // Vérifier que le téléphone du contact n'existe pas déjà pour une autre entreprise (exclut l'id donné)
            if (!newPhone.isEmpty() && companyRepository.existsByContactPhoneAndIdNot(newPhone, id)) {
                throw new PhoneAlreadyExistsException("Ce numéro de téléphone existe déjà. Utilisez un autre numéro.");
            }
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
    @Transactional
    public void updateCompanyStatus(Long id, UpdateCompanyStatusDTO updateCompanyStatusDTO) {

        Users commercial = getCurrentUser();

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

        // Règle 4 : impossible d'activer une entreprise si status != PARTNER_SIGNED
        if (Boolean.TRUE.equals(updateCompanyStatusDTO.getIsActive()) && company.getStatus() != CompanyStatus.PARTNER_SIGNED) {
            throw new RuntimeException("Seules les entreprises avec le statut « Partenaire signé » peuvent être activées.");
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
    @Transactional
    public void uploadCompanyLogo(Long id, MultipartFile file) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent modifier le logo d'une entreprise");
        }
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        if (!company.getCommercial().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fichier requis");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
                throw new RuntimeException("Format d'image non supporté. Formats acceptés: JPG, PNG");
            }
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("La taille du fichier ne doit pas dépasser 5 Mo");
        }
        try {
            String logoFileName = minioService.uploadFile(file, "companies");
            company.setLogo(logoFileName);
            companyRepository.save(company);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du logo", e);
        }
    }

    @Override
    @Transactional
    public void deleteCompanyLogo(Long id) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent modifier le logo d'une entreprise");
        }
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        if (!company.getCommercial().getId().equals(commercial.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette entreprise");
        }
        company.setLogo(null);
        companyRepository.save(company);
    }

    @Override
    @Transactional
    public CompanyStatsDTO getCompanyStats() {

        Users commercial = getCurrentUser();

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

    @Override
    @Transactional(readOnly = true)
    public ProspectStatsDTO getProspectStats() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques de prospection");
        }
        ProspectStatsDTO dto = new ProspectStatsDTO();
        // Comptages globaux (tous commerciaux confondus) pour ne pas restreindre la vue
        dto.setTotal(companyRepository.countByStatusNot(CompanyStatus.PARTNER_SIGNED));
        dto.setEnAttente(companyRepository.countByStatus(CompanyStatus.PENDING));
        dto.setInteresses(companyRepository.countByStatus(CompanyStatus.INTERESTED));
        dto.setARelancer(companyRepository.countByStatus(CompanyStatus.RELAUNCHED));
        dto.setRdvPlanifie(companyRepository.countByStatus(CompanyStatus.MEETING_SCHEDULED));
        dto.setSignes(companyRepository.countByStatus(CompanyStatus.PARTNER_SIGNED));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyStatsDTO getPartnerStats() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques des partenaires");
        }
        CompanyStatsDTO dto = new CompanyStatsDTO();
        long total = companyRepository.countByCommercialAndStatus(commercial, CompanyStatus.PARTNER_SIGNED);
        long active = companyRepository.countByCommercialAndStatusAndIsActive(commercial, CompanyStatus.PARTNER_SIGNED, true);
        long inactive = companyRepository.countByCommercialAndStatusAndIsActive(commercial, CompanyStatus.PARTNER_SIGNED, false);
        dto.setTotalCompanies(total);
        dto.setActiveCompanies(active);
        dto.setInactiveCompanies(inactive);
        return dto;
    }


    // ============================================================================
    // 👤 GESTION DES EMPLOYÉS
    // ============================================================================

    @Override
    @Transactional
    public void createEmployee(CreateEmployeeDTO employee) {

        Users commercial = getCurrentUser();

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

        if (company.getStatus() != CompanyStatus.PARTNER_SIGNED) {
            throw new RuntimeException("L'entreprise doit avoir le statut « Partenaire signé » pour pouvoir inscrire des salariés.");
        }


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
        employeeEntity.setUser(userSaved);
        employeeEntity.setCreatedBy(commercial);
        employeeEntity.setEmployeeCode(employeeCode);

        employeeRepository.save(employeeEntity);

        log.info("Employé créé avec succès: {} (email: {}, code: {}) par le commercial {}",
                employee.getFirstName() + " " + employee.getLastName(), employee.getEmail(), employeeCode, commercial.getEmail());
    }

    @Override
    @Transactional
    public EmployeeListResponseDTO getAllEmployees(int page, int size, String search, Long companyId, Boolean isActive) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs employés");
        }

        // Créer l'objet Pageable pour la pagination (les plus récents en premier)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

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
    @Transactional
    public EmployeeStatsDTO getEmployeeStats() {

        Users commercial = getCurrentUser();

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
    @Transactional
    public EmployeeDetailsDTO getEmployeeById(Long id) {

        Users commercial = getCurrentUser();

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
    @Transactional
    public void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {

        Users commercial = getCurrentUser();

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

        // Mettre à jour l'adresse principale si address est fourni
        if (updateEmployeeDTO.getAddress() != null) {
            Address primary = addressRepository.findByEmployeeAndIsPrimaryTrue(employee);
            if (primary != null) {
                primary.setFormattedAddress(updateEmployeeDTO.getAddress().trim());
                addressRepository.save(primary);
            } else {
                Address newAddress = new Address();
                newAddress.setEmployee(employee);
                newAddress.setFormattedAddress(updateEmployeeDTO.getAddress().trim());
                newAddress.setPrimary(true);
                newAddress.setDeliveryMode(DeliveryMode.HOME);
                addressRepository.save(newAddress);
            }
        }

        // Sauvegarder les modifications
        userRepository.save(user);
        employeeRepository.save(employee);

        log.info("Employé {} modifié avec succès par le commercial {}",
                user.getFirstName() + " " + user.getLastName(), commercial.getEmail());
    }

    @Override
    @Transactional
    public void updateEmployeeStatus(Long id, UpdateEmployeeStatusDTO updateEmployeeStatusDTO) {

        Users commercial = getCurrentUser();

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
    //  🏷️ Coupons
    // ============================================================================

    @Override
    @Transactional
    public CouponDetailsDTO getCouponById(Long id) {
        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les coupons");
        }

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon introuvable"));

        // Récupérer les produits concernés selon le scope (CART_TOTAL : pas de produit ni catégorie liés → liste vide)
        List<Product> products = Collections.emptyList();
        // Récupérer les produits concernés selon le scope du coupon (pour l’affichage détail)
        // CART_TOTAL : code promo sur le total du panier, pas de produit ni catégorie liés → liste vide
        if (coupon.getScope() == CouponScope.ALL_PRODUCTS || coupon.getScope() == CouponScope.PRODUCTS) {
            // Coupon appliqué à des produits : on charge les produits qui ont ce coupon en référence
            products = productRepository.findByCouponId(coupon.getId());
        } else if (coupon.getScope() == CouponScope.CATEGORIES) {
            // Coupon appliqué à des catégories : on charge les catégories liées, puis tous les produits de ces catégories
            List<Category> categories = categoryRepository.findByCouponId(coupon.getId());
            if (categories != null && !categories.isEmpty()) {
                products = productRepository.findByCategoryIn(categories);//findByCategoryIn signifie "trouver les produits qui appartiennent à une des catégories"
            }
        }

        List<CouponProductItemDTO> productItems = products.stream()
                .map(this::mapToCouponProductItemDTO)
                .collect(Collectors.toList());

        return mapToCouponDetailsDTO(coupon, productItems);
    }

    @Override
    @Transactional
    public CouponListResponseDTO getAllCoupons(int page, int size, String search,
                                               CouponStatus status, CouponScope scope, Boolean isActive) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les coupons");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Coupon> couponPage = couponRepository.findAllWithFilters(
                searchTerm, status, scope, isActive, pageable);

        List<CouponListItemDTO> couponList = couponPage.getContent().stream()
                .map(this::mapToCouponListItemDTO)
                .collect(Collectors.toList());

        CouponListResponseDTO response = new CouponListResponseDTO();
        response.setContent(couponList);
        response.setTotalElements(couponPage.getTotalElements());
        response.setTotalPages(couponPage.getTotalPages());
        response.setCurrentPage(couponPage.getNumber());
        response.setPageSize(couponPage.getSize());
        response.setHasNext(couponPage.hasNext());
        response.setHasPrevious(couponPage.hasPrevious());

        log.info("Page {} de {} coupons récupérée par le commercial {} (total: {}, recherche: '{}', status: {}, scope: {}, isActive: {})",
                page + 1, couponPage.getTotalPages(), commercial.getEmail(), couponPage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune",
                status != null ? status : "tous",
                scope != null ? scope : "tous",
                isActive != null ? isActive : "tous");

        return response;
    }

    @Override
    public void addCoupon(CreateCouponDTO createCouponDTO) {

        // 1) Validation d'unicité
        if (couponRepository.existsByCode(createCouponDTO.getCode())) {
            throw new RuntimeException("Un coupon avec ce code existe déjà");
        }
        if (couponRepository.existsByName(createCouponDTO.getName())) {
            throw new RuntimeException("Un coupon avec ce nom existe déjà");
        }

        // 2) Validation des dates
        if (createCouponDTO.getEndDate().isBefore(createCouponDTO.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        // 2b) Scope CART_TOTAL = code promo panier, pas de produit ni catégorie
        if (createCouponDTO.getScope() == CouponScope.CART_TOTAL) {
            if ((createCouponDTO.getProductIds() != null && !createCouponDTO.getProductIds().isEmpty())
                    || (createCouponDTO.getCategoryIds() != null && !createCouponDTO.getCategoryIds().isEmpty())) {
                throw new RuntimeException("Un coupon \"Sur le total du panier\" ne doit pas être lié à des produits ou catégories");
            }
        }

        // 2c) Montant fixe = uniquement sur le total du panier (évite coupon > prix produit sur une ligne(article) de panier)
        if (createCouponDTO.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            if (createCouponDTO.getScope() != CouponScope.CART_TOTAL) {
                throw new RuntimeException("Un coupon en montant fixe ne peut être appliqué que sur le total du panier (scope CART_TOTAL)");
            }
            if ((createCouponDTO.getProductIds() != null && !createCouponDTO.getProductIds().isEmpty())
                    || (createCouponDTO.getCategoryIds() != null && !createCouponDTO.getCategoryIds().isEmpty())) {
                throw new RuntimeException("Un coupon en montant fixe ne doit pas être lié à des produits ou catégories");
            }
        }

        // 3) Création du coupon
        Coupon coupon = new Coupon();
        coupon.setCode(createCouponDTO.getCode().trim().toUpperCase()); // Normalisation du code
        coupon.setName(createCouponDTO.getName().trim()); // Normalisation du nom
        coupon.setDiscountType(createCouponDTO.getDiscountType());//Type de coupon
        coupon.setValue(createCouponDTO.getValue());
        coupon.setScope(createCouponDTO.getScope());
        coupon.setIsActive(false);
        coupon.setStatus(CouponStatus.PLANNED);
        coupon.setStartDate(createCouponDTO.getStartDate());
        coupon.setEndDate(createCouponDTO.getEndDate());

        Coupon saved = couponRepository.save(coupon);

        // 4) Lier des produits/catégories uniquement pour les scopes autre que CART_TOTAL (déjà validé en 2b)
        if (createCouponDTO.getScope() == CouponScope.ALL_PRODUCTS) {
            // Appliquer sur tous les produits actifs
            List<Product> activeProducts = productRepository.findByStatus(true);
            for (Product p : activeProducts) {
                p.setCoupon(saved);
            }
            productRepository.saveAll(activeProducts);
        } else if (createCouponDTO.getScope() == CouponScope.PRODUCTS) {//on a ciblé des produits, on les lie au coupon
            if (createCouponDTO.getProductIds() == null || createCouponDTO.getProductIds().isEmpty()) {
                throw new RuntimeException("Veuillez sélectionner au moins un produit");
            }
            List<Product> products = productRepository.findAllById(createCouponDTO.getProductIds());
            for (Product p : products) {
                if (Boolean.FALSE.equals(p.getStatus())) {
                    throw new RuntimeException("Produit inactif détecté: " + p.getName());
                }
                p.setCoupon(saved);
            }
            productRepository.saveAll(products);
        } else if (createCouponDTO.getScope() == CouponScope.CATEGORIES) {
            if (createCouponDTO.getCategoryIds() == null || createCouponDTO.getCategoryIds().isEmpty()) {
                throw new RuntimeException("Veuillez sélectionner au moins une catégorie");
            }
            List<Category> categories = categoryRepository.findAllById(createCouponDTO.getCategoryIds());
            for (Category c : categories) {
                c.setCoupon(saved);
            }
            categoryRepository.saveAll(categories);
        }

    }


    @Override
    @Transactional
    public void updateCouponStatus(Long id, UpdateCouponStatusDTO updateCouponStatusDTO) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon introuvable"));

        coupon.setIsActive(updateCouponStatusDTO.getIsActive());
        coupon.setStatus(computeStatus(coupon.getStartDate(), coupon.getEndDate(), updateCouponStatusDTO.getIsActive()));

        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon introuvable"));

        // Délier les produits qui référencent ce coupon
        List<Product> productsWithCoupon = productRepository.findByCouponId(coupon.getId());
        for (Product p : productsWithCoupon) {
            p.setCoupon(null);
        }
        productRepository.saveAll(productsWithCoupon);

        // Délier les catégories qui référencent ce coupon
        List<Category> categoriesWithCoupon = categoryRepository.findByCouponId(coupon.getId());
        for (Category c : categoriesWithCoupon) {
            c.setCoupon(null);
        }
        categoryRepository.saveAll(categoriesWithCoupon);

        couponRepository.delete(coupon);
    }

    @Override
    public CartTotalCouponStatsDTO getCartTotalCouponStats() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques coupons");
        }
        long activeCount = couponRepository.countByScopeAndStatus(CouponScope.CART_TOTAL, CouponStatus.ACTIVE);
        long totalUsages = couponRepository.sumUsageCountByScope(CouponScope.CART_TOTAL);
        BigDecimal totalGenerated = couponRepository.sumTotalGeneratedByScope(CouponScope.CART_TOTAL);
        if (totalGenerated == null) {
            totalGenerated = java.math.BigDecimal.ZERO;
        }
        return new CartTotalCouponStatsDTO(activeCount, totalUsages, totalGenerated);
    }

    /**
     * KPIs du tableau de bord commercial : salariés actifs, nouveaux ce mois, commandes et ventes ce mois avec évolution %, promotions actives.
     * Données globales (tous les salariés / toutes les commandes), sans filtre createdBy, pour ne pas restreindre la vue.
     */
    @Override
    public CommercialDashboardKpisDTO getDashboardKpis() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter le tableau de bord");
        }

        LocalDate today = LocalDate.now();//date du jour
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();//date du début du mois
        LocalDateTime monthEnd = today.atTime(23, 59, 59, 999_999_999);//date de la fin du mois
        LocalDate lastMonthDate = today.minusMonths(1);//mois précédent
        LocalDateTime lastMonthStart = lastMonthDate.withDayOfMonth(1).atStartOfDay();//date du début du mois précédent
        LocalDateTime lastMonthEnd = lastMonthDate.atTime(23, 59, 59, 999_999_999);//date de la fin du mois précédent

        long totalSalaries = employeeRepository.countByUserIsActive(true);//nombre de salariés actifs
        long nouveauxSalariesCeMois = employeeRepository.countByCreatedAtBetween(monthStart, monthEnd);//nombre de nouveaux salariés ce mois
        long commandesCeMois = orderRepository.countByCreatedAtBetween(monthStart, monthEnd);//nombre de commandes créées ce mois
        long commandesMoisDernier = orderRepository.countByCreatedAtBetween(lastMonthStart, lastMonthEnd);//nombre de commandes le mois dernier
        Double evolutionCommandesPct = (commandesMoisDernier > 0)
                ? ((commandesCeMois - commandesMoisDernier) * 100.0 / commandesMoisDernier)
                : null;//((ce mois - mois dernier) / mois dernier) * 100

        BigDecimal ventesCeMois = orderRepository.sumTotalPriceByStatusAndDeliveryCompletedAtBetween(
                OrderStatus.LIVREE, monthStart, monthEnd);//sum(totalPrice) des commandes LIVREE livrées ce mois
        if (ventesCeMois == null) {
            ventesCeMois = BigDecimal.ZERO;
        }
        BigDecimal ventesMoisDernier = orderRepository.sumTotalPriceByStatusAndDeliveryCompletedAtBetween(
                OrderStatus.LIVREE, lastMonthStart, lastMonthEnd);//sum(totalPrice) des commandes LIVREE livrées le mois dernier
        if (ventesMoisDernier == null) {
            ventesMoisDernier = BigDecimal.ZERO;
        }
        Double evolutionVentesPct = (ventesMoisDernier.compareTo(BigDecimal.ZERO) > 0)
                ? (ventesCeMois.subtract(ventesMoisDernier).doubleValue() * 100.0 / ventesMoisDernier.doubleValue())
                : null;//((ce mois - mois dernier) / mois dernier) * 100

        long promotionsActives = couponRepository.countByIsActiveTrue();//count(coupons isActive=true)

        // Graphique 1 — Ventes par mois (6 derniers mois)
        LocalDateTime debut = today.minusMonths(5).withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin = monthEnd;
        List<VentesParMoisDTO> evolutionVentes = buildEvolutionVentes(debut, fin);
        // Graphique 2 — Commandes par mois (6 derniers mois)
        List<CommandesParMoisDTO> evolutionCommandes = buildEvolutionCommandes(debut, fin);

        return new CommercialDashboardKpisDTO(
                totalSalaries,
                nouveauxSalariesCeMois,
                commandesCeMois,
                evolutionCommandesPct,
                ventesCeMois,
                evolutionVentesPct,
                promotionsActives,
                evolutionVentes,
                evolutionCommandes
        );
    }

    /** Coupons utilisés par jour (7 derniers jours) pour le graphique « Tendance des coupons utilisés ». */
    @Override
    public List<CouponUsageParJourDTO> getCouponsUtilisesParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<CouponUsageParJourDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.atTime(23, 59, 59, 999_999_999);
            long nbUtilisations = orderRepository.countByCouponIsNotNullAndCreatedAtBetween(dayStart, dayEnd);
            result.add(new CouponUsageParJourDTO(day.format(formatter), nbUtilisations));
        }
        return result;
    }

    private static final String[] MOIS_LABELS = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

    /** Construit la liste des 6 derniers mois avec montant des ventes (LIVREE). */
    /**
     * Construit la liste des 6 derniers mois avec le montant des ventes (commandes LIVREE) pour le graphique « Évolution des ventes ».
     * Appelle orderRepository.sumVentesParMois puis remplit les 6 mois (libellé Jan, Fév, …) ; les mois sans donnée ont un montant à 0.
     */
    private List<VentesParMoisDTO> buildEvolutionVentes(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> raw = orderRepository.sumVentesParMois(OrderStatus.LIVREE, debut, fin);
        Map<String, BigDecimal> byKey = new HashMap<>();
        for (Object[] row : raw) {
            int year = (Integer) row[0];
            int month = (Integer) row[1];
            BigDecimal montant = (BigDecimal) row[2];
            byKey.put(year + "-" + month, montant != null ? montant : BigDecimal.ZERO);
        }
        List<VentesParMoisDTO> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate m = LocalDate.now().minusMonths(5 - i);
            String key = m.getYear() + "-" + m.getMonthValue();
            String moisLabel = MOIS_LABELS[m.getMonthValue() - 1];
            result.add(new VentesParMoisDTO(moisLabel, byKey.getOrDefault(key, BigDecimal.ZERO)));
        }
        return result;
    }

     /**
     * Construit la liste des 6 derniers mois avec le nombre de commandes pour le graphique « Nombre de commandes ».
     * Appelle orderRepository.countCommandesParMois puis remplit les 6 mois (libellé Jan, Fév, …) ; les mois sans donnée ont 0 commande.
     */
    private List<CommandesParMoisDTO> buildEvolutionCommandes(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> raw = orderRepository.countCommandesParMois(debut, fin);
        Map<String, Long> byKey = new HashMap<>();
        for (Object[] row : raw) {
            int year = (Integer) row[0];
            int month = (Integer) row[1];
            long count = (Long) row[2];
            byKey.put(year + "-" + month, count);
        }
        List<CommandesParMoisDTO> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate m = LocalDate.now().minusMonths(5 - i);
            String key = m.getYear() + "-" + m.getMonthValue();
            String moisLabel = MOIS_LABELS[m.getMonthValue() - 1];
            result.add(new CommandesParMoisDTO(moisLabel, byKey.getOrDefault(key, 0L)));
        }
        return result;
    }

    // ============================================================================
    // 🔧 MÉTHODES UTILITAIRES
    // ============================================================================

    /**
     * Récupère l'utilisateur actuellement connecté.
     *
     * @return Users l'utilisateur connecté
     * @throws RuntimeException si aucun utilisateur n'est authentifié
     */
    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable avec email: " + userEmail
                ));
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
        dto.setSector(formatSector(company.getSector()));
        dto.setLocation(company.getLocation());
        dto.setContactName(company.getContactName());
        dto.setContactPhone(company.getContactPhone());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setStatus(company.getStatus() != null ? company.getStatus().getLabel() : null); // Libellé prospection (ex. Partenaire signé)
        dto.setLogo(company.getLogo());
        dto.setIsActive(company.getIsActive());
        return dto;
    }

    private String formatSector(CompanySector sector) {
        if (sector == null) {
            return "Autre";
        }
        return switch (sector) {
            case TECHNOLOGY -> "Technologie";
            case FINANCE -> "Finance";
            case HEALTHCARE -> "Santé";
            case EDUCATION -> "Éducation";
            case RETAIL -> "Commerce de détail";
            case MANUFACTURING -> "Industrie manufacturière";
            case CONSTRUCTION -> "BTP / Construction";
            case TRANSPORTATION -> "Transport";
            case HOSPITALITY -> "Hôtellerie / Restauration";
            case ENERGY -> "Énergie";
            case TELECOMMUNICATIONS -> "Télécommunications";
            case AGRICULTURE -> "Agriculture";
            case FOOD_AND_BEVERAGE -> "Agroalimentaire";
            case PHARMACEUTICAL -> "Pharmaceutique";
            case AUTOMOTIVE -> "Automobile";
            case TEXTILE -> "Textile";
            case CONSULTING -> "Conseil";
            case REAL_ESTATE -> "Immobilier";
            case MEDIA -> "Médias";
            case GOVERNMENT -> "Secteur public";
            case NON_PROFIT -> "Association / ONG";
            case OTHER -> "Autre";
        };
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
        dto.setStatus(company.getStatus() != null ? company.getStatus().getLabel() : null); // Libellé prospection (ex. Partenaire signé)
        dto.setCompanyCode(company.getCompanyCode());
        dto.setSector(company.getSector());
        dto.setNote(company.getNote());
        dto.setLogo(company.getLogo());
        dto.setIsActive(company.getIsActive());
        if (company.getStatus() == CompanyStatus.PARTNER_SIGNED) {
            dto.setEmployeeCount(employeeRepository.countByCompany(company));
            dto.setOrderCount(orderRepository.countByEmployeeCompany(company));
        }
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
        dto.setProfilePhotoUrl(employee.getUser().getProfilePhotoUrl());
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
        Address primaryAddress = addressRepository.findByEmployeeAndIsPrimaryTrue(employee);
        dto.setAddress(primaryAddress != null ? primaryAddress.getFormattedAddress() : null);
        dto.setCompanyName(employee.getCompany().getName());
        dto.setCompanyId(employee.getCompany().getId());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setStatus(status(employee.getUser().getIsActive())); // Convertit isActive en "Actif" ou "Inactif"
        return dto;
    }

    /**
     * Mappe une entité Coupon vers un CouponListItemDTO
     *
     * @param coupon L'entité Coupon à mapper
     * @return Le DTO correspondant
     */
    private CouponListItemDTO mapToCouponListItemDTO(Coupon coupon) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        CouponListItemDTO dto = new CouponListItemDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setValue(coupon.getValue());
        dto.setScope(coupon.getScope());
        dto.setStatus(coupon.getStatus());
        dto.setValidFrom(coupon.getStartDate() != null ? coupon.getStartDate().format(formatter) : null);
        dto.setValidTo(coupon.getEndDate() != null ? coupon.getEndDate().format(formatter) : null);
        dto.setUsageCount(coupon.getUsageCount());
        dto.setTotalGenerated(coupon.getTotalGenerated());
        return dto;
    }

    /**
     * Mappe une entité Product vers un CouponProductItemDTO
     *
     * @param product L'entité Product à mapper
     * @return Le DTO correspondant
     */
    private CouponProductItemDTO mapToCouponProductItemDTO(Product product) {
        CouponProductItemDTO dto = new CouponProductItemDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        dto.setDescription(product.getDescription());
        dto.setCurrentStock(product.getCurrentStock());
        dto.setImage(product.getImage());
        return dto;
    }

    /**
     * Mappe une entité Coupon vers un CouponDetailsDTO
     *
     * @param coupon L'entité Coupon à mapper
     * @param products Liste des produits liés
     * @return Le DTO correspondant
     */
    private CouponDetailsDTO mapToCouponDetailsDTO(Coupon coupon, List<CouponProductItemDTO> products) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        CouponDetailsDTO dto = new CouponDetailsDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setValue(coupon.getValue());
        dto.setScope(coupon.getScope());
        dto.setStatus(coupon.getStatus());
        dto.setIsActive(coupon.getIsActive());
        dto.setValidFrom(coupon.getStartDate() != null ? coupon.getStartDate().format(formatter) : null);
        dto.setValidTo(coupon.getEndDate() != null ? coupon.getEndDate().format(formatter) : null);
        dto.setUsageCount(coupon.getUsageCount());
        dto.setTotalGenerated(coupon.getTotalGenerated());
        dto.setProducts(products);
        return dto;
    }

    /**
     * Calcule automatiquement le statut d'un coupon selon les dates et l'activation manuelle.
     * @param startDate La date de début du coupon
     * @param endDate La date de fin du coupon
     * @param isActive La valeur de l'activation manuelle du coupon
     */
    private CouponStatus computeStatus(LocalDateTime startDate, LocalDateTime endDate, boolean isActive) {
        LocalDateTime now = LocalDateTime.now();//Récupère la date et l'heure actuelle

        if (isActive == false) {
            return CouponStatus.DISABLED;//Si le coupon n'est pas activé, le statut est DISABLED
        }
        if (now.isAfter(endDate)) {
            return CouponStatus.EXPIRED;//Si la date de fin est passée, le statut est EXPIRED
        }
        if (now.isBefore(startDate)) {
            return CouponStatus.PLANNED;//Si la date de début est dans le futur, le statut est PLANNED
        }
        return CouponStatus.ACTIVE;//Sinon, le statut est ACTIVE
    }
}

