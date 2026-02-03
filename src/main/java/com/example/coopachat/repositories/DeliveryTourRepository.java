package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryTour;
import com.example.coopachat.enums.DeliveryTourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryTourRepository extends JpaRepository<DeliveryTour, Long > {

    /**
     * Recherche paginée des tournées avec filtres
     * - Filtre par numéro de tournée (optionnel)
     * - Filtre par statut (optionnel)
     *
     * @param tourNumber  Numéro de tournée à rechercher (optionnel)
     * @param status      Statut de la tournée (optionnel)
     * @return Tournées paginées triées par date de livraison décroissante puis par date de création décroissante
     */
    @Query("SELECT dt FROM DeliveryTour dt " +
            "WHERE (:tourNumber IS NULL OR " +
            "       LOWER(dt.tourNumber) LIKE LOWER(CONCAT('%', :tourNumber, '%'))) " +
            "AND (:status IS NULL OR dt.status = :status) " +
            "ORDER BY dt.deliveryDate DESC, dt.createdAt DESC")
    Page<DeliveryTour> findDeliveryTourWithFilters(
            @Param("tourNumber") String tourNumber,
            @Param("status") DeliveryTourStatus status,
            Pageable pageable);

    /**
     * Compte le nombre de tournées par statut
     * @param status Statut des tournées à compter
     * @return Nombre de tournées ayant ce statut
     */
    long countByStatus(DeliveryTourStatus status);

}
