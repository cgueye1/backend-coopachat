package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverDocument;
import com.example.coopachat.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {
    
    List<DriverDocument> findByDriverId(Long driverId);
    
    boolean existsByDriverIdAndDocumentTypeIdAndStatus(Long driverId, Long documentTypeId, DocumentStatus status);

    Optional<DriverDocument> findByDriverIdAndDocumentTypeId(Long driverId, Long documentTypeId);

    long countByStatus(DocumentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT d.driver.id) FROM DriverDocument d")
    long countDistinctDriver();
}
