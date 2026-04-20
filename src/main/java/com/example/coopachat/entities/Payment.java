package com.example.coopachat.entities;

import com.example.coopachat.enums.MobileOperator;
import com.example.coopachat.enums.PaymentMethodType;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.PaymentTimingType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Commande associée (1-to-1)
     */
    @OneToOne
    @JoinColumn(name = "order_id",nullable = false, unique = true)
    private Order order ;

    /**
     * Mode de paiement (ENUM). Null tant que le client n'a pas choisi (paiement créé à la commande, statut Impayé).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = true, columnDefinition = "VARCHAR(50) NULL")
    private PaymentMethodType paymentMethod;

    /**
     * Moment du paiement (ENUM). Null tant que non défini (rempli au moment du paiement).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_timing", nullable = true, columnDefinition = "VARCHAR(50) NULL")
    private PaymentTimingType paymentTiming;


    /**
     * Opérateur Mobile Money (ENUM, null si carte/espèces)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_operator")
    private MobileOperator mobileOperator;

    /**
     * Statut du paiement
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.UNPAID;

    /**
     * Référence de transaction (simulée)
     */
    @Column(name = "transaction_reference", length = 50)
    private String transactionReference;


    /**
     * Date et heure du paiement
     */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //l’ID InTouch
    @Column(name = "gu_transaction_id")
    private String guTransactionId;

    //le message que InTouch envoie pour expliquer le résultat du paiement
    @Column(name = "provider_message", columnDefinition = "TEXT")
    private String providerMessage;

}
