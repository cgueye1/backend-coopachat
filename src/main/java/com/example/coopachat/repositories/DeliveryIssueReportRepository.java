package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryIssueReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryIssueReportRepository extends JpaRepository<DeliveryIssueReport, Long> {

    List<DeliveryIssueReport> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
