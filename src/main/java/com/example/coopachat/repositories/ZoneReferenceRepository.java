package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryZoneReference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//ZONES REFERENCES CONFIGUREES PAR ADMIN
@Repository
public interface ZoneReferenceRepository extends JpaRepository <DeliveryZoneReference , Long> {

    boolean existsByZoneName( String zoneName);
}
