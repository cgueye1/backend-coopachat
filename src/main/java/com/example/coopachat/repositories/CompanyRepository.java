package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Company
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * Vérifie si un code d'entreprise existe déjà
     *
     * @param companyCode Le code à vérifier
     * @return true si le code existe, false sinon
     */
    boolean existsByCompanyCode(String companyCode);
}

