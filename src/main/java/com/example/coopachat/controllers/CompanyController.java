package com.example.coopachat.controllers;

import com.example.coopachat.dtos.dashboard.company.CompanyDashboardKpisDTO;
import com.example.coopachat.dtos.employees.CreateEmployeeDTO;
import com.example.coopachat.dtos.employees.EmployeeListResponseDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeDTO;
import com.example.coopachat.dtos.employees.UpdateEmployeeStatusDTO;
import com.example.coopachat.services.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/entreprise")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/dashboard/kpis")
    public ResponseEntity<CompanyDashboardKpisDTO> getDashboardKpis() {
        return ResponseEntity.ok(companyService.getDashboardKpis());
    }

    @GetMapping("/employees")
    public ResponseEntity<EmployeeListResponseDTO> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(companyService.getMyEmployees(page, size, search, isActive));
    }

    @PostMapping("/employees")
    public ResponseEntity<Void> createEmployee(@RequestBody CreateEmployeeDTO employeeDTO) {
        companyService.createEmployee(employeeDTO);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Void> updateEmployee(@PathVariable Long id, @RequestBody UpdateEmployeeDTO employeeDTO) {
        companyService.updateEmployee(id, employeeDTO);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/employees/{id}/status")
    public ResponseEntity<Void> updateEmployeeStatus(@PathVariable Long id, @RequestBody UpdateEmployeeStatusDTO statusDTO) {
        companyService.updateEmployeeStatus(id, statusDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/employees/import")
    public ResponseEntity<Void> importEmployees(@RequestParam("file") MultipartFile file) {
        companyService.importEmployees(file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/employees/export")
    public ResponseEntity<ByteArrayResource> exportEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive) {
        ByteArrayResource resource = companyService.exportEmployees(search, isActive);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=salaries.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
