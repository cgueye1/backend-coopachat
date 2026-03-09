package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryIssueReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryIssueReasonRepository extends JpaRepository<DeliveryIssueReason, Long> {
}
