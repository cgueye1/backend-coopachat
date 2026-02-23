package com.example.coopachat.entities;

import com.example.coopachat.enums.DriverReportType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Signalement soumis par un livreur (formulaire "Signaler un problème").
 * Toujours lié à une ligne de commande (orderItem) affichée lors du swipe.
 */
@Entity
@Table(name = "driver_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private DriverReportType reportType;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Ligne de commande concernée. */
    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
