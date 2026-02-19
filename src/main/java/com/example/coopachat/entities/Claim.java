package com.example.coopachat.entities;

import com.example.coopachat.enums.ClaimProblemType;
import com.example.coopachat.enums.ClaimStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Réclamation soumise par un salarié sur une commande (un ou plusieurs produits concernés).
 */
@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Produits concernés **/
    @ManyToMany
    @JoinTable(
            name = "claim_order_items",//Nom de la table de jointure
            joinColumns = @JoinColumn(name = "claim_id"),//Nom de la colonne de la table de jointure qui référence l'id de la réclamation
            inverseJoinColumns = @JoinColumn(name = "order_item_id")//Nom de la colonne de la table de jointure qui pointe vers l'id de l'article
    )
    private List<OrderItem> orderItems = new ArrayList<>();//Liste des articles concernés par la réclamation

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_type", nullable = false)
    private ClaimProblemType problemType;//Nature du problème

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;//Commentaire de la réclamation

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClaimStatus status = ClaimStatus.EN_ATTENTE;//Statut de la réclamation

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;



}
