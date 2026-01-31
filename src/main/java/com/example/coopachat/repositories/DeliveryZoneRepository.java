package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryZone;
import com.example.coopachat.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryZoneRepository extends JpaRepository <DeliveryZone, Long> {

     Optional <DeliveryZone> findByDriver(Driver driver);
}
