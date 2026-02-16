package com.example.coopachat.services.auth;

import com.example.coopachat.dtos.UserDto;
import com.example.coopachat.dtos.auth.LoginResponseDTO;
import com.example.coopachat.exceptions.EmailAlreadyExistsException;
import com.example.coopachat.exceptions.PhoneAlreadyExistsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation du service JWT
 * Gère la génération, la validation et l'extraction des tokens JWT
 */
@Service
public class JwtServiceImpl implements JwtService {

    // ============================================================================
    // 🔐 CONFIGURATION
    // ============================================================================

    // Clé secrète pour signer les tokens, chargée depuis la configuration
    @Value("${jwt.secret:coopachat-secret-key-256-bits-minimum-required-for-security-2025}")
    private String secretKey;

    // Expiration du token (24 heures par défaut)
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    // ============================================================================
    // 🔑 GÉNÉRATION DE CLÉS
    // ============================================================================

    /**
     * Génère la clé de signature cryptographique
     * Convertit la chaîne secrète en clé HMAC-SHA compatible
     * SecretKey {
     *     algorithm: "HmacSHA256",
     *     format: "RAW",
     *     encoded: byte[32] // 32 octets = 256 bits
     * }
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes()); // Keys.hmacShaKeyFor() s'assure que la clé fait exactement 256 bits
    }

    // ============================================================================
    // 🎫 GÉNÉRATION DE TOKENS
    // ============================================================================

    @Override
    public String generateToken(String email, String role, Long id) {
        // Création des claims personnalisées (données supplémentaires)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);    // Rôle de l'utilisateur
        claims.put("id", id);        // ID unique en base de données

        return createToken(claims, email);
    }

    /**
     * Construit le token JWT avec toutes ses composantes
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)                  // Données personnalisées
                .setSubject(subject)                // Identifiant principal (email)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + expiration))        // Date d'expiration calculée
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signature géré par .signWith en utilisant La clé sécréte et les autres valeurs (claims)
                .compact();                         // Génération finale
    }


    // ============================================================================
    // 🔍 EXTRACTION DE CLAIMS
    // ============================================================================

    /**
     * Extrait toutes les claims du token et vérifie la signature
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())     // Clé de vérification
                .build()
                .parseClaimsJws(token)              // Analyse et validation
                .getBody();                         // Retourne le payload
    }

    @Override
    public String extractEmail(String token) {
        // Extrait directement l'email (subject) depuis toutes les claims
        final Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    @Override
    public String extractRole(String token) {
        // Extrait directement le rôle depuis toutes les claims
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    @Override
    public Long extractUserId(String token) {
        // Extrait directement l'ID utilisateur depuis toutes les claims
        final Claims claims = extractAllClaims(token);
        return claims.get("id", Long.class);
    }


    @Override
    public String extractUsername(String token) {
        // Alias pour extractEmail - compatible avec Spring Security
        return extractEmail(token);
    }

    //Extrait la date d'expiration
    @Override
    public Date extractExpiration(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    // ============================================================================
    // ✅ VALIDATION DE TOKENS
    // ============================================================================

    @Override
    public Boolean isTokenExpired(String token) {
        // Vérifie si la date d'expiration est passée
        final Claims claims = extractAllClaims(token);
        return claims.getExpiration().before(new Date());
    }

    @Override
    public Boolean validateToken(String token, String email) {
        // Validation complète : correspondance email + non expiration
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }

    @Override
    public Boolean isTokenValid(String token) {
        try {
            // Vérifier que le token n'est pas vide et est bien structuré
            //token.split("\\.") : découpe le token par . , Si ce n'est pas 3 parties → invalide
            if (token == null || token.split("\\.").length != 3) {
                return false;
            }
            return !isTokenExpired(token); //si le token est expiré (true) alors on retourne (false) ,le token n'est pas valide ,sinon on retourne (true).
        } catch (Exception e) {
            return false;
        }
    }

}