package com.example.coopachat.services.user;

import com.example.coopachat.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Génère une référence utilisateur unique et peu prévisible (même principe que {@code CMD-} pour les commandes).
 * Format : {@code US-} + 8 caractères hexadécimaux en majuscules.
 */
@Component
@RequiredArgsConstructor
public class UserReferenceGenerator {

    private final UserRepository userRepository;

    public String generateUniqueRefUser() {
        final int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String candidate = "US-" + suffix;
            if (!userRepository.existsByRefUser(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Impossible de générer une référence utilisateur unique");
    }
}
