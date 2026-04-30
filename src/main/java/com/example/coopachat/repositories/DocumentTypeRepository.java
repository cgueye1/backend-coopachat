package com.example.coopachat.repositories;

import com.example.coopachat.entities.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository pour l'entité DocumentType (Types de documents prérequis)
 */
@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    // Vérifie si un type de document avec le même nom ou synonyme existe
    @Query("SELECT COUNT(d) > 0 FROM DocumentType d WHERE LOWER(d.name) = LOWER(:name) OR :name MEMBER OF d.synonyms")
    boolean existsByNameOrSynonym(@Param("name") String name);
}
