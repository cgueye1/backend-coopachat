package com.example.coopachat.entities;

import com.example.coopachat.enums.ClaimDecisionType;
import com.example.coopachat.enums.ClaimProblemType;
import com.example.coopachat.enums.ClaimStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
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

    // ⭐ UN SEUL PRODUIT
    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_type", nullable = false)
    private ClaimProblemType problemType;//Nature du problème

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;//Commentaire de la réclamation

    // Photos (optionnel)
    private List<String> photoUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClaimStatus status = ClaimStatus.EN_ATTENTE;//Statut de la réclamation

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Type de décision à la validation : réintégration au stock ou remboursement. */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type")
    private ClaimDecisionType decisionType;

    /** Montant remboursé (rempli lorsque decisionType = REMBOURSEMENT). */
    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    /** Motif du rejet (rempli lorsque status = REJETE). */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

}
