package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOptionRepository extends JpaRepository<DeliveryOption, Long> {
  boolean existsByName(String name);

  /** Options actives (pour le salarié qui passe commande). */
  java.util.List<DeliveryOption> findByIsActiveTrue();
}
