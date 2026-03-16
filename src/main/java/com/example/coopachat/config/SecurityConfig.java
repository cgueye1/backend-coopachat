package com.example.coopachat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * SecurityConfig configure la sécurité de l'API en définissant les règles d'accès (endpoints publics/protégés), en activant JWT, en configurant CORS et en fournissant le PasswordEncoder pour hasher les mots de passe.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================================================
    // 🔐 CONFIGURATION
    // ============================================================================

    /**
     * Crée l'outil pour crypter les mots de passe
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuration principale de la sécurité
     * Définit qui peut accéder à quoi
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Autorise les appels depuis d'autres sites (frontend)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Désactive la protection CSRF (pas besoin pour API REST, il protège spécifiquement les attaques via les cookies)
                .csrf(csrf -> csrf.disable())

                // Pas de sessions - on utilise JWT à chaque requête
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CONFIGURATION DES AUTORISATIONS
                .authorizeHttpRequests(auth -> auth
                        // ==================================
                        // 🔒 PROFIL UTILISATEUR CONNECTÉ
                        // ==================================
                        .requestMatchers("/api/auth/me").authenticated()               // Profil courant (tous rôles)

                        // ==================================
                        // 🟢 ZONES PUBLIQUES (sans connexion)
                        // ==================================
                        .requestMatchers("/api/auth/**").permitAll()                   // Inscription + Connexion
                        .requestMatchers("/files/**").permitAll()                      // Images / fichiers (img src ne peut pas envoyer le token)
                        .requestMatchers("/swagger-ui/**").permitAll()                  // Documentation API
                        .requestMatchers("/v3/api-docs/**").permitAll()                 // Documentation API
                        .requestMatchers("/swagger-ui.html").permitAll()                // Documentation API

                        // ==================================
                        // 🔴 ZONES ADMIN UNIQUEMENT
                        // ==================================
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")

                        // ==================================
                        // 🟡 ZONES AVEC RÔLES SPÉCIFIQUES
                        // ==================================
                        // Commercial + Admin
                        .requestMatchers("/api/companies/**").hasAnyRole("COMMERCIAL", "ADMINISTRATOR")
                        .requestMatchers("/api/employees/**").hasAnyRole("COMMERCIAL", "ADMINISTRATOR")

                        // Responsable Logistique + Admin
                        .requestMatchers("/api/logistics/**").hasAnyRole("LOGISTICS_MANAGER", "ADMINISTRATOR")

                        // Livreur + Admin
                        .requestMatchers("/api/deliveries/**", "/api/driver/**").hasAnyRole("DELIVERY_DRIVER", "ADMINISTRATOR")

                        // ==================================
                        // 🔴 TOUT LE RESTE
                        // ==================================
                        .anyRequest().authenticated()  // Toutes autres URLs nécessitent connexion
                )

                /**
                 * Ajouter le filtre JWT dans la chaîne avant le filtre d'authentification par défaut de spring security  pour que le token JWT soit validé en premier.
                 * [mon filtre JWT] → Traite le token → Authentifie → [filtre login(UsernamePasswordAuthenticationFilter)] → Rien à faire
                 * [mon filtre JWT] → Rien à faire → [filtre login] → Traite si c'est /login
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ============================================================================
    // 🌐 CONFIGURATION CORS
    // ============================================================================
    /**
     * Configuration CORS - Autorise les appels depuis le frontend(sans cette configuration, le frontend ne peut pas envoyer les requêtes à l'API).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées : * + localhost (front en dev ou Docker sur 8080)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "*",
                "http://localhost:8080",
                "http://localhost:4200"
        ));

        // Autorise ces méthodes HTTP (OPTIONS requis pour le preflight CORS)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Autorise tous les headers (dont Authorization pour le Bearer token)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Credentials (cookies) : false
        configuration.setAllowCredentials(false);

        // Cache du preflight (OPTIONS) en secondes
        configuration.setMaxAge(3600L);

        // Crée la "boîte à configuration" CORS
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Applique ces règles CORS à TOUTES les URLs de l'API
        source.registerCorsConfiguration("/**", configuration);

        // Donne la configuration à Spring Security
        return source;
    }
}