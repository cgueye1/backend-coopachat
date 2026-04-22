package com.example.coopachat.services.commercial;

import com.example.coopachat.dtos.companies.*;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;
import com.example.coopachat.dtos.coupons.*;
import com.example.coopachat.dtos.promotions.CreatePromotionDTO;
import com.example.coopachat.dtos.promotions.ProductReductionItemDTO;
import com.example.coopachat.dtos.promotions.PromotionDetailsDTO;
import com.example.coopachat.dtos.promotions.PromotionListResponseDTO;
import com.example.coopachat.dtos.promotions.PromotionListItemDTO;
import com.example.coopachat.dtos.promotions.PromotionProductItemDTO;
import com.example.coopachat.dtos.promotions.PromotionStatsDTO;
import com.example.coopachat.dtos.dashboard.admin.CouponUsageParJourDTO;
import com.example.coopachat.dtos.dashboard.commercial.CommandesParMoisDTO;
import com.example.coopachat.dtos.dashboard.commercial.CommercialDashboardKpisDTO;
import com.example.coopachat.dtos.dashboard.commercial.VentesParMoisDTO;
import com.example.coopachat.dtos.employees.*;
import com.example.coopachat.entities.Address;
import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.CompanySector;
import com.example.coopachat.entities.Coupon;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Product;
import com.example.coopachat.entities.Promotion;
import com.example.coopachat.entities.PromotionProduct;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.enums.DeliveryMode;
import com.example.coopachat.enums.*;
import com.example.coopachat.exceptions.BadRequestBusinessException;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.auth.EmailService;
import com.example.coopachat.services.Employee.EmployeeNotificationService;
import com.example.coopachat.services.user.UserReferenceGenerator;
import com.example.coopachat.services.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.coopachat.util.EmployeeExcelUtility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final CompanySectorRepository companySectorRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final MinioService minioService;
    private final EmailService emailService;
    private final EmployeeNotificationService employeeNotificationService;
    private final UserReferenceGenerator userReferenceGenerator;
    private final com.example.coopachat.services.auth.ActivationCodeService activationCodeService;

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

        String contactEmail = createCompanyDTO.getContactEmail() != null ? createCompanyDTO.getContactEmail().trim() : "";

        // Vérifier que l'email du contact n'existe pas déjà (table Company)
        if (!contactEmail.isEmpty() && companyRepository.existsByContactEmailIgnoreCase(contactEmail)) {
            throw new EmailAlreadyExistsException("Cet email de contact existe déjà dans une autre entreprise.");
        }

        // Vérifier aussi dans la table Users pour éviter les doublons de compte
        if (!contactEmail.isEmpty() && userRepository.existsByEmail(contactEmail)) {
            throw new EmailAlreadyExistsException("Un compte utilisateur existe déjà avec cet email.");
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
        if (createCompanyDTO.getSectorId() != null) {
            CompanySector sector = companySectorRepository.findById(createCompanyDTO.getSectorId())
                    .orElseThrow(() -> new RuntimeException("Secteur d'activité introuvable"));
            company.setSector(sector);
        }
        company.setLocation(createCompanyDTO.getLocation());
        company.setContactName(createCompanyDTO.getContactName());
        company.setContactEmail(contactEmail.isEmpty() ? null : contactEmail);
        company.setContactPhone(createCompanyDTO.getContactPhone());
        company.setStatus(createCompanyDTO.getStatus());
        company.setNote(createCompanyDTO.getNote());
        company.setIsActive(false); // Inactif jusqu'à activation
        company.setCommercial(commercial);
        if (createCompanyDTO.getLogo() != null && !createCompanyDTO.getLogo().isBlank()) {
            company.setLogo(createCompanyDTO.getLogo());
        }

        // Sauvegarder l'entreprise en base
        Company savedCompany = companyRepository.save(company);

        // Si un email est fourni, on crée le compte "Espace Entreprise" pour le contact
        if (!contactEmail.isEmpty()) {
            createOrUpdateCompanyAccount(savedCompany, contactEmail, createCompanyDTO.getContactName(), createCompanyDTO.getContactPhone(), commercial);
        }

        log.info("Entreprise créée avec succès: {} (code: {}) par le commercial {}",
                savedCompany.getName(), companyCode, commercial.getEmail());
    }

    /**
     * Crée ou met à jour le compte utilisateur "Espace Entreprise" associé au contact d'une entreprise.
     */
    private void createOrUpdateCompanyAccount(Company company, String email, String contactName, String phone, Users commercial) {
        if (email == null || email.isBlank()) return;

        // Vérifier si un compte existe déjà pour cet email
        Optional<Users> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isEmpty()) {
            // Créer le compte utilisateur
            Users user = new Users();
            user.setEmail(email);
            
            // Découpage sommaire du nom (Prénom Nom)
            String[] nameParts = contactName.split("\\s+", 2);
            if (nameParts.length > 1) {
                user.setFirstName(nameParts[0]);
                user.setLastName(nameParts[1]);
            } else {
                user.setFirstName(contactName);
                user.setLastName("-"); // Placeholder car le champ est  obligatoire au nniveau de la table users
            }
            
            user.setPhone(phone);
            user.setRole(UserRole.COMPANY);
            user.setIsActive(false);
            user.setRefUser(userReferenceGenerator.generateUniqueRefUser());
            Users savedUser = userRepository.save(user);

            // Créer l'entrée Employee pour le lien (le représentant de l'entreprise est considéré comme le "salarié n°1")
            Employee representative = new Employee();
            representative.setCompany(company);
            representative.setUser(savedUser);
            representative.setCreatedBy(commercial);
            representative.setEmployeeCode(generateUniqueEmployeeCode());
            employeeRepository.save(representative);

            // Envoyer l'email d'activation
            String code = activationCodeService.generateAndStoreCode(savedUser.getEmail());
            emailService.sendCompanyActivationLink(savedUser.getEmail(), code, savedUser.getFirstName(), company.getName());
            
            log.info("Nouveau compte Espace Entreprise créé pour {} (Entreprise: {})", email, company.getName());
        } else {
            // Si l'utilisateur existe déjà
            log.warn("Un compte utilisateur existe déjà pour l'email {}, création ignorée.", email);
        }
    }

    @Override
    @Transactional
    public CompanyListResponseDTO getAllCompanies(int page, int size, String search, Long sectorId, Boolean isActive,
                                                  Boolean partnerOnly, Boolean prospectOnly, CompanyStatus prospectionStatus) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs entreprises");
        }

        // Créer l'objet Pageable : tri par date de modification décroissante (nouveau partenaire en 1ère ligne)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));

        // Normaliser le terme de recherche (supprimer les espaces)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Résoudre le secteur (référentiel)
        CompanySector sector = null;
        if (sectorId != null) {
            sector = companySectorRepository.findById(sectorId).orElse(null);
        }

        // Filtre par type : partenaires uniquement ou prospects uniquement (avec option statut prospection)
        String companyType = null;
        if (Boolean.TRUE.equals(partnerOnly)) {
            companyType = "partenaires";
        } else if (Boolean.TRUE.equals(prospectOnly)) {
            companyType = "prospects";
        }

        Page<Company> companyPage;
        if (Boolean.TRUE.equals(partnerOnly)) {
            // Partenaires : liste globale (tous les commerciaux)
            companyPage = companyRepository.findPartnersByOptionalFilters(searchTerm, sector, isActive, pageable);
        } else if (companyType != null) {
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

        log.info("Page {} de {} entreprises récupérée pour le commercial {} (total: {} entreprises, recherche: '{}', secteurId: {}, isActive: {}, type: {})",
                page + 1, companyPage.getTotalPages(), commercial.getEmail(), companyPage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune", sectorId != null ? sectorId : "tous", isActive != null ? isActive : "tous", companyType != null ? companyType : "tous");

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyListResponseDTO getCompaniesOnly(int page, int size, String search, Long sectorId, Boolean isActive) {
        return getAllCompanies(page, size, search, sectorId, isActive, true, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyListResponseDTO getProspectsOnly(int page, int size, String search, Long sectorId, CompanyStatus prospectionStatus) {
        return getAllCompanies(page, size, search, sectorId, null, false, true, prospectionStatus);
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
    @Transactional(readOnly = true)
    public List<ReferenceItemDTO> getCompanySectors() {
        return companySectorRepository.findAll().stream()
                .map(e -> new ReferenceItemDTO(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
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

        // Mettre à jour les champs de l'entreprise (seulement si non null)
        if (updateCompanyDTO.getName() != null) {
            company.setName(updateCompanyDTO.getName());
        }
        if (updateCompanyDTO.getSectorId() != null) {
            CompanySector sector = companySectorRepository.findById(updateCompanyDTO.getSectorId())
                    .orElseThrow(() -> new RuntimeException("Secteur d'activité introuvable"));
            company.setSector(sector);
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

        // Si l'email a été modifié ou ajouté, on s'assure que le compte utilisateur existe
        if (updateCompanyDTO.getContactEmail() != null && !updateCompanyDTO.getContactEmail().trim().isEmpty()) {
            createOrUpdateCompanyAccount(company, updateCompanyDTO.getContactEmail().trim(), 
                    company.getContactName(), company.getContactPhone(), commercial);
        }

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
        dto.setRelancer(companyRepository.countByStatus(CompanyStatus.RELAUNCHED));
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
        // Comptage global (tous les partenaires, tous commerciaux) pour aligner avec la liste globale
        long total = companyRepository.countByStatus(CompanyStatus.PARTNER_SIGNED);
        long active = companyRepository.countByStatusAndIsActive(CompanyStatus.PARTNER_SIGNED, true);
        long inactive = companyRepository.countByStatusAndIsActive(CompanyStatus.PARTNER_SIGNED, false);
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

        String phone = employee.getPhone() != null ? employee.getPhone().trim() : "";
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé : " + phone);
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
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setRole(UserRole.EMPLOYEE);
        user.setIsActive(false); // Inactif jusqu'à activation
        user.setRefUser(userReferenceGenerator.generateUniqueRefUser());

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

        // Génération du code et envoi du lien d'activation direct
        String code = activationCodeService.generateAndStoreCode(userSaved.getEmail());
        String commercialName = commercial.getFirstName() + " " + commercial.getLastName();
        emailService.sendEmployeeActivationLink(userSaved.getEmail(), code, userSaved.getFirstName(), commercialName, company.getName());

        log.info("Employé créé avec succès: {} (email: {}, code: {}) par le commercial {}. Lien d'activation envoyé.",
                employee.getFirstName() + " " + employee.getLastName(), employee.getEmail(), employeeCode, commercial.getEmail());
    }

    /**
     * Persistance d'une ligne importée (utilisateur + employé + notification), sans revérifier le rôle commercial.
     */
    private void persistEmployeeFromExcelImport(CreateEmployeeDTO employee, Users commercial) {
        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }
        String phone = employee.getPhone() != null ? employee.getPhone().trim() : "";
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé : " + phone);
        }
        Company company = companyRepository.findById(employee.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        if (company.getStatus() != CompanyStatus.PARTNER_SIGNED) {
            throw new RuntimeException("L'entreprise doit avoir le statut « Partenaire signé » pour pouvoir inscrire des salariés.");
        }
        Users user = new Users();
        user.setEmail(employee.getEmail());
        user.setFirstName(employee.getFirstName());
        user.setLastName(employee.getLastName());
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setRole(UserRole.EMPLOYEE);
        user.setIsActive(false);
        user.setRefUser(userReferenceGenerator.generateUniqueRefUser());
        Users userSaved = userRepository.save(user);
        String employeeCode = generateUniqueEmployeeCode();
        Employee employeeEntity = new Employee();
        employeeEntity.setCompany(company);
        employeeEntity.setUser(userSaved);
        employeeEntity.setCreatedBy(commercial);
        employeeEntity.setEmployeeCode(employeeCode);
        employeeRepository.save(employeeEntity);

        // Génération du code et envoi du lien d'activation direct
        String code = activationCodeService.generateAndStoreCode(userSaved.getEmail());
        String commercialName = commercial.getFirstName() + " " + commercial.getLastName();
        emailService.sendEmployeeActivationLink(userSaved.getEmail(), code, userSaved.getFirstName(), commercialName, company.getName());

        log.info("Employé importé avec succès: {} (email: {}, code: {}) par le commercial {}. Lien d'activation envoyé.",
                employee.getFirstName() + " " + employee.getLastName(), employee.getEmail(), employeeCode, commercial.getEmail());
    }

    @Override
    @Transactional
    public void saveEmployeesFromMultipart(MultipartFile file, Long companyId) {

        Users commercial = getCurrentUser();//Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {//Vérifier que l'utilisateur est bien un commercial
            throw new RuntimeException("Seuls les commerciaux peuvent importer des salariés");
        }
        if (file == null || file.isEmpty()) {//Vérifier que le fichier n'est pas vide
            throw new RuntimeException("Fichier requis");
        }
        if (companyId == null) {//Vérifier que l'identifiant de l'entreprise est fourni
            throw new RuntimeException("L'identifiant de l'entreprise est obligatoire");
        }

        Company company = companyRepository.findById(companyId)//Récupérer l'entreprise associée
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        if (company.getStatus() != CompanyStatus.PARTNER_SIGNED) {//Vérifier que l'entreprise a le statut « Partenaire signé »
            throw new RuntimeException("L'entreprise doit avoir le statut « Partenaire signé » pour pouvoir inscrire des salariés.");
        }

        //Vérifier que le fichier est un fichier Excel
        String original = file.getOriginalFilename();//Récupérer le nom du fichier
        //si le nom du fichier est null ou si il ne se termine pas par .xlsx ou .xls, alors on lance une exception
        if (original == null
                || !(original.toLowerCase(Locale.ROOT).endsWith(".xlsx") || original.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new RuntimeException("Format non supporté : envoyez un fichier .xlsx ou .xls.");
        }

        // Comme excelToStuList : lecture Excel → liste de DTO, puis enregistrement
        List<CreateEmployeeDTO> employeesFromExcel;//Liste qui va contenir tous les employés lus depuis le fichier excel
        try (InputStream inputStream = file.getInputStream()) {
            employeesFromExcel = EmployeeExcelUtility.excelToEmployeeDtoList(inputStream, companyId);//Lecture du fichier Excel et conversion en liste de DTO
        } catch (IOException e) {
            throw new RuntimeException("Lecture du fichier impossible : " + e.getMessage(), e);
        }

        if (employeesFromExcel.isEmpty()) {//Vérifier que la liste n'est pas vide
            throw new RuntimeException("Aucune ligne valide dans le fichier (colonnes : Prénom, Nom, Email, Téléphone).");
        }

        for (CreateEmployeeDTO dto : employeesFromExcel) {//Parcours de la liste des employés
            persistEmployeeFromExcelImport(dto, commercial);//Enregistrement de l'employé dans la base de données
        }
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

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        Page<Employee> employeePage = findEmployeesForCompany(company, searchTerm, isActive, pageable);

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
                searchTerm != null ? searchTerm : "aucune", company.getName(), isActive != null ? isActive : "tous");

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportEmployees(String search, Long companyId, Boolean isActive) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent exporter les salariés");
        }
        if (companyId == null) {
            throw new RuntimeException("L'identifiant de l'entreprise est obligatoire");
        }
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "id"));
        Page<Employee> employeePage = findEmployeesForCompany(company, searchTerm, isActive, pageable);
        List<Employee> employees = employeePage.getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Salariés");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Code", "Prénom", "Nom", "Email", "Téléphone", "Entreprise", "Date inscription", "Statut"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowNum++);
                Users u = e.getUser();
                row.createCell(0).setCellValue(e.getEmployeeCode() != null ? e.getEmployeeCode() : "");
                row.createCell(1).setCellValue(u.getFirstName() != null ? u.getFirstName() : "");
                row.createCell(2).setCellValue(u.getLastName() != null ? u.getLastName() : "");
                row.createCell(3).setCellValue(u.getEmail() != null ? u.getEmail() : "");
                row.createCell(4).setCellValue(u.getPhone() != null ? u.getPhone() : "");
                row.createCell(5).setCellValue(e.getCompany().getName() != null ? e.getCompany().getName() : "");
                row.createCell(6).setCellValue(e.getCreatedAt() != null ? e.getCreatedAt().format(dtFormatter) : "");
                row.createCell(7).setCellValue(Boolean.TRUE.equals(u.getIsActive()) ? "Actif" : "Inactif");
            }

            autoSizeColumnsSafe(sheet, headers.length);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    private Page<Employee> findEmployeesForCompany(Company company, String searchTerm, Boolean isActive, Pageable pageable) {
        if (searchTerm != null && isActive != null) {
            return employeeRepository.findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompanyAndUserIsActive(
                    searchTerm, searchTerm, company, isActive, pageable);
        }
        if (searchTerm != null) {
            return employeeRepository.findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompany(
                    searchTerm, searchTerm, company, pageable);
        }
        if (isActive != null) {
            return employeeRepository.findByCompanyAndUserIsActive(company, isActive, pageable);
        }
        return employeeRepository.findByCompany(company, pageable);
    }

    private void autoSizeColumnsSafe(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Throwable e) {
                log.debug("autoSizeColumn({}) ignoré (headless / polices): {}", i, e.getMessage());
                sheet.setColumnWidth(i, 20 * 256);
            }
        }
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
    @Transactional(readOnly = true)
    public EmployeeDetailsDTO getEmployeeById(Long id) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter leurs employés");
        }

        // Récupérer l'employé par son ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

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

        // Sauvegarder l'ancien statut pour le log
        Boolean oldStatus = employee.getUser().getIsActive();

        // Impossible d'activer un salarié qui n'a pas encore défini son mot de passe
        Users user = employee.getUser();
        if (Boolean.TRUE.equals(updateEmployeeStatusDTO.getIsActive())
                && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new BadRequestBusinessException(
                    "Impossible d'activer ce salarié : il n'a pas encore défini son mot de passe. "
                            + "Le salarié doit d'abord terminer son inscription (code d'activation reçu par e-mail, puis création du mot de passe). "
                            + "Ensuite vous pourrez activer son compte depuis cet écran.");
        }

        // Mettre à jour le statut
        user.setIsActive(updateEmployeeStatusDTO.getIsActive());

        // Sauvegarder les modifications
        userRepository.save(user);

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

        // Récupérer les produits concernés selon le scope du coupon (pour l’affichage détail)
        return mapToCouponDetailsDTO(coupon);
    }

    @Override
    @Transactional
    public CouponListResponseDTO getAllCoupons(int page, int size, String search,
                                               CouponStatus status, Boolean isActive) {

        Users commercial = getCurrentUser();

        // Vérifier que l'utilisateur est bien un commercial
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les coupons");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Coupon> couponPage = couponRepository.findAllWithFilters(
                searchTerm, status, isActive, pageable);

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

        log.info("Page {} de {} coupons récupérée par le commercial {} (total: {}, recherche: '{}', status: {}, isActive: {})",
                page + 1, couponPage.getTotalPages(), commercial.getEmail(), couponPage.getTotalElements(),
                searchTerm != null ? searchTerm : "aucune",
                status != null ? status : "tous",
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

        // 3) Création du coupon (code promo panier uniquement)
        Coupon coupon = new Coupon();
        coupon.setCode(createCouponDTO.getCode().trim().toUpperCase());
        coupon.setName(createCouponDTO.getName().trim());
        coupon.setValue(createCouponDTO.getValue());
        LocalDateTime startDate = createCouponDTO.getStartDate();
        LocalDateTime endDate = createCouponDTO.getEndDate();
        coupon.setStartDate(startDate);
        coupon.setEndDate(endDate);
        // Si la date de début est aujourd'hui : activation immédiate (sinon planifié, désactivé jusqu'à activation manuelle).
        if (startDate.toLocalDate().equals(LocalDate.now())) {
            coupon.setIsActive(true);
            coupon.setStatus(computeStatus(startDate, endDate, true));
        } else {
            coupon.setIsActive(false);
            coupon.setStatus(CouponStatus.PLANNED);
        }

        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void addPromotion(CreatePromotionDTO createPromotionDTO) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent créer des promotions");
        }

        if (createPromotionDTO.getEndDate().isBefore(createPromotionDTO.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        List<ProductReductionItemDTO> items = createPromotionDTO.getProductItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Au moins un produit avec réduction est obligatoire");
        }

        Promotion promotion = new Promotion();
        promotion.setName(createPromotionDTO.getName().trim());
        promotion.setStatus(CouponStatus.PLANNED);
        promotion.setIsActive(false);
        promotion.setStartDate(createPromotionDTO.getStartDate());
        promotion.setEndDate(createPromotionDTO.getEndDate());

        Promotion savedPromotion = promotionRepository.save(promotion);

        for (ProductReductionItemDTO item : items) {
            if (item.getDiscountValue() == null || item.getDiscountValue().compareTo(java.math.BigDecimal.ZERO) <= 0
                    || item.getDiscountValue().compareTo(new BigDecimal("100")) > 0)//
            {
                throw new RuntimeException("La réduction doit être entre 1 et 100 % pour le produit id " + item.getProductId());
            }
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable : id " + item.getProductId()));
            if (Boolean.FALSE.equals(product.getStatus())) {
                throw new RuntimeException("Le produit \"" + product.getName() + "\" est inactif");
            }
            PromotionProduct pp = new PromotionProduct();
            pp.setPromotion(savedPromotion);
            pp.setProduct(product);
            pp.setDiscountValue(item.getDiscountValue());
            promotionProductRepository.save(pp);
        }

        log.info("Promotion \"{}\" créée (id={}) avec {} produit(s) par le commercial {}",
                savedPromotion.getName(), savedPromotion.getId(), items.size(), commercial.getEmail());
    }

    @Override
    @Transactional
    public void updateCouponStatus(Long id, UpdateCouponStatusDTO updateCouponStatusDTO) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon introuvable"));

        // Garde-fou métier : on ne peut pas activer un coupon avant sa date de début ou après sa date de fin.
        if (Boolean.TRUE.equals(updateCouponStatusDTO.getIsActive())) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getStartDate())) {
                throw new RuntimeException("Impossible d'activer : la date de début n'est pas encore arrivée");
            }
            if (now.isAfter(coupon.getEndDate())) {
                throw new RuntimeException("Impossible d'activer : le coupon est expiré");
            }
        }

        coupon.setIsActive(updateCouponStatusDTO.getIsActive());
        coupon.setStatus(computeStatus(coupon.getStartDate(), coupon.getEndDate(), updateCouponStatusDTO.getIsActive()));

        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void updatePromotionStatus(Long id, UpdateCouponStatusDTO updateCouponStatusDTO) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion introuvable"));

        // Même règle que les coupons : impossible d'activer hors fenêtre de validité.
        if (Boolean.TRUE.equals(updateCouponStatusDTO.getIsActive())) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate())) {
                throw new RuntimeException("Impossible d'activer : la date de début n'est pas encore arrivée");
            }
            if (now.isAfter(promotion.getEndDate())) {
                throw new RuntimeException("Impossible d'activer : la promotion est expirée");
            }
        }

        promotion.setIsActive(updateCouponStatusDTO.getIsActive());
        promotion.setStatus(computeStatus(promotion.getStartDate(), promotion.getEndDate(), updateCouponStatusDTO.getIsActive()));
        promotionRepository.save(promotion);
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
        long activeCount = couponRepository.countByStatus(CouponStatus.ACTIVE);
        long totalUsages = couponRepository.sumUsageCount();
        BigDecimal totalGenerated = couponRepository.sumTotalGenerated();
        if (totalGenerated == null) {
            totalGenerated = java.math.BigDecimal.ZERO;
        }
        return new CartTotalCouponStatsDTO(activeCount, totalUsages, totalGenerated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.coopachat.dtos.coupons.IdNameDTO> getActiveProductsForCoupon() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les produits pour coupons");
        }
        return productRepository.findByStatusTrueOrderByNameAsc().stream()
                .map(p -> new IdNameDTO(p.getId(), p.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdNameDTO> getCategoriesForCoupon() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les catégories pour coupons");
        }
        // Ne remonter que les catégories ayant au moins un produit actif
        // (évite d'afficher des catégories "vides" sur les écrans promotions/coupons du commercial)
        return categoryRepository.findCategoriesWithAtLeastOneActiveProduct().stream()
                .sorted(java.util.Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new IdNameDTO(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdNameDTO> getProductsForPromotion(Long categoryId) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les produits pour promotions");
        }
        if (categoryId == null) {
            return productRepository.findByStatusTrueOrderByNameAsc().stream()
                    .map(p -> new IdNameDTO(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0, Integer.MAX_VALUE, org.springframework.data.domain.Sort.by("name"));
        return productRepository.findByCategoryAndStatus(category, true, pageable).getContent().stream()
                .map(p -> new IdNameDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionListResponseDTO getAllPromotions(int page, int size, String search, CouponStatus status) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les promotions");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Promotion> promotionPage = promotionRepository.findAllWithFilters(searchTerm, status, pageable);
        List<PromotionListItemDTO> list = promotionPage.getContent().stream()
                .map(this::mapToPromotionListItemDTO)
                .collect(Collectors.toList());
        PromotionListResponseDTO response = new PromotionListResponseDTO();
        response.setContent(list);
        response.setTotalElements(promotionPage.getTotalElements());
        response.setTotalPages(promotionPage.getTotalPages());
        response.setCurrentPage(promotionPage.getNumber());
        response.setPageSize(promotionPage.getSize());
        response.setHasNext(promotionPage.hasNext());
        response.setHasPrevious(promotionPage.hasPrevious());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDetailsDTO getPromotionById(Long id) {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les promotions");
        }
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion introuvable"));
        return mapToPromotionDetailsDTO(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionStatsDTO getPromotionStats() {
        Users commercial = getCurrentUser();
        if (commercial.getRole() != UserRole.COMMERCIAL) {
            throw new RuntimeException("Seuls les commerciaux peuvent consulter les statistiques promotions");
        }
        long total = promotionRepository.count();
        long actives = promotionRepository.countByStatus(CouponStatus.ACTIVE);
        long planifiees = promotionRepository.countByStatus(CouponStatus.PLANNED);
        long expirees = promotionRepository.countByStatus(CouponStatus.EXPIRED);
        long desactivees = promotionRepository.countByStatus(CouponStatus.DISABLED);
        long totalProduits = promotionProductRepository.count();
        return new PromotionStatsDTO(total, actives, planifiees, expirees, desactivees, totalProduits);
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
        // Toutes les commandes du mois (tous statuts : en attente, validée, livrée, etc.), pas seulement les en attente
        long commandesCeMois = orderRepository.countByCreatedAtBetween(monthStart, monthEnd);//nombre de commandes créées ce mois
        long commandesMoisDernier = orderRepository.countByCreatedAtBetween(lastMonthStart, lastMonthEnd);//nombre de commandes le mois dernier
        Double evolutionCommandesPct = (commandesMoisDernier > 0)
                ? ((commandesCeMois - commandesMoisDernier) * 100.0 / commandesMoisDernier)
                : (commandesCeMois > 0 ? 100.0 : 0.0);// si mois dernier=0 : +100% si ce mois>0, 0% sinon

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
                : (ventesCeMois.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);// si mois dernier=0 : +100% si ventes ce mois>0, 0% sinon

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
     * Code salarié unique (commandes {@code CMD-XXXXXXXX}) :
     * format {@code SAL-XXXXXXXX}.
     */
    private String generateUniqueEmployeeCode() {
        final int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String candidate = "SAL-" + suffix;
            if (!employeeRepository.existsByEmployeeCode(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Impossible de générer un code salarié unique");
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
        dto.setSector(company.getSector() != null ? company.getSector().getName() : null);
        dto.setLocation(company.getLocation());
        dto.setContactName(company.getContactName());
        dto.setContactPhone(company.getContactPhone());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setStatus(company.getStatus() != null ? company.getStatus().getLabel() : null); // Libellé prospection (ex. Partenaire signé)
        dto.setLogo(company.getLogo());
        dto.setIsActive(company.getIsActive());
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
        dto.setStatus(company.getStatus() != null ? company.getStatus().getLabel() : null); // Libellé prospection (ex. Partenaire signé)
        dto.setCompanyCode(company.getCompanyCode());
        dto.setSectorId(company.getSector() != null ? company.getSector().getId() : null);
        dto.setSectorLabel(company.getSector() != null ? company.getSector().getName() : null);
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
        dto.setValue(coupon.getValue());
        dto.setStatus(coupon.getStatus());
        dto.setValidFrom(coupon.getStartDate() != null ? coupon.getStartDate().format(formatter) : null);
        dto.setValidTo(coupon.getEndDate() != null ? coupon.getEndDate().format(formatter) : null);
        dto.setUsageCount(coupon.getUsageCount());
        dto.setTotalGenerated(coupon.getTotalGenerated());
        return dto;
    }

    private static final DateTimeFormatter PROMO_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private PromotionListItemDTO mapToPromotionListItemDTO(Promotion p) {
        PromotionListItemDTO dto = new PromotionListItemDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setStatus(p.getStatus());
        dto.setIsActive(p.getIsActive());
        dto.setStartDate(p.getStartDate() != null ? p.getStartDate().format(PROMO_DATE_FORMAT) : null);
        dto.setEndDate(p.getEndDate() != null ? p.getEndDate().format(PROMO_DATE_FORMAT) : null);
        dto.setProductCount((int) promotionProductRepository.countByPromotionId(p.getId()));
        return dto;
    }

    private PromotionDetailsDTO mapToPromotionDetailsDTO(Promotion p) {
        PromotionDetailsDTO dto = new PromotionDetailsDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setStatus(p.getStatus());
        dto.setIsActive(p.getIsActive());
        dto.setStartDate(p.getStartDate() != null ? p.getStartDate().format(PROMO_DATE_FORMAT) : null);
        dto.setEndDate(p.getEndDate() != null ? p.getEndDate().format(PROMO_DATE_FORMAT) : null);
        List<PromotionProduct> items = promotionProductRepository.findByPromotionId(p.getId());
        dto.setProducts(items.stream()
                .map(pp -> new PromotionProductItemDTO(
                        pp.getProduct().getId(),
                        pp.getProduct().getName(),
                        pp.getProduct().getImage(),
                        pp.getDiscountValue()))
                .collect(Collectors.toList()));
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
     * Mappe une entité Coupon vers un CouponDetailsDTO (code promo panier, pas de produits liés).
     */
    private CouponDetailsDTO mapToCouponDetailsDTO(Coupon coupon) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        CouponDetailsDTO dto = new CouponDetailsDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setValue(coupon.getValue());
        dto.setStatus(coupon.getStatus());
        dto.setIsActive(coupon.getIsActive());
        dto.setValidFrom(coupon.getStartDate() != null ? coupon.getStartDate().format(formatter) : null);
        dto.setValidTo(coupon.getEndDate() != null ? coupon.getEndDate().format(formatter) : null);
        dto.setUsageCount(coupon.getUsageCount());
        dto.setTotalGenerated(coupon.getTotalGenerated());
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