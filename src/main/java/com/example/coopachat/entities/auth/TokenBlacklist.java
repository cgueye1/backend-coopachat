package com.example.coopachat.entities.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;  // Le token JWT complet

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // Date d'expiration du token

    @CreationTimestamp
    private LocalDateTime createdAt;  // Date d'ajout à la blacklist
}
