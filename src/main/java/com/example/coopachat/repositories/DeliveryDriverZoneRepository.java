package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryDriverZone;
import com.example.coopachat.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//ZONES CHOISIES PAR LE DRIVER
@Repository
public interface DeliveryDriverZoneRepository extends JpaRepository <DeliveryDriverZone, Long> {

     Optional <DeliveryDriverZone> findByDriver(Driver driver);

     boolean existsByDriver(Driver driver);
}
