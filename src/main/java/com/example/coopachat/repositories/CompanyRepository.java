package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Vérifie si un email de contact existe déjà pour une entreprise
     */
    boolean existsByContactEmailIgnoreCase(String contactEmail);

    /**
     * Vérifie si un email de contact existe déjà pour une autre entreprise (exclut l'id donné)
     */
    boolean existsByContactEmailIgnoreCaseAndIdNot(String contactEmail, Long excludeId);

    /**
     * Vérifie si un numéro de téléphone de contact existe déjà pour une entreprise
     */
    boolean existsByContactPhone(String contactPhone);

    /**
     * Vérifie si un numéro de téléphone existe déjà pour une autre entreprise (exclut l'id donné)
     */
    boolean existsByContactPhoneAndIdNot(String contactPhone, Long excludeId);

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

    /**
     * Récupère les entreprises d'un commercial avec filtres optionnels dont type (partenaires/prospects) et statut de prospection.
     * companyType: "partenaires" = uniquement PARTNER_SIGNED, "prospects" = tout sauf PARTNER_SIGNED (optionnellement filtré par prospectionStatus).
     */
    @Query("SELECT c FROM Company c WHERE c.commercial = :commercial " +
            "AND (:companyType IS NULL OR (:companyType = 'partenaires' AND c.status = com.example.coopachat.enums.CompanyStatus.PARTNER_SIGNED) " +
            "     OR (:companyType = 'prospects' AND c.status <> com.example.coopachat.enums.CompanyStatus.PARTNER_SIGNED AND (:prospectionStatus IS NULL OR c.status = :prospectionStatus))) " +
            "AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:sector IS NULL OR c.sector = :sector) " +
            "AND (:isActive IS NULL OR c.isActive = :isActive)")
    Page<Company> findByCommercialAndOptionalFilters(
            @Param("commercial") Users commercial,
            @Param("companyType") String companyType,
            @Param("prospectionStatus") CompanyStatus prospectionStatus,
            @Param("search") String search,
            @Param("sector") CompanySector sector,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    /**
     * Récupère les N dernières entreprises en prospection (status != PARTNER_SIGNED) du commercial, tri par id décroissant.
     * Utiliser Pageable pour limiter (ex. PageRequest.of(0, 3, Sort.by(DESC, "id"))).
     */
    @Query("SELECT c FROM Company c WHERE c.commercial = :commercial AND c.status <> com.example.coopachat.enums.CompanyStatus.PARTNER_SIGNED ORDER BY c.id DESC")
    List<Company> findByCommercialAndStatusNotOrderByIdDesc(@Param("commercial") Users commercial, Pageable pageable);

    /**
     * Compte les entreprises d'un commercial par statut de prospection.
     */
    long countByCommercialAndStatus(Users commercial, CompanyStatus status);

    /**
     * Compte les entreprises d'un commercial dont le statut est différent de PARTNER_SIGNED (prospects).
     */
    long countByCommercialAndStatusNot(Users commercial, CompanyStatus status);

    /**
     * Compte les entreprises partenaires (PARTNER_SIGNED) d'un commercial selon isActive.
     */
    long countByCommercialAndStatusAndIsActive(Users commercial, CompanyStatus status, Boolean isActive);

    // ============================================================================
    // 📊 COMPTAGES GLOBAUX (sans filtre commercial)
    // ============================================================================

    /**
     * Compte toutes les entreprises par statut de prospection (tous commerciaux confondus).
     */
    long countByStatus(CompanyStatus status);

    /**
     * Compte toutes les entreprises dont le statut est différent de la valeur donnée.
     */
    long countByStatusNot(CompanyStatus status);
}
