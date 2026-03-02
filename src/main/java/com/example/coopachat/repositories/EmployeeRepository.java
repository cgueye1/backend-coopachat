package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.OrderItem;
import com.example.coopachat.entities.Users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    /**
     * Vérifie si un code d'employé existe déjà
     * @param employeeCode Le code à vérifier
     * @return true si le code existe, false sinon
     */
    boolean existsByEmployeeCode(String employeeCode);

    /**
     * Récupère un employé par l'email de son utilisateur associé
     *
     * @param email Email de l'utilisateur
     * @return Employé correspondant si trouvé
     */
    Optional<Employee> findByUserEmail(String email);

    /**
     * Récupère un employé par son User
     */
    Optional<Employee> findByUser(Users user);

    /**
     * Compte le nombre total d'employés créés par un commercial
     *
     * @param commercial Le commercial
     * @return Le nombre total d'employés du commercial
     */
    long countByCreatedBy(Users commercial);

    /**
     * Compte le nombre d'employés créés par un commercial selon leur statut actif/inactif
     *
     * @param commercial Le commercial
     * @param isActive true pour les employés actifs, false pour les inactifs
     * @return Le nombre d'employés correspondant au statut
     */
    long countByCreatedByAndUserIsActive(Users commercial, Boolean isActive);

    /**
     * Compte le nombre d'employés créés par un commercial entre deux dates (ex. pour "nouveaux ce mois").
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.createdBy = :commercial AND e.createdAt BETWEEN :start AND :end")
    long countByCreatedByAndCreatedAtBetween(
            @Param("commercial") Users commercial,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Compte tous les employés dont l'utilisateur associé est actif ou non (sans filtre commercial). */
    long countByUserIsActive(Boolean isActive);

    /** Compte tous les employés créés entre start et end (sans filtre commercial). */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ============================================================================
    // 🔍 MÉTHODES DE RECHERCHE ET FILTRES
    // ============================================================================

    /**
     * Récupère toutes les employés créés par un commercial avec pagination
     *
     * @param commercial Le commercial
     * @param pageable Les paramètres de pagination (page, size)
     * @return Page des employés du commercial
     */
    Page<Employee> findByCreatedBy(Users commercial, Pageable pageable);
    /**
     * Récupère les employés d'un commercial avec recherche par prénom ou nom (pagination)
     *
     * @param commercial Le commercial
     * @param firstName Le terme de recherche pour le prénom (recherche partielle insensible à la casse)
     * @param lastName Le terme de recherche pour le nom (recherche partielle insensible à la casse)
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCase(Users commercial, String firstName, String lastName, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec recherche par prénom ou nom et filtre par entreprise (pagination)
     *
     * @param commercial Le commercial
     * @param firstName Le terme de recherche pour le prénom
     * @param lastName Le terme de recherche pour le nom
     * @param company L'entreprise
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompany(Users commercial, String firstName, String lastName, Company company, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec recherche par prénom ou nom et filtre actif/inactif (pagination)
     *
     * @param commercial Le commercial
     * @param firstName Le terme de recherche pour le prénom
     * @param lastName Le terme de recherche pour le nom
     * @param isActive true pour les employés actifs, false pour les inactifs
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndUserIsActive(Users commercial, String firstName, String lastName, Boolean isActive, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec recherche par prénom ou nom, filtre entreprise et filtre actif/inactif (pagination)
     *
     * @param commercial Le commercial
     * @param firstName Le terme de recherche pour le prénom
     * @param lastName Le terme de recherche pour le nom
     * @param company L'entreprise
     * @param isActive true pour les employés actifs, false pour les inactifs
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompanyAndUserIsActive(Users commercial, String firstName, String lastName, Company company, Boolean isActive, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec filtre par entreprise seulement
     *
     * @param commercial Le commercial
     * @param company L'entreprise
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndCompany(Users commercial, Company company, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec filtre actif/inactif seulement
     *
     * @param commercial Le commercial
     * @param isActive true pour les employés actifs, false pour les inactifs
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndUserIsActive(Users commercial, Boolean isActive, Pageable pageable);

    /**
     * Récupère les employés d'un commercial avec filtre entreprise et actif/inactif
     *
     * @param commercial Le commercial
     * @param company L'entreprise
     * @param isActive true pour les employés actifs, false pour les inactifs
     * @param pageable Les paramètres de pagination
     * @return Page des employés correspondants
     */
    Page<Employee> findByCreatedByAndCompanyAndUserIsActive(Users commercial, Company company, Boolean isActive, Pageable pageable);

    /**
     * Compte le nombre d'employés d'une entreprise (pour les détails entreprise partenaire).
     */
    long countByCompany(Company company);
}
