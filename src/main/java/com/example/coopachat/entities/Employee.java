package com.example.coopachat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "employees")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeCode; // Code unique du salarié

    @Column(nullable = false)
    @NotBlank(message = "L'adresse est obligatoire")
    private String address; // Adresse du salarié

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> deliveryPreferences; // Préférences de livraison


    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt; // Date d'inscription

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    private LocalDateTime updatedAt; // Date de modification

    // Relations
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // L'utilisateur salarié

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // L'entreprise associée

    @ManyToOne
    @JoinColumn(name = "enrolled_by_commercial_id", nullable = false)
    private User enrolledBy; // Commercial qui a inscrit le salarié
}