package com.example.coopachat.entities;

import com.example.coopachat.enums.DeliveryIssueReportSource;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Signalement d'un problème de livraison (livreur ou salarié).
 * Permet de notifier rapidement qu'il y a un souci sur une commande.
 */
@Entity
@Table(name = "delivery_issue_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryIssueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private Users reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_source", nullable = false)
    private DeliveryIssueReportSource reportSource;

    /** Libellé de la raison (livreur : DeliveryIssueReason, salarié : EmployeeDeliveryIssueReason). */
    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
