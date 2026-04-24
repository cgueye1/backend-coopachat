package com.example.coopachat.repositories;

import com.example.coopachat.entities.Supplier;
import com.example.coopachat.enums.SupplierType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByEmail(String email);
    Optional<Supplier> findByPhone(String phone);
    Optional<Supplier> findByNinea(String ninea);
    long countByIsActive(boolean isActive);
    java.util.List<Supplier> findByIsActiveTrue();

    // Recherche avec filtres (on affiche uniquement les actifs par défaut si isActive est null, 
    // mais ici on laisse le choix à l'appelant. Le user a dit "Afficher uniquement les fournisseurs actifs dans les listes")
    @Query("SELECT DISTINCT s FROM Supplier s LEFT JOIN s.categories c WHERE " +
            "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:type IS NULL OR s.type = :type) " +
            "AND (:isActive IS NULL OR s.isActive = :isActive)")
    Page<Supplier> findWithFilters(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("type") SupplierType type,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
