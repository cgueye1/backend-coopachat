package com.example.coopachat.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nature du problème pour une réclamation (géré par l'admin : nom + description).
 */
@Entity
@Table(name = "claim_problem_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimProblemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
