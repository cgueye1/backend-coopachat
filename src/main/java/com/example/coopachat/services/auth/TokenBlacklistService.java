package com.example.coopachat.services.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour gérer la blacklist des tokens JWT invalidés
 */
public interface TokenBlacklistService {

    /**
     * Ajoute un token à la blacklist
     * @param token Le token JWT à invalider
     */
     void  addToBlackList(String token);

    /**
     * Vérifie si un token est blacklister
     */
     boolean isBlackListed(String token);

    /**
     * Nettoie les tokens expirés de la blacklist
     */
    void cleanupExpiredTokens();

}