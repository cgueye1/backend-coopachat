package com.example.coopachat.repositories;

import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Order;
import com.example.coopachat.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * vérifie si un numéro de commande existe déjà
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Recherche paginée des commandes salariés avec filtres optionnels
       - Si search est null : ignore la recherche
       - Si search a une valeur : recherche dans le numéro de commande OU le nom complet
       - Si status est null : ignore le filtre statut
       - Si status a une valeur : filtre par ce statut exact
       Résultats triés du plus récent au plus ancien
       Charge en une requête les items et les produits pour éviter products vides (lazy).
     **/
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT o FROM Order o " +
            "WHERE (:search IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " CONCAT(LOWER(o.employee.user.firstName), ' ', LOWER(o.employee.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findEmployeeOrders(
            @Param("search") String search,
            @Param("status") OrderStatus status,
            Pageable pageable);

    /**
     * Commandes du livreur (orders dans ses tournées), avec filtres optionnels.
     * - driverId : obligatoire (livreur connecté)
     * - deliveryDate : optionnel (ex. aujourd'hui)
     * - status : optionnel (À livrer / En cours / Livrée)
     * - search : optionnel (numéro de commande ou nom du client)
     * ordonne d'abord par date de livraison, puis id de la tournée, puis id de la commande du plus ancien au plus récent
     */
    @Query("SELECT o FROM Order o " +
            "WHERE o.deliveryTour IS NOT NULL AND o.deliveryTour.driver.id = :driverId " +
            "AND (:deliveryDate IS NULL OR o.deliveryDate = :deliveryDate) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "      LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(CONCAT(o.employee.user.firstName, ' ', o.employee.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY o.deliveryDate ASC, o.deliveryTour.id ASC, o.id ASC")
    List<Order> findDriverDeliveries(
            @Param("driverId") Long driverId,
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("status") OrderStatus status,
            @Param("search") String search);

    /**
     * Mes commandes (profil client / salarié) : commandes de l'employé avec filtres optionnels.
     * - search : numéro de commande (ex. CMD-1234)
     * - status : filtre par statut si présent
     * Tri : du plus récent au plus ancien.
     */
    @Query("SELECT o FROM Order o WHERE o.employee = :employee " +
            "AND (:search IS NULL OR :search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByEmployeeWithFilters(
            @Param("employee") Employee employee,
            @Param("search") String search,
            @Param("status") OrderStatus status,
            Pageable pageable);

    /**
     * Commandes éligibles pour une tournée : date + EN_ATTENTE + pas en tournée + employé actif.
     */
    @Query("SELECT DISTINCT o FROM Order o " +                           // on prend les commandes, sans doublon
            "JOIN FETCH o.employee e " +                            // on charge l'employé
            "JOIN FETCH e.user " +                                  // on charge l'utilisateur
            "WHERE o.deliveryDate = :deliveryDate AND o.status = :status " +  // filtre : même date + statut EN_ATTENTE
            "AND o.deliveryTour IS NULL AND e.user.isActive = true " +   // pas déjà en tournée + employé actif
            "ORDER BY o.id ASC")                                         // tri par id pour ordre stable
    List<Order> findEligibleOrdersForDate(
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("status") OrderStatus status);

}
