package com.example.coopachat.entities.auth;

import com.example.coopachat.enums.CodeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activation_codes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;  // Email de l'utilisateur

    @Column(nullable = false, length = 255)//255 pour UUID
    private String code;   // Code à 6 chiffres

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // Expiration

    @Column(nullable = false)
    private Boolean used = false;  // Code déjà utilisé ?

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CodeType type;// Type de code

    @CreationTimestamp
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;


}