package com.example.coopachat.entities;

import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Historique des transitions de statut d'une commande.
 * Permet de tracer: qui a fait quoi, quand, et pourquoi.
 */
@Entity
@Table(
        name = "order_status_history"

)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Commande concernée par le changement de statut. */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Ancien statut (null possible pour un event d'initialisation). */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private OrderStatus fromStatus;

    /** Nouveau statut appliqué. */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private OrderStatus toStatus;

    /** Date/heure du changement. */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /** Utilisateur ayant effectué l'action (null si action système). */
    @ManyToOne
    @JoinColumn(name = "changed_by_user_id")
    private Users changedByUser;

    /** Rôle de l'acteur au moment du changement. */
    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_role", length = 32)
    private UserRole changedByRole;

    /**  nom acteur au moment du changement (évite les écarts si profil modifié ensuite). */
    @Column(name = "actor_first_name", length = 100)
    private String actorFirstName;

    /**  prénom acteur au moment du changement (évite les écarts si profil modifié ensuite). */
    @Column(name = "actor_last_name", length = 100)
    private String actorLastName;

    /** Motif métier (ex: Client absent, Adresse introuvable, etc.). */
    @Column(name = "reason", length = 255)
    private String reason;

    /** Commentaire libre associé à la transition. */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Clé technique d'origine de l'action (ex: DRIVER_REPORT_ISSUE, EMPLOYEE_CANCEL). */
    @Column(name = "source_action", length = 80)
    private String sourceAction;
}

