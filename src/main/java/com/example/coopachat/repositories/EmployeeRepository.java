package com.example.coopachat.repositories;

import com.example.coopachat.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository  extends JpaRepository <Employee , Long> {
    
    /**
     * Vérifie si un code d'employé existe déjà
     * @param employeeCode Le code à vérifier
     * @return true si le code existe, false sinon
     */
    boolean existsByEmployeeCode(String employeeCode);
}
