package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryTour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryTourRepository extends JpaRepository<DeliveryTour, Long > {
}
