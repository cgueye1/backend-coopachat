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
     * Mode de paiement (ENUM)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethodType paymentMethod;

    /**
     * Moment du paiement (ENUM)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_timing", nullable = false)
    private PaymentTimingType paymentTiming = PaymentTimingType.ONLINE;


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

}
