package com.example.coopachat.services.company;

import com.example.coopachat.dtos.dashboard.commercial.CommandesParMoisDTO;
import com.example.coopachat.dtos.dashboard.company.CompanyDashboardKpisDTO;
import com.example.coopachat.dtos.employees.*;
import com.example.coopachat.entities.Address;
import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.CompanyStatus;
import com.example.coopachat.enums.DeliveryMode;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.exceptions.BadRequestBusinessException;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import com.example.coopachat.services.user.UserReferenceGenerator;
import com.example.coopachat.util.EmployeeExcelUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final EmailService emailService;
    private final ActivationCodeService activationCodeService;
    private final UserReferenceGenerator userReferenceGenerator;

    private static final String[] MOIS_LABELS = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

    @Override
    @Transactional(readOnly = true)
    public CompanyDashboardKpisDTO getDashboardKpis() {
        Company company = getMyCompany();
        CompanyDashboardKpisDTO dto = new CompanyDashboardKpisDTO();
        
        long totalEmployees = employeeRepository.countByCompany(company);
        long activeEmployees = employeeRepository.countByCompanyAndUserIsActive(company, true);
        long inactiveEmployees = totalEmployees - activeEmployees;
        long ordersThisMonth = orderRepository.countByEmployeeCompanyAndCreatedAtAfter(company, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0));

        dto.setTotalEmployees(totalEmployees);
        dto.setActiveEmployees(activeEmployees);
        dto.setInactiveEmployees(inactiveEmployees);
        dto.setOrdersThisMonth(ordersThisMonth);
        dto.setActiveEmployeesRatio(activeEmployees + "/" + totalEmployees);

        // Évolution des commandes sur 6 mois
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
        dto.setEvolutionCommandes(buildEvolutionCommandes(company, debut, fin));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeListResponseDTO getMyEmployees(int page, int size, String search, Boolean isActive) {
        Company company = getMyCompany();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Employee> employeePage;
        if (searchTerm != null && isActive != null) {
            employeePage = employeeRepository.findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompanyAndUserIsActive(
                    searchTerm, searchTerm, company, isActive, pageable);
        } else if (searchTerm != null) {
            employeePage = employeeRepository.findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompany(
                    searchTerm, searchTerm, company, pageable);
        } else if (isActive != null) {
            employeePage = employeeRepository.findByCompanyAndUserIsActive(company, isActive, pageable);
        } else {
            employeePage = employeeRepository.findByCompany(company, pageable);
        }

        List<EmployeeListItemDTO> list = employeePage.getContent().stream()
                .map(this::mapToEmployeeListItemDTO)
                .collect(Collectors.toList());

        EmployeeListResponseDTO response = new EmployeeListResponseDTO();
        response.setContent(list);
        response.setTotalElements(employeePage.getTotalElements());
        response.setTotalPages(employeePage.getTotalPages());
        response.setCurrentPage(employeePage.getNumber());
        return response;
    }

    @Override
    @Transactional
    public void createEmployee(CreateEmployeeDTO employeeDTO) {
        Users representative = getCurrentUser();
        Company company = getMyCompany();

        if (company.getStatus() != CompanyStatus.PARTNER_SIGNED) {
            throw new RuntimeException("Votre entreprise doit être au statut 'Partenaire signé' pour inscrire des salariés.");
        }

        if (userRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        Users user = new Users();
        user.setEmail(employeeDTO.getEmail());
        user.setFirstName(employeeDTO.getFirstName());
        user.setLastName(employeeDTO.getLastName());
        user.setPhone(employeeDTO.getPhone());
        user.setRole(UserRole.EMPLOYEE);
        user.setIsActive(false);
        user.setRefUser(userReferenceGenerator.generateUniqueRefUser());
        Users userSaved = userRepository.save(user);

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(userSaved);
        employee.setCreatedBy(representative);
        employee.setEmployeeCode(generateUniqueEmployeeCode());
        employeeRepository.save(employee);

        // Envoi du mail d'activation
        String code = activationCodeService.generateAndStoreCode(userSaved.getEmail());
        emailService.sendEmployeeActivationLink(userSaved.getEmail(), code, userSaved.getFirstName(), 
                representative.getFirstName() + " " + representative.getLastName(), company.getName());
        
        log.info("Salarié créé par le responsable de {}: {}", company.getName(), user.getEmail());
    }

    @Override
    @Transactional
    public void importEmployees(MultipartFile file) {
        Company company = getMyCompany();
        Users representative = getCurrentUser();

        if (file == null || file.isEmpty()) throw new RuntimeException("Fichier requis");

        String original = file.getOriginalFilename();
        if (original == null || !(original.toLowerCase(Locale.ROOT).endsWith(".xlsx") || original.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new RuntimeException("Format non supporté (Excel attendu)");
        }

        try (InputStream is = file.getInputStream()) {
            List<CreateEmployeeDTO> dtos = EmployeeExcelUtility.excelToEmployeeDtoList(is, company.getId());
            for (CreateEmployeeDTO dto : dtos) {
                createEmployee(dto);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier Excel", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportEmployees(String search, Boolean isActive) {
        EmployeeListResponseDTO data = getMyEmployees(0, Integer.MAX_VALUE, search, isActive);
        List<EmployeeListItemDTO> employees = data.getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mes Salariés");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Prénom", "Nom", "Email", "Code", "Statut"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (EmployeeListItemDTO emp : employees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getFirstName());
                row.createCell(1).setCellValue(emp.getLastName());
                row.createCell(2).setCellValue(emp.getEmail());
                row.createCell(3).setCellValue(emp.getEmployeeCode());
                row.createCell(4).setCellValue(emp.getStatus());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new ByteArrayResource(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur export Excel", e);
        }
    }

    @Override
    @Transactional
    public void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
        Company myCompany = getMyCompany();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salarié introuvable"));
        
        if (!employee.getCompany().getId().equals(myCompany.getId())) {
            throw new RuntimeException("Accès refusé : ce salarié n'appartient pas à votre entreprise.");
        }

        Users user = employee.getUser();

        if (updateEmployeeDTO.getEmail() != null && !updateEmployeeDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateEmployeeDTO.getEmail())) {
                throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
            }
            user.setEmail(updateEmployeeDTO.getEmail());
        }

        if (updateEmployeeDTO.getPhone() != null && !updateEmployeeDTO.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(updateEmployeeDTO.getPhone())) {
                throw new PhoneAlreadyExistsException("Ce numéro de téléphone est déjà utilisé");
            }
            user.setPhone(updateEmployeeDTO.getPhone());
        }

        if (updateEmployeeDTO.getFirstName() != null) user.setFirstName(updateEmployeeDTO.getFirstName());
        if (updateEmployeeDTO.getLastName() != null) user.setLastName(updateEmployeeDTO.getLastName());

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

        userRepository.save(user);
        employeeRepository.save(employee);
        log.info("Salarié {} mis à jour par le responsable de {}", user.getEmail(), myCompany.getName());
    }

    @Override
    @Transactional
    public void updateEmployeeStatus(Long id, UpdateEmployeeStatusDTO updateEmployeeStatusDTO) {
        Company myCompany = getMyCompany();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salarié introuvable"));
        
        if (!employee.getCompany().getId().equals(myCompany.getId())) {
            throw new RuntimeException("Accès refusé.");
        }

        Users user = employee.getUser();
        if (Boolean.TRUE.equals(updateEmployeeStatusDTO.getIsActive())
                && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new BadRequestBusinessException("Impossible d'activer ce salarié : il doit d'abord définir son mot de passe.");
        }

        user.setIsActive(updateEmployeeStatusDTO.getIsActive());
        userRepository.save(user);
        log.info("Statut du salarié {} mis à jour par le responsable de {}", user.getEmail(), myCompany.getName());
    }

    // --- Helpers ---

    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    private Company getMyCompany() {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.COMPANY) {
            throw new RuntimeException("Accès refusé.");
        }
        return employeeRepository.findByUser(user)
                .map(Employee::getCompany)
                .orElseThrow(() -> new RuntimeException("Aucune entreprise liée."));
    }

    private EmployeeListItemDTO mapToEmployeeListItemDTO(Employee e) {
        EmployeeListItemDTO dto = new EmployeeListItemDTO();
        dto.setId(e.getId());
        dto.setFirstName(e.getUser().getFirstName());
        dto.setLastName(e.getUser().getLastName());
        dto.setEmail(e.getUser().getEmail());
        dto.setCompanyName(e.getCompany().getName());
        dto.setEmployeeCode(e.getEmployeeCode());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setStatus(e.getUser().getIsActive() ? "Actif" : "Inactif");
        return dto;
    }

    private String generateUniqueEmployeeCode() {
        String code;
        do {
            code = "EMP-" + (int)(Math.random() * 900000 + 100000);
        } while (employeeRepository.existsByEmployeeCode(code));
        return code;
    }

    private List<CommandesParMoisDTO> buildEvolutionCommandes(Company company, LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> raw = orderRepository.countCommandesParMoisByCompany(company, debut, fin);
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
}
