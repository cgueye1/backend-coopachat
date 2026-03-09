package com.example.coopachat.repositories;

import com.example.coopachat.entities.CompanySector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanySectorRepository extends JpaRepository<CompanySector, Long> {

    Optional<CompanySector> findByNameIgnoreCase(String name);
}
