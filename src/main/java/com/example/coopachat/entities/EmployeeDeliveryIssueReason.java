package com.example.coopachat.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Raison d'un signalement de problème par le salarié (client) (géré par l'admin : nom + description).
 */
@Entity
@Table(name = "employee_delivery_issue_reasons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDeliveryIssueReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
