package com.example.coopachat.repositories;

import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.DriverAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverAvailabilityRepository extends JpaRepository <DriverAvailability , Long> {

    Optional<DriverAvailability> findByDriver(Driver driver);
}
