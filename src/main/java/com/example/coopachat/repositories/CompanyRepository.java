package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.CompanySector;
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
     * Récupère toutes les entreprises créées par un commercial avec pagination
     *
     * @param commercial Le commercial
     * @param pageable Les paramètres de pagination (page, size, sort)
     * @return Page des entreprises du commercial
     */
    Page<Company> findByCommercial(Users commercial, Pageable pageable);

    /**
     * Compte le nombre total d'entreprises créées par un commercial
     *
     * @param commercial Le commercial
     * @return Le nombre total d'entreprises du commercial
     */
    long countByCommercial(Users commercial);

    /**
     * Compte le nombre d'entreprises créées par un commercial selon leur statut actif/inactif
     *
     * @param commercial Le commercial
     * @param isActive true pour les entreprises actives, false pour les inactives
     * @return Le nombre d'entreprises correspondant au statut
     */
    long countByCommercialAndIsActive(Users commercial, Boolean isActive);

    // ============================================================================
    // 🔍 MÉTHODES DE RECHERCHE ET FILTRES
    // ============================================================================

    /**
     * Récupère les entreprises d'un commercial avec recherche par nom (pagination)
     *
     * @param commercial Le commercial
     * @param name Le terme de recherche (recherche partielle insensible à la casse)
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndNameContainingIgnoreCase(Users commercial, String name, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec recherche par nom et filtre secteur (pagination)
     *
     * @param commercial Le commercial
     * @param name Le terme de recherche
     * @param sector Le secteur d'activité
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndNameContainingIgnoreCaseAndSector(Users commercial, String name, CompanySector sector, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec recherche par nom et filtre actif/inactif (pagination)
     *
     * @param commercial Le commercial
     * @param name Le terme de recherche
     * @param isActive true pour les entreprises actives, false pour les inactives
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndNameContainingIgnoreCaseAndIsActive(Users commercial, String name, Boolean isActive, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec recherche par nom, filtre secteur et filtre actif/inactif (pagination)
     *
     * @param commercial Le commercial
     * @param name Le terme de recherche
     * @param sector Le secteur d'activité
     * @param isActive true pour les entreprises actives, false pour les inactives
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndNameContainingIgnoreCaseAndSectorAndIsActive(Users commercial, String name, CompanySector sector, Boolean isActive, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec filtre secteur seulement (pagination)
     *
     * @param commercial Le commercial
     * @param sector Le secteur d'activité
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndSector(Users commercial, CompanySector sector, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec filtre actif/inactif seulement (pagination)
     *
     * @param commercial Le commercial
     * @param isActive true pour les entreprises actives, false pour les inactives
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndIsActive(Users commercial, Boolean isActive, Pageable pageable);

    /**
     * Récupère les entreprises d'un commercial avec filtre secteur et actif/inactif (pagination)
     *
     * @param commercial Le commercial
     * @param sector Le secteur d'activité
     * @param isActive true pour les entreprises actives, false pour les inactives
     * @param pageable Les paramètres de pagination
     * @return Page des entreprises correspondantes
     */
    Page<Company> findByCommercialAndSectorAndIsActive(Users commercial, CompanySector sector, Boolean isActive, Pageable pageable);
}
