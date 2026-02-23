package com.example.coopachat.services.coupons;

import com.example.coopachat.entities.Category;
import com.example.coopachat.entities.Coupon;
import com.example.coopachat.entities.Product;
import com.example.coopachat.repositories.CategoryRepository;
import com.example.coopachat.repositories.CouponRepository;
import com.example.coopachat.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nettoyage automatique des coupons expirés.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCleanupService {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Supprime les coupons expirés tous les jours à minuit.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> expiredCoupons = couponRepository.findByEndDateBefore(now);

        if (expiredCoupons.isEmpty()) {
            log.info("Aucun coupon expiré à supprimer");
            return;
        }

        for (Coupon coupon : expiredCoupons) {
            // Délier les produits
            List<Product> linkedProducts = productRepository.findByCouponId(coupon.getId());
            for (Product product : linkedProducts) {
                product.setCoupon(null);
            }
            if (!linkedProducts.isEmpty()) {
                productRepository.saveAll(linkedProducts);
            }

            // Délier les catégories
            List<Category> linkedCategories = categoryRepository.findByCouponId(coupon.getId());
            for (Category category : linkedCategories) {
                category.setCoupon(null);
            }
            if (!linkedCategories.isEmpty()) {
                categoryRepository.saveAll(linkedCategories);
            }
        }

        couponRepository.deleteAll(expiredCoupons);
        log.info("Coupons expirés supprimés: {}", expiredCoupons.size());
    }
}
