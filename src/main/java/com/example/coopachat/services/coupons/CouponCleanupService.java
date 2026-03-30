package com.example.coopachat.services.coupons;

import com.example.coopachat.entities.Coupon;
import com.example.coopachat.entities.Promotion;
import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.repositories.CouponRepository;
import com.example.coopachat.repositories.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service planifié : bascule automatiquement le statut en {@link CouponStatus#EXPIRED} lorsque la date de fin
 * ({@code endDate}) est dépassée, pour les <strong>coupons panier</strong> et les <strong>promotions produits</strong>.
 * <p>
 * <strong>Pourquoi ce service existe :</strong> côté employé, les offres ne sont montrées que si la période
 * est encore valide (requêtes filtrées sur les dates). Côté commercial, la liste affichait souvent encore
 * « Actif » tant que personne ne changeait le statut en base — d’où une incohérence. Ici on met la base
 * à jour pour que l’interface commercial reflète « Expiré » comme la réalité métier.
 * <p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCleanupService {

    /**
     * Seuls ACTIVE et PLANNED peuvent « devenir » expirés automatiquement.
     * On ne touche pas à DISABLED (désactivation manuelle) ni à EXPIRED (déjà traité).
     */
    private static final List<CouponStatus> STATUTS_A_EXPIRER = List.of(CouponStatus.ACTIVE, CouponStatus.PLANNED);

    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;

    /**
     * Cron : toutes les heures à la minute 0 (ex. 10:00, 11:00…).
     * <p>
     * Fréquence volontairement plus élevée qu’un passage unique à minuit : ainsi le tableau commercial
     * se met à jour le jour même de l’expiration, sans attendre le lendemain matin.
     * <p>
     * {@link Transactional} : une seule transaction pour coupons + promotions ; en cas d’erreur, tout est annulé.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOutdatedCouponsAndPromotions() {
        // Heure serveur ( JVM ). À garder aligné avec le stockage des dates en base (même fuseau / convention).
        LocalDateTime now = LocalDateTime.now();
        int coupons = expireCoupons(now);
        int promotions = expirePromotions(now);
        if (coupons > 0 || promotions > 0) {
            log.info("Expiration automatique : {} coupon(s), {} promotion(s) passés en EXPIRED", coupons, promotions);
        }
    }

    /**
     * Coupons : {@code endDate &lt; now} et statut encore ACTIVE ou PLANNED → EXPIRED + {@code isActive = false}.
     * <p>
     * {@code isActive = false} évite qu’ils réapparaissent dans les écrans qui filtrent sur le flag actif,
     * en cohérence avec le statut EXPIRED.
     */
    private int expireCoupons(LocalDateTime now) {
        List<Coupon> list = couponRepository.findByEndDateBeforeAndStatusIn(now, STATUTS_A_EXPIRER);
        for (Coupon c : list) {
            c.setStatus(CouponStatus.EXPIRED);
            c.setIsActive(false);
        }
        if (!list.isEmpty()) {
            couponRepository.saveAll(list);
        }
        return list.size();
    }

    /**
     * Promotions (réductions sur produits / catalogue) : même règle que pour les coupons.
     * Entité distincte {@link Promotion}, même énumération {@link CouponStatus} pour homogénéité.
     */
    private int expirePromotions(LocalDateTime now) {
        List<Promotion> list = promotionRepository.findByEndDateBeforeAndStatusIn(now, STATUTS_A_EXPIRER);
        for (Promotion p : list) {
            p.setStatus(CouponStatus.EXPIRED);
            p.setIsActive(false);
        }
        if (!list.isEmpty()) {
            promotionRepository.saveAll(list);
        }
        return list.size();
    }
}
