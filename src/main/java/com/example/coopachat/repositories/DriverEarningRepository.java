package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DriverEarningRepository extends JpaRepository<DriverEarning, Long> {

    /** Somme des gains du livreur entre start et end (ex. aujourd'hui). */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM DriverEarning e WHERE e.driver.id = :driverId AND e.earnedAt BETWEEN :start AND :end")
    BigDecimal sumAmountByDriverIdAndEarnedAtBetween(
            @Param("driverId") Long driverId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Historique des gains du livreur, trié du plus récent au plus ancien. */
    List<DriverEarning> findByDriverIdOrderByEarnedAtDesc(Long driverId, org.springframework.data.domain.Pageable pageable);

    /** Compter les gains du livreur par période (pour performances). */
    @Query("SELECT COUNT(e) FROM DriverEarning e WHERE e.driver.id = :driverId AND e.earnedAt BETWEEN :start AND :end")
    long countByDriverIdAndEarnedAtBetween(
            @Param("driverId") Long driverId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
