package com.example.coopachat.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table (name = "drivers")
public class Driver {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    // Le compte du livreur
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    //L'utilisateur qui a créé le livreur (ADMIN / LOGISTICS_MANAGER)
    @ManyToOne
    @JoinColumn(name ="created_by" , nullable = false)
    private Users createdBy;

    @OneToOne(mappedBy = "driver", fetch = FetchType.LAZY)
    private DeliveryDriverZone deliveryDriverZone;//les zones qu'ils couvrent

    @OneToOne(mappedBy = "driver", fetch = FetchType.LAZY)
    private DriverAvailability availability;//ses disponibilités
}
