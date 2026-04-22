package com.example.coopachat.services.company;

import com.example.coopachat.dtos.employees.CreateEmployeeDTO;
import com.example.coopachat.dtos.employees.EmployeeListResponseDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeStatusDTO;
import com.example.coopachat.dtos.dashboard.company.CompanyDashboardKpisDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {

    /**
     * Récupère les indicateurs clés (KPIs) pour le dashboard de l'entreprise.
     */
    CompanyDashboardKpisDTO getDashboardKpis();

    /**
     * Récupère la liste des salariés de l'entreprise actuelle.
     */
    EmployeeListResponseDTO getMyEmployees(int page, int size, String search, Boolean isActive);

    /**
     * Crée un nouveau salarié pour l'entreprise actuelle.
     */
    void createEmployee(CreateEmployeeDTO employeeDTO);

    /**
     * Importe des salariés via un fichier Excel pour l'entreprise actuelle.
     */
    void importEmployees(MultipartFile file);

    /**
     * Exporte la liste des salariés au format Excel.
     */
    ByteArrayResource exportEmployees(String search, Boolean isActive);

    /**
     * Met à jour les informations d'un salarié.
     */
    void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO);

    /**
     * Active ou désactive le compte d'un salarié.
     */
    void updateEmployeeStatus(Long id, UpdateEmployeeStatusDTO updateEmployeeStatusDTO);
}
