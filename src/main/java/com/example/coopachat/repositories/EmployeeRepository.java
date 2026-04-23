package com.example.coopachat.repositories;

import com.example.coopachat.entities.Company;
import com.example.coopachat.entities.Employee;
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
    // Liste salariés par entreprise (GET commercial /employees), sans filtre createdBy.
    // ============================================================================

    /**
     * Salariés d'une entreprise (pagination).
     */
    Page<Employee> findByCompany(Company company, Pageable pageable);

    /**
     * Idem, recherche partielle insensible à la casse sur prénom ou nom (utilisateur lié).
     */
    Page<Employee> findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompany(
            String firstName, String lastName, Company company, Pageable pageable);

    /**
     * Idem avec filtre actif/inactif sur l'utilisateur associé.
     */
    Page<Employee> findByCompanyAndUserIsActive(Company company, Boolean isActive, Pageable pageable);

    /**
     * Recherche prénom/nom + entreprise + actif/inactif.
     */
    Page<Employee> findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseAndCompanyAndUserIsActive(
            String firstName, String lastName, Company company, Boolean isActive, Pageable pageable);

    /**
     * Compte le nombre d'employés d'une entreprise (pour les détails entreprise partenaire).
     */
    long countByCompany(Company company);

    /**
     * Compte le nombre d'employés d'une entreprise selon leur statut.
     */
    long countByCompanyAndUserIsActive(Company company, Boolean isActive);

    /**
     * Récupère directement l'entreprise associée à un utilisateur.
     */
    @Query("SELECT e.company FROM Employee e WHERE e.user = :user")
    Optional<Company> findCompanyByUser(@Param("user") Users user);

    /**
     * Récupère tous les employés d'une entreprise (sans pagination).
     */
    List<Employee> findAllByCompany(Company company);
}
