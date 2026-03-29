package com.example.coopachat.repositories;

import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"employee", "employee.user"})
    List<Order> findByDeliveryTour_Id(Long deliveryTourId);

    /**
     * Charge une commande avec ses lignes (items) et les produits associés, pour les détails RL.
     * Évite les chargements lazy et garantit que getItems() est rempli avec product + category.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH o.employee e " +
            "LEFT JOIN FETCH e.user " +
            "WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithItemsAndProducts(@Param("id") Long id);

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
       Résultats triés du plus récent au plus ancien.
       EntityGraph charge items et items.product dans la même session pour éviter LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "employee", "employee.user"})
    @Query("SELECT DISTINCT o FROM Order o " +
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
     * Join -> seules les lignes qui ont une correspondance dans les deux tables sont retournées.
     * JOIN FETCH, en récupérant chaque commande, Hibernate charge en même temps son employé et l'utilisateur lié à cet employé, le tout dans une seule requête SQL
     */
    @Query("SELECT DISTINCT o FROM Order o " +                           // on prend les commandes, sans doublon
            "JOIN FETCH o.employee e " +                            // on charge l'employé
            "JOIN FETCH e.user " +                                  // on charge l'utilisateur
            "WHERE o.deliveryDate <= :deliveryDate AND o.status = :status " +  // filtre : même date + statut EN_ATTENTE
            "AND o.deliveryTour IS NULL AND e.user.isActive = true " +   // pas déjà en tournée + employé actif
            "ORDER BY o.id ASC")                                         // tri par id pour ordre stable
    List<Order> findEligibleOrdersForDate(
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("status") OrderStatus status);

    /**
     * Détail complet pour le livreur : commande LIVREE appartenant au livreur (via tournée),
     * avec chargement eager de employee+user, items+product et payment pour éviter LazyInitialization.
     */
    @Query("""
           SELECT DISTINCT o
           FROM Order o
           JOIN FETCH o.employee e
           JOIN FETCH e.user u
           LEFT JOIN FETCH o.items it
           LEFT JOIN FETCH it.product p
           LEFT JOIN FETCH o.payment pay
           JOIN o.deliveryTour t
           WHERE o.id = :orderId
             AND t.driver.id = :driverId
           """)
    java.util.Optional<Order> findDriverDeliveredOrderDetails(
            @Param("orderId") Long orderId,
            @Param("driverId") Long driverId);

    /**
     * Commandes de l'employé ayant un paiement avec le statut donné (ex. PAID), tri par date de paiement décroissante.
     */
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.payment p " +        // JOIN FETCH = charge Order + Payment en 1 requête
            "WHERE o.employee = :employee " +   // Filtre : commandes de cet employé
            "AND p.status = :status " +         // Filtre : paiements avec ce statut
            "ORDER BY p.paidAt DESC")           // Tri : plus récents en premier
    List<Order> findPaidOrdersByEmployee(
            @Param("employee") Employee employee,
            @Param("status") PaymentStatus status
    );

    /**
     * Nombre de commandes livrées par un livreur à une date donnée (ex. aujourd'hui).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryTour.driver.id = :driverId AND o.status = :status AND o.deliveryDate = :deliveryDate")
    long countByDeliveryTourDriverIdAndStatusAndDeliveryDate(
            @Param("driverId") Long driverId,
            @Param("status") OrderStatus status,
            @Param("deliveryDate") LocalDate deliveryDate);

    /**
     * Nombre total de commandes livrées par un livreur (toutes dates).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryTour.driver.id = :driverId AND o.status = :status")
    long countByDeliveryTourDriverIdAndStatus(
            @Param("driverId") Long driverId,
            @Param("status") OrderStatus status);

    /**
     * Nombre de commandes livrées par un livreur dont deliveryCompletedAt est entre start et end.
     * Utilisé pour "Livraisons aujourd'hui" (livraisons effectivement complétées aujourd'hui).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryTour.driver.id = :driverId AND o.status = :status AND o.deliveryCompletedAt BETWEEN :start AND :end")
    long countByDeliveryTourDriverIdAndStatusAndDeliveryCompletedAtBetween(
            @Param("driverId") Long driverId,
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Livraisons (commandes LIVREE) d'un livreur sur une plage de dates, pour le dashboard livreur.
     * On se base sur deliveryCompletedAt afin de compter les commandes réellement remises.
     */
    @Query("""
           SELECT o FROM Order o
           WHERE o.deliveryTour.driver.id = :driverId
             AND o.status = :status
             AND o.deliveryCompletedAt BETWEEN :start AND :end
           """)
    List<Order> findDriverDeliveriesBetween(
            @Param("driverId") Long driverId,
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Nombre de commandes avec le statut donné, créées dans la période [start, end].
     */
    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    /**
     * Nombre de commandes ayant le statut donné et dont la date de livraison effective (deliveryCompletedAt)
     * est dans la période [start, end]. Utilisé pour le graphique "Commandes vs Livraisons" (livraisons par jour).
     * <p>
     * Exemple : pour le 10/02, compter les commandes LIVREE avec deliveryCompletedAt entre
     * 10/02 00:00:00 et 10/02 23:59:59.
     * </p>
     *
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deliveryCompletedAt IS NOT NULL AND o.deliveryCompletedAt BETWEEN :start AND :end")
    long countByStatusAndDeliveryCompletedAtBetween(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Commandes avec le statut donné et non affectées à une tournée (à valider par le RL). */
    long countByStatusAndDeliveryTourIsNull(OrderStatus status);

    /** Commandes avec le statut donné, date de livraison avant la date donnée, non affectées à une tournée (en retard). */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deliveryDate < :before AND o.deliveryTour IS NULL")
    long countByStatusAndDeliveryDateBeforeAndDeliveryTourIsNull(
            @Param("status") OrderStatus status,
            @Param("before") LocalDate before);

    /** Commandes en retard : statut donné et date de livraison strictement avant :now (ex. LocalDate.now()) (pour notification RL et Admin). */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deliveryDate < :now ORDER BY o.deliveryDate ASC")
    List<Order> findByStatusAndDeliveryDateBefore(
            @Param("status") OrderStatus status,
            @Param("now") LocalDate now);

    /**
     * Nombre de commandes avec le statut donné et date de livraison prévue strictement avant {@code now}.
     * GET /admin/alerts ; dashboard livraisons par jour ({@code nbRetard} avec statut EN_ATTENTE).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deliveryDate < :now")
    long countByStatusAndDeliveryDateBefore(
            @Param("status") OrderStatus status,
            @Param("now") LocalDate now);

    /** Nombre de commandes ayant le statut donné (ex. VALIDEE). */
    long countByStatus(OrderStatus status);

    /** Nombre de commandes créées dans la période [start, end] (tous statuts). Pour graphique "Commandes par jour". */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** Nombre de commandes ayant utilisé un coupon (coupon non null), créées entre start et end. Pour graphique "Coupons utilisés par jour". */
    long countByCouponIsNotNullAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** Nombre de commandes dont le statut est dans la liste et la date de livraison = day. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.deliveryDate = :day")
    long countByStatusInAndDeliveryDate(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("day") LocalDate day);

    /** Date de livraison prévue = jour, hors annulées. Dashboard livraisons par jour ({@code nbPrevues}). */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryDate = :day AND o.status <> :annulee")
    long countByDeliveryDateExcludingCancelled(
            @Param("day") LocalDate day,
            @Param("annulee") OrderStatus annulee);

    /** Statut et date de livraison prévue = jour (ex. LIVREE pour {@code nbLivreesALaDate}). */
    long countByStatusAndDeliveryDate(OrderStatus status, LocalDate deliveryDate);

    /** Nombre de commandes EN_ATTENTE, date de livraison = day, non assignées à une tournée. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deliveryDate = :day AND o.deliveryTour IS NULL")
    long countByStatusAndDeliveryDateAndDeliveryTourIsNull(
            @Param("status") OrderStatus status,
            @Param("day") LocalDate day);

    /**
     * Calendrier RL (mois) — commandes EN_ATTENTE non planifiées, groupées par deliveryDate.
     * Retour : lignes (LocalDate deliveryDate, long count).
     */
    @Query("""
           SELECT o.deliveryDate, COUNT(o)
           FROM Order o
           WHERE o.status = :status
             AND o.deliveryTour IS NULL
             AND o.deliveryDate BETWEEN :start AND :end
           GROUP BY o.deliveryDate
           """)
    List<Object[]> countPendingOrdersByDeliveryDateBetween(
            @Param("status") OrderStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Calendrier RL (mois) — commandes "planifiées" = assignées à un livreur (dans une tournée),
     * au sens "dans une tournée active" (statuts inclus dans :tourStatuses).
     * Groupées par date de livraison de la commande (Order.deliveryDate).
     *
     * Retour : lignes (LocalDate deliveryDate, long count).
     */
    @Query("""
           SELECT o.deliveryDate, COUNT(o)
           FROM Order o
           JOIN o.deliveryTour t
           WHERE o.deliveryDate BETWEEN :start AND :end
             AND t.status IN :tourStatuses
           GROUP BY o.deliveryDate
           """)
    List<Object[]> countPlannedOrdersByDeliveryDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("tourStatuses") List<DeliveryTourStatus> tourStatuses);

    /**
     * Commandes du livreur dont la tournée a un des statuts donnés (paginé).
     * triés par date de livraison, puis id de la tournée, puis id de la commande du plus ancien au plus récent
     */
    @Query("SELECT o FROM Order o WHERE o.deliveryTour IS NOT NULL AND o.deliveryTour.driver.id = :driverId AND o.deliveryTour.status IN :statuses ORDER BY o.deliveryDate ASC, o.deliveryTour.id ASC, o.id ASC")
    Page<Order> findByDriverAndTourStatusIn(
            @Param("driverId") Long driverId,
            @Param("statuses") List<DeliveryTourStatus> statuses,
            Pageable pageable);

    /**
     * Commandes du livreur dont la tournée a le statut donné (paginé).
     * triés par date de livraison, puis id de la tournée, puis id de la commande du plus ancien au plus récent
     */
    @Query("SELECT o FROM Order o WHERE o.deliveryTour IS NOT NULL AND o.deliveryTour.driver.id = :driverId AND o.deliveryTour.status = :status ORDER BY o.deliveryDate ASC, o.deliveryTour.id ASC, o.id ASC")
    Page<Order> findByDriverAndTourStatus(
            @Param("driverId") Long driverId,
            @Param("status") DeliveryTourStatus status,
            Pageable pageable);

    // ============================================================================
    // Dashboard commercial (commandes des employés du commercial)
    // ============================================================================

    /** Nombre de commandes passées par les employés du commercial, créées entre start et end. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.employee.createdBy = :commercial AND o.createdAt BETWEEN :start AND :end")
    long countByEmployeeCreatedByAndCreatedAtBetween(
            @Param("commercial") Users commercial,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Somme des totalPrice des commandes LIVREE des employées  du commercial dont deliveryCompletedAt est entre start et end. */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.employee.createdBy = :commercial AND o.status = :status AND o.deliveryCompletedAt BETWEEN :start AND :end")
    BigDecimal sumTotalPriceByEmployeeCreatedByAndStatusAndDeliveryCompletedAtBetween(
            @Param("commercial") Users commercial,
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Somme des totalPrice des commandes LIVREE (toutes) dont deliveryCompletedAt est entre start et end. Dashboard commercial global. */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status AND o.deliveryCompletedAt BETWEEN :start AND :end")
    BigDecimal sumTotalPriceByStatusAndDeliveryCompletedAtBetween(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Ventes par mois (année, mois, montant) pour graphique — commandes LIVREE, deliveryCompletedAt entre debut et fin. Résultat : [(year, month, sum), ...]. */
    @Query("SELECT YEAR(o.deliveryCompletedAt), MONTH(o.deliveryCompletedAt), COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status AND o.deliveryCompletedAt BETWEEN :debut AND :fin GROUP BY YEAR(o.deliveryCompletedAt), MONTH(o.deliveryCompletedAt) ORDER BY YEAR(o.deliveryCompletedAt), MONTH(o.deliveryCompletedAt)")
    List<Object[]> sumVentesParMois(
            @Param("status") OrderStatus status,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /** Commandes par mois (année, mois, count) pour graphique — toutes les commandes (tous statuts), createdAt entre debut et fin. Résultat : [(year, month, count), ...]. */
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :debut AND :fin GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> countCommandesParMois(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /** Nombre de commandes passées par les salariés d'une entreprise (pour détails entreprise partenaire). */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.employee.company = :company")
    long countByEmployeeCompany(@Param("company") com.example.coopachat.entities.Company company);
}
