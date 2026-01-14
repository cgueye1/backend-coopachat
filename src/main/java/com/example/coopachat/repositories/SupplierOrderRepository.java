package com.example.coopachat.repositories;

import com.example.coopachat.entities.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pour l'entité CommandeFournisseur
 */
@Repository
public interface SupplierOrderRepository extends JpaRepository <SupplierOrder, Long>{

   /**
    * Vérifie si le numéro de cmd existe déjà
    */
    boolean existsByOrderNumber(String orderNumber);
}