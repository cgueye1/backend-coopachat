package com.example.coopachat.repositories;

import com.example.coopachat.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Supplier (Fournisseur)
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}


