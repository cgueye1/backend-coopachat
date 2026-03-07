package com.example.coopachat.config;

import com.example.coopachat.services.auth.JwtService;
import com.example.coopachat.services.auth.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtre JWT pour valider les tokens à chaque requête en relation avec SecurityConfig
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    // ============================================================================
    // 🔍 FILTRAGE DES REQUÊTES
    // ============================================================================

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,// La requête HTTP
            @NonNull HttpServletResponse response,// La réponse HTTP
            @NonNull FilterChain filterChain// La chaîne de filtres
    ) throws ServletException, IOException {

        // Liste des chemins publics qui ne nécessitent pas  de validation JWT
        String requestPath = request.getRequestURI();// Récupérer le chemin de la requête

        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);// Laisser passer sans validation JWT
            return; // on s'arrête là
        }

        //si le chemin n'est pas public, on continue

        // Récupérer le token depuis le header Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Vérifier si le header contient un token Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Token manquant. Veuillez vous connecter.");
            return;
        }

        // si oui , on fait :
        // Extraire le token (enlever "Bearer ")
        jwt = authHeader.substring(7);

        // Vérifier si le token est blacklisté, si oui, on envoie une réponse d'erreur 401 , sinon on continue
        if (tokenBlacklistService.isBlackListed(jwt)) {
            sendUnauthorizedResponse(response, "Token invalide. Veuillez vous reconnecter.");
            return;
        }

        try {
            // Extraire l'email depuis le token
            userEmail = jwtService.extractUsername(jwt);

            // Si l'email est valide
            if (userEmail != null) {

                // Valider le token via jwtService
                if (jwtService.isTokenValid(jwt)) {
                    // Extraire le rôle depuis le token
                    String role = jwtService.extractRole(jwt);

                    // Créer l'authentification Spring Security avec le rôle
                    String authority = "ROLE_" + role;

                    // Création de l'objet Authentication avec le rôle et l'email
                    // Le null c'est pour le mot de passe car on ne l'utilise pas
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );

                    // Créer les détails de l'authentification avec les détails de la requête
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Enregistrement dans le contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Laisser la requête continuer
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    // Token invalide ou expiré
                    logger.warn("JWT token is not valid or expired");
                    sendUnauthorizedResponse(response, "Token invalide ou expiré");
                    return;
                }
            } else {
                // Email non trouvé dans le token
                logger.warn("JWT token does not contain a valid email");
                sendUnauthorizedResponse(response, "Token invalide");
                return;
            }
        } catch (ExpiredJwtException e) {
            // Token expiré - retourner 401
            logger.warn("JWT token expired: " + e.getMessage());
            sendUnauthorizedResponse(response, "Token expiré. Veuillez vous reconnecter.");
            return;
        } catch (JwtException e) {
            // Autre erreur JWT (signature invalide, format incorrect, etc.)
            logger.warn("JWT token validation failed: " + e.getMessage());
            sendUnauthorizedResponse(response, "Token invalide");
            return;
        } catch (Exception e) {
            // Erreur inattendue
            logger.error("Unexpected error during JWT validation: " + e.getMessage(), e);
            sendUnauthorizedResponse(response, "Erreur d'authentification");
        }
    }

    // ============================================================================
    // 🔓 VÉRIFICATION DES ROUTES PUBLIQUES
    // ============================================================================

    /**
     * Vérifie si le chemin de la requête est une route publique
     *
     * @param path Le chemin de la requête
     * @return true si c'est une route publique (il peut accèder à cette fonctionnalité sans token ), false sinon
     */
    private boolean isPublicPath(String path) {
        // Routes d'authentification publiques sauf (/api/auth/logout  continet un token à blacklister si nécessaire)
        if (path.startsWith("/api/auth/") && !path.equals("/api/auth/logout")) {
            return true;
        }

        // Documentation API
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui.html")) {
            return true;
        }

        return false;
    }

    // ============================================================================
    // ❌ GESTION DES ERREURS
    // ============================================================================

    /**
     * Envoie une réponse d'erreur 401 (Unauthorized) au format JSON
     *
     * @param response La réponse HTTP
     * @param message  Le message d'erreur
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Créer un objet JSON avec le message d'erreur
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "UNAUTHORIZED");
        errorResponse.put("message", message);
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("timestamp", System.currentTimeMillis());

        // Convertir en JSON et écrire dans la réponse
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}