package com.example.coopachat.repositories;

import com.example.coopachat.entities.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository pour l'entité DocumentType (Types de documents prérequis)
 */
@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    // Vérifie si un type de document avec le même nom (":name") passé par le user  existe déjà ou bien le nom qu'il a entré est dans la collection des synonymes 
    @Query("SELECT COUNT(d) > 0 FROM DocumentType d WHERE LOWER(d.name) = LOWER(:name) OR :name MEMBER OF d.synonyms")
    boolean existsByNameOrSynonym(@Param("name") String name);

    // Vérifie l'existence d'un nom ou synonyme pour un AUTRE document (utilisé lors de l'update)
    @Query("SELECT COUNT(d) > 0 FROM DocumentType d WHERE (LOWER(d.name) = LOWER(:name) OR :name MEMBER OF d.synonyms) AND d.id <> :id")
    boolean existsByNameOrSynonymExcludingId(@Param("name") String name, @Param("id") Long id);

    /**
     * Recherche filtrée et paginée des types de documents
     * Filtre sur le nom  ou les synonymes
     */
    @Query("SELECT d FROM DocumentType d WHERE " +
            "(:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "EXISTS (SELECT s FROM d.synonyms s WHERE LOWER(s) LIKE LOWER(CONCAT('%', :search, '%')))) AND " +
            "(:isActive IS NULL OR d.isActive = :isActive)")
    Page<DocumentType> findAllWithFilters(@Param("search") String search, @Param("isActive") Boolean isActive, Pageable pageable);

    List<DocumentType> findAllByIsActiveTrue();

    List<DocumentType> findAllByIsActiveTrueAndIsIdentityVerificationTrue();

    long countByIsIdentityVerificationTrue();

    long countByIsIdentityVerificationFalse();
}
