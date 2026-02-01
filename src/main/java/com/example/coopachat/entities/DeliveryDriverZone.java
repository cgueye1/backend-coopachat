package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

/**
 *  Les zones du livreur
 */
@Entity
@Table(name = "delivery_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDriverZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver; // Un livreur = ses zones de livraison


    // relation directe
    @ManyToMany
    @JoinTable(
            name = "driver_zones",                    // Nom table
            joinColumns = @JoinColumn(name = "driver_id"),        // Colonne Driver
            inverseJoinColumns = @JoinColumn(name = "zone_id")    // Colonne Zone
    )
    private Set<DeliveryZoneReference> zones = new HashSet<>();// les Zones qu'il couvre

}