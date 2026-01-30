package com.example.coopachat.repositories;

import com.example.coopachat.entities.Order;
import com.example.coopachat.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Recherche paginée des commandes salariés avec filtres optionnels
       - Si search est null : ignore la recherche
       - Si search a une valeur : recherche dans le numéro de commande OU le nom complet
       - Si status est null : ignore le filtre statut
       - Si status a une valeur : filtre par ce statut exact
       Résultats triés du plus récent au plus ancien
     **/
    @Query("SELECT o FROM Order o " +
            "WHERE (:search IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " CONCAT(LOWER(o.employee.user.firstName), ' ', LOWER(o.employee.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findEmployeeOrders(
            @Param("search") String search,
            @Param("status") OrderStatus status,
            Pageable pageable);
}
