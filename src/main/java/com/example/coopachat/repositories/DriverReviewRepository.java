package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverAvis;
import com.example.coopachat.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverReviewRepository extends JpaRepository<DriverAvis, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<DriverAvis> findByOrder(Order order);

    /**
     * Calcule la moyenne des notes (1 à 5) pour un livreur.
     * @param driverId id du livreur
     * @return la moyenne ou null si le livreur n'a encore aucun avis
     */
    @Query(value = "SELECT AVG(rating) FROM driver_reviews WHERE driver_id = :driverId", nativeQuery = true)
    Double getAverageRatingByDriverId(@Param("driverId") Long driverId);
}
