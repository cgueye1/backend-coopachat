package com.example.coopachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ForwardedHeaderFilter;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })//Pour ne pas avoir le mot de passe de jwt en claire dans la console
@EnableScheduling // permet d'activer le scheduling (planification des tâches)
public class CoopachatApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoopachatApplication.class, args);
		System.out.println("✨Coopachat Application Started✨");
	}

	/**
	 * Filtre global pour intercepter les en-tetes du reverse proxy (ex: Nginx).
	 * Permet a Spring et Swagger de savoir que l'application tourne en HTTPS
	 * et d'eviter les erreurs de "Mixed Content" (requetes HTTP bloquees).
	 */
	@Bean
	public ForwardedHeaderFilter forwardedHeaderFilter() {
		return new ForwardedHeaderFilter();
	}

}