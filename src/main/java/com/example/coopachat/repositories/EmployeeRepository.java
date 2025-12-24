package com.example.coopachat.repositories;

import com.example.coopachat.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pour l'entité Employee
 */
@Repository
public interface EmployeeRepository  extends JpaRepository <Employee, Long> {

}
