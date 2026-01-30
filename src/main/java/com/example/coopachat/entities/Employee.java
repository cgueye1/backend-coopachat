
package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity représentant un salarié d'une entreprise
 */
@Entity
@Table(name = "employees")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String employeeCode; // Code unique du salarié 

    // Relation avec l'entreprise
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Relation avec Users (pour l'authentification)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    // Adresse de livraison du salarié
    @OneToMany(mappedBy = "employee")
    private List<Address> addresses = new ArrayList<>();

    // Commercial qui a créé ce salarié
    @ManyToOne
    @JoinColumn(name = "commercial_id", nullable = false)
    private Users createdBy;

    // Un Employé peut avoir PLUSIEURS items dans son panier
    @OneToMany(mappedBy = "employee")
    private List<CartItem> cartItems = new ArrayList<>();

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date de création

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

}
