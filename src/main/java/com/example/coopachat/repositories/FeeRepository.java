package com.example.coopachat.repositories;

import com.example.coopachat.entities.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    boolean existsByName(String name);

    List<Fee> findByIsActiveTrue();
}
