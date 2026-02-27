package com.example.coopachat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Au démarrage, corrige la colonne addresses.id en AUTO_INCREMENT si besoin
 * (base existante sans Flyway ou migration non appliquée). Une fois appliqué, les prochains
 * démarrages ne font rien.
 */
@Component
@Order(Integer.MAX_VALUE) // après Flyway et le reste
@Slf4j
public class AddressesIdAutoIncrementFix implements ApplicationRunner {

    private final DataSource dataSource;

    public AddressesIdAutoIncrementFix(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT EXTRA FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses' AND COLUMN_NAME = 'id'")) {
                if (!rs.next()) {
                    return; // table absente, rien à faire
                }
                String extra = rs.getString("EXTRA");
                if (extra != null && extra.toLowerCase().contains("auto_increment")) {
                    return; // déjà OK
                }
            }

            stmt.executeUpdate("ALTER TABLE addresses MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
            log.info("Correctif appliqué: addresses.id est maintenant AUTO_INCREMENT.");

        } catch (Exception e) {
            log.debug("Correctif addresses.id non appliqué (table absente ou déjà OK): {}", e.getMessage());
        }
    }
}
