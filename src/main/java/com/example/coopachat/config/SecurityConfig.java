package com.example.coopachat.config;

import com.example.coopachat.dtos.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * SecurityConfig configure la sécurité de l'API en définissant les règles d'accès (endpoints publics/protégés), en activant JWT, en configurant CORS et en fournissant le PasswordEncoder pour hasher les mots de passe.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                        // 🔒 PROFIL UTILISATEUR CONNECTÉ (avant permitAll /api/auth/**)
                        // ==================================
                        // PUT /api/auth/me* : profil commercial / RL / Admin (JWT + rôle)
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me").hasAnyRole("COMMERCIAL", "LOGISTICS_MANAGER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me/profile-photo").hasAnyRole("COMMERCIAL", "LOGISTICS_MANAGER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/me/profile-photo").hasAnyRole("COMMERCIAL", "LOGISTICS_MANAGER", "ADMINISTRATOR")

                        // GET /api/auth/me : « Mon compte » — JWT obligatoire, tous les rôles (géré par AuthController + AuthService)
                        .requestMatchers("/api/auth/me").authenticated()

                        // ==================================
                        // 🟢 ZONES PUBLIQUES (sans connexion)
                        // ==================================
                        // Ne couvre pas /api/auth/me (règle plus spécifique ci-dessus)
                        .requestMatchers("/api/auth/**").permitAll()                   // Inscription + Connexion + OTP…
                        .requestMatchers("/api/payments/intouch/callback").permitAll() // Callback provider de paiement (InTouch)
                        .requestMatchers("/api/payments/bridge/**").permitAll()        // Bridge TouchPay (Public pour TEST)
                        .requestMatchers("/touchpay-bridge.html").permitAll()
                        .requestMatchers("/api/files/**").permitAll()                  // Images / fichiers (img src ne peut pas envoyer le token)
                        .requestMatchers("/swagger-ui/**").permitAll()                  // Documentation API
                        .requestMatchers("/v3/api-docs/**").permitAll()                 // Documentation API
                        .requestMatchers("/swagger-ui.html").permitAll()                // Documentation API

                        // ==================================
                        // 🔴 ZONES ADMIN UNIQUEMENT
                        // ==================================
                        .requestMatchers(HttpMethod.GET, "/api/admin/categories").hasAnyRole("ADMINISTRATOR", "LOGISTICS_MANAGER")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")

                        // ==================================
                        // 🟡 ZONES AVEC RÔLES SPÉCIFIQUES (+ ADMIN)
                        // ==================================
                        // Commercial
                        .requestMatchers("/api/commercial/**").hasRole("COMMERCIAL")

                        // Responsable Logistique
                        .requestMatchers("/api/logistics/**").hasRole("LOGISTICS_MANAGER")

                        // Livreur
                        .requestMatchers("/api/deliveries/**", "/api/driver/**").hasRole("DELIVERY_DRIVER")

                        // Salarié
                        .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")

                        // ==================================
                        // 🔴 TOUT LE RESTE
                        // ==================================
                        .anyRequest().authenticated()  // Toutes autres URLs nécessitent connexion
                )

                // GESTION DES ERREURS D'AUTHENTIFICATION ET D'AUTORISATION (401 et 403)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentification requise"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, HttpStatus.FORBIDDEN, "Accès refusé : permissions insuffisantes"))
                )

                /**
                 * Ajouter le filtre JWT dans la chaîne avant le filtre d'authentification par défaut de spring security pour que le token JWT soit validé en premier.
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ErrorResponseDTO error = new ErrorResponseDTO(message, null, LocalDateTime.now(), status.value());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().findAndRegisterModules().writeValue(response.getWriter(), error);
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

        // Origines autorisées
        configuration.setAllowedOrigins(Arrays.asList(
                "https://coopachat.innovimpactdev.cloud",
                "https://api.coopachat.innovimpactdev.cloud",
                "http://localhost:8080",
                "http://localhost:4200"
        ));

        // Autorise ces méthodes HTTP (OPTIONS requis pour le preflight CORS)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Autorise tous les headers (dont Authorization pour le Bearer token)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Credentials (cookies) : true (vérifie les badges)
        configuration.setAllowCredentials(true);

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