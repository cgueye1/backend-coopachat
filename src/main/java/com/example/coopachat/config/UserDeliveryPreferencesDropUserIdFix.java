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
 * Au démarrage, supprime la colonne user_id de user_delivery_preferences si elle existe
 * (legacy : l'entité n'utilise que employee_id). Évite "Field 'user_id' doesn't have a default value" à l'INSERT.
 */
@Component
@Order(Integer.MAX_VALUE - 1) // après Flyway, avant les autres fix
@Slf4j
public class UserDeliveryPreferencesDropUserIdFix implements ApplicationRunner {

    private final DataSource dataSource;

    public UserDeliveryPreferencesDropUserIdFix(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_delivery_preferences' AND COLUMN_NAME = 'user_id'")) {
                if (!rs.next()) {
                    return; // colonne absente, rien à faire
                }
            }

            stmt.executeUpdate("ALTER TABLE user_delivery_preferences DROP COLUMN user_id");
            log.info("Correctif appliqué: colonne user_id supprimée de user_delivery_preferences.");

        } catch (Exception e) {
            log.debug("Correctif user_delivery_preferences.user_id non appliqué: {}", e.getMessage());
        }
    }
}
