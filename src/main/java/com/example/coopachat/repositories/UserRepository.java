package com.example.coopachat.repositories;

import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    // ============================================================================
    // 🔍 RECHERCHES
    // ============================================================================

    boolean existsByRefUser(String refUser);

    /**
     * Attention : un rôle n'est pas unique (il peut y avoir plusieurs admins, commerciaux, etc.).
     * Utiliser plutôt {@link #existsByRole(UserRole)} ou {@link #findAllByRole(UserRole)}.
     */
    Optional<Users> findByRole(UserRole role);

    boolean existsByRole(UserRole role);

    List<Users> findAllByRole(UserRole role);

    /** Vérifie si un email existe déjà dans la base de données */
    Boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    /** Recherche par numéro de téléphone (pour connexion email ou téléphone). */
    Optional<Users> findByPhone(String phone);

    /** Vérifie si un téléphone existe déjà dans la base de données */
    Boolean existsByPhone(String phone);

    /** Vérifie si un autre utilisateur (id différent) a déjà cet email (pour mise à jour). */
    Boolean existsByEmailAndIdNot(String email, Long id);

    /** Vérifie si un autre utilisateur (id différent) a déjà ce téléphone (pour mise à jour). */
    Boolean existsByPhoneAndIdNot(String phone, Long id);

    void deleteAllByRole(UserRole role);

    /**
     * Liste paginée des utilisateurs avec filtres optionnels (admin).
     * - search : recherche sur prénom, nom, nom complet ou email (insensible à la casse)
     * - role : u.role <> EMPLOYEE et COMPANY = on exclut tous les employés et entreprises, quoi qu’il arrive.
     * Puis AND (:role IS NULL OR u.role = :role) = si l’admin choisit un rôle (Commercial, RL, …) on filtre dessus parmi ceux qui restent sinon si role est null on prend tous les autres rôles
     * - isActive : filtre par statut (null = tous, true = actif, false = inactif)
     * Tri : du plus récent au plus ancien (createdAt DESC).
     */
    @Query("SELECT u FROM Users u " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "      LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (u.role NOT IN (com.example.coopachat.enums.UserRole.EMPLOYEE, com.example.coopachat.enums.UserRole.COMPANY, com.example.coopachat.enums.UserRole.SUPPLIER)) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive) " +
            "ORDER BY u.createdAt DESC")
    Page<Users> findAllWithFilters(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // ============================================================================
    // 📊 STATISTIQUES (admin)
    // ============================================================================

    /** Nombre d'utilisateurs actifs (isActive = true). */
    long countByIsActiveTrue();

    /** Nombre d'utilisateurs inactifs (isActive = false). */
    long countByIsActiveFalse();

    /** Comptages admin : même périmètre que findAllWithFilters (hors salariés et entreprises). */
    @Query("SELECT COUNT(u) FROM Users u WHERE u.role NOT IN (com.example.coopachat.enums.UserRole.EMPLOYEE, com.example.coopachat.enums.UserRole.COMPANY, com.example.coopachat.enums.UserRole.SUPPLIER)")
    long countExcludingEmployee();

    @Query("SELECT COUNT(u) FROM Users u WHERE u.role NOT IN (com.example.coopachat.enums.UserRole.EMPLOYEE, com.example.coopachat.enums.UserRole.COMPANY, com.example.coopachat.enums.UserRole.SUPPLIER) AND u.isActive = true")
    long countByIsActiveTrueExcludingEmployee();

    @Query("SELECT COUNT(u) FROM Users u WHERE u.role NOT IN (com.example.coopachat.enums.UserRole.EMPLOYEE, com.example.coopachat.enums.UserRole.COMPANY, com.example.coopachat.enums.UserRole.SUPPLIER) AND u.isActive = false")
    long countByIsActiveFalseExcludingEmployee();

    /** Nombre d'utilisateurs pour un rôle donné (pour le graphique "Utilisateurs par rôle"). */
    long countByRole(UserRole role);

    /** Liste des utilisateurs actifs ayant le rôle donné */
    List<Users> findByRoleAndIsActiveTrue(UserRole role);
}