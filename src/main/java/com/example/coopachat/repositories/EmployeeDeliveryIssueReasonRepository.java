package com.example.coopachat.repositories;

import com.example.coopachat.entities.EmployeeDeliveryIssueReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeDeliveryIssueReasonRepository extends JpaRepository<EmployeeDeliveryIssueReason, Long> {
}
