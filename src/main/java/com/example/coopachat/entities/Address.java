package com.example.coopachat.entities;

import com.example.coopachat.enums.DeliveryMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

//Adresse de livraison du salarié
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // le salarié concerné

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode; // "Domicile", "Bureau", "Autre"
    private String city; //ville
    private String district; // quartier
    private String street; // rue
    private boolean isPrimary;// utiliser ou pas comme adresse principale


    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}