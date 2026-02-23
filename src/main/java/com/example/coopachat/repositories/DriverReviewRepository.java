package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverReview;
import com.example.coopachat.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverReviewRepository extends JpaRepository<DriverReview, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<DriverReview> findByOrder(Order order);

    /**
     * Calcule la moyenne des notes (1 à 5) pour un livreur.
     * La base fait : somme des {rating} de tous les avis (DriverReview) de ce livreur,divisée par le nombre d'avis.
     * @param driverId id du livreur
     * @return la moyenne ou {null} si le livreur n'a encore aucun avis
     */
    @Query("SELECT AVG(dr.rating) FROM DriverReview dr WHERE dr.driver.id = :driverId")
    Double getAverageRatingByDriverId(@Param("driverId") Long driverId);
}
