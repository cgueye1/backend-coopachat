package com.example.coopachat.repositories;

import com.example.coopachat.entities.DeliveryZoneReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//ZONE REFERENCES CONFIGURES PAR ADMIN
@Repository
public interface ZoneReferenceRepository extends JpaRepository <DeliveryZoneReference , Long> {

}
