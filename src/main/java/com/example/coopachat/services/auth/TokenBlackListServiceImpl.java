package com.example.coopachat.services.auth;

import com.example.coopachat.entities.auth.TokenBlacklist;
import com.example.coopachat.repositories.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@RequiredArgsConstructor
@Service
@Slf4j
public class TokenBlackListServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    /**
     * Ajoute un token à la blacklist
     */
    public void addToBlackList(String token) {
        try {

            // Vérifier si le token n'est pas déjà dans la blacklist
            if (tokenBlacklistRepository.findByToken(token).isPresent()) {
                log.warn("Token déjà présent dans la blacklist");
                return; // on s'arrête là
            }
            // sinon, Extraire la date d'expiration du token
            Date expirationDate = jwtService.extractExpiration(token);

            //la convertir en LocalDateTime avant sauvegarde à la base
            LocalDateTime expiresAt = LocalDateTime.ofInstant(expirationDate.toInstant(), java.time.ZoneId.systemDefault());

            // Créer et sauvegarder l'entrée dans la blacklist
            TokenBlacklist blacklistedToken = new TokenBlacklist();
            blacklistedToken.setToken(token);
            blacklistedToken.setExpiresAt(expiresAt);
            tokenBlacklistRepository.save(blacklistedToken);

            log.info("Token ajouté à la blacklist");

        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du token à la blacklist: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la déconnexion", e);
        }
    }

    /**
     * Vérifie si un token est blacklister
     */
    @Override
    public boolean isBlackListed(String token){
        return tokenBlacklistRepository.findByToken(token).isPresent();
    }


    /**
     * Nettoie les tokens expirés de la blacklist (exécuté quotidiennement à minuit)
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    @Override
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenBlacklistRepository.deleteExpiredTokens(now);
        log.info("Nettoyage des tokens expirés de la blacklist effectué");
    }

}

