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

// Tâche automatique pour les coupons (panier) et les promotions (produits) :
// expiration quand la date de fin est dépassée, activation quand la période a commencé mais le statut est encore planifié.
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCleanupService {

    // Tout sauf EXPIRED : une fois la date de fin passée, même un coupon désactivé (DISABLED) passe en EXPIRED.
    private static final List<CouponStatus> STATUTS_A_EXPIRER = List.of(
            CouponStatus.ACTIVE, CouponStatus.PLANNED, CouponStatus.DISABLED);

    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;

    // Cron : chaque heure à la minute 0. Ordre : d’abord expiration, puis activation des offres planifiées dont la date de début est atteinte.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOutdatedCouponsAndPromotions() {
        LocalDateTime now = LocalDateTime.now();
        int couponsExpired = expireCoupons(now); //Expiration des coupons
        int promotionsExpired = expirePromotions(now); //Expiration des promotions

        //Si des coupons ou des promotions ont expiré, on log l'expiration
        if (couponsExpired > 0 || promotionsExpired > 0) {
            log.info("Expiration automatique : {} coupon(s), {} promotion(s) passés en EXPIRED",
                    couponsExpired, promotionsExpired);
        }
        int couponsActivated = activatePlannedCoupons(now); //Activation des coupons planifiés
        int promotionsActivated = activatePlannedPromotions(now); //Activation des promotions planifiés

        //Si des coupons ou des promotions ont été activés, on log l'activation
        if (couponsActivated > 0 || promotionsActivated > 0) {
            log.info("Activation automatique : {} coupon(s), {} promotion(s) passés en ACTIVE",
                    couponsActivated, promotionsActivated);
        }
    }

    // Coupons : date de fin avant maintenant, statut encore ACTIVE, PLANNED ou DISABLED → EXPIRED, isActive à false.
    private int expireCoupons(LocalDateTime now) {
        // Coupons dont la fin est dépassée 
        List<Coupon> list = couponRepository.findByEndDateBeforeAndStatusIn(now, STATUTS_A_EXPIRER);
        //On met le statut des coupons en EXPIRED et on désactive leur activation
        for (Coupon c : list) {
            c.setStatus(CouponStatus.EXPIRED);
            c.setIsActive(false);
        }
        //si la liste n'est pas vide, on sauvegarde les coupons
        if (!list.isEmpty()) {
            couponRepository.saveAll(list); //Sauvegarde des coupons
        }
        return list.size(); //Retourne le nombre de coupons expirés
    }

    // Promotions : même règle que pour les coupons.
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

    // Coupons encore PLANNED alors qu’on est entre la date de début et la date de fin → ACTIVE, isActive à true.
    private int activatePlannedCoupons(LocalDateTime now) {
        //On récupère les coupons dont la date de début est atteinte et la date de fin pas encore atteinte
        List<Coupon> list = couponRepository.findPlannedCouponsToAutoActivate(now);
        //On met le statut des coupons en ACTIVE et on active leur activation
        for (Coupon c : list) {
            c.setIsActive(true);
            c.setStatus(CouponStatus.ACTIVE);
        }
        //si la liste n'est pas vide, on sauvegarde les coupons
        if (!list.isEmpty()) {
            couponRepository.saveAll(list);
        }
        return list.size();
    }

    // Promotions planifiées : même logique que les coupons.
    private int activatePlannedPromotions(LocalDateTime now) {
        List<Promotion> list = promotionRepository.findPlannedPromotionsToAutoActivate(now);
        for (Promotion p : list) {
            p.setIsActive(true);
            p.setStatus(CouponStatus.ACTIVE);
        }
        if (!list.isEmpty()) {
            promotionRepository.saveAll(list);
        }
        return list.size();
    }
}
