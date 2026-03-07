package com.example.coopachat.repositories;

import com.example.coopachat.entities.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    boolean existsByName(String name);

    List<Fee> findByIsActiveTrue();

    /** Tarif par livraison (nom réservé "Tarif livreur" créé par l'admin). */
   Optional<Fee> findByNameAndIsActiveTrue(String name);
}
