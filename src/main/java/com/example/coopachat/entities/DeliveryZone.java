package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "delivery_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver; // Un livreur = une disponibilité

    @ElementCollection
    @CollectionTable(
            name = "driver_delivery_zones",
            joinColumns = @JoinColumn(name = "delivery_zone_id")
    )
    @Column(name = "zone_name")
    private Set<String> deliveryZones = new HashSet<>();
}