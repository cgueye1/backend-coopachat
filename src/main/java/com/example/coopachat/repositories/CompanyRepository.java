package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * Récupère toutes les entreprises créées par un commercial
     *
     * @param commercial Le commercial
     * @param pageable Les paramètres de pagination (page, size, ...)
     * @return Page des entreprises du commercial
     */
    Page <Company> findByCommercial(Users commercial , Pageable pageable);
}
