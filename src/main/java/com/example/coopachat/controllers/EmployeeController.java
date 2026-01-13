package com.example.coopachat.controllers;


import com.example.coopachat.dtos.auth.ResetPasswordRequestDTO;
import com.example.coopachat.services.Employee.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

 @RestController
 @RequestMapping("/api/employees")
 @RequiredArgsConstructor
 @Tag(name = "Salariés", description = "API pour la gestion des salariés ")
public class EmployeeController {

    private final EmployeeService employeeService;


    @Operation(
            summary = "Activer le compte salarié",
            description = "Active le compte d'un salarié et crée son mot de passe via le token d'invitation reçu par email. " +
                    "Le token doit être valide et non expiré."
    )
    @PostMapping("mobile/activate")
    public ResponseEntity<String> activateEmployeeAccount(@RequestBody @Valid ResetPasswordRequestDTO requestDTO) {
        employeeService.activateEmployeeAccount(
                requestDTO.getToken(),
                requestDTO.getNewPassword(),
                requestDTO.getConfirmPassword()
        );
        return ResponseEntity.ok("Compte activé avec succès. Vous pouvez maintenant vous connecter.");
    }
}
