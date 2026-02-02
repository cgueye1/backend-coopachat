package com.example.coopachat.repositories;

import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.EmployeeDeliveryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeDeliveryPreferenceRepository extends JpaRepository <EmployeeDeliveryPreference, Long> {

    Optional<EmployeeDeliveryPreference> findByEmployee(Employee employee);
}
