package com.example.coopachat.repositories;

import com.example.coopachat.entities.SupplierOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité SupplierOrderItem (Produit dans une commande fournisseur)
 */
@Repository
public interface SupplierOrderItemRepository extends JpaRepository<SupplierOrderItem, Long> {

}



