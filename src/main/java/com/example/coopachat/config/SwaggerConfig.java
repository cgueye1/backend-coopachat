package com.example.coopachat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration Swagger/OpenAPI pour la documentation de l'API
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoopAchat Back Office API")
                        .description("API pour la gestion du système CoopAchat")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    /**
     * Crée le schéma de sécurité JWT pour Swagger
     */
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    /**
     * Force Swagger à toujours utiliser HTTPS et des URLs relatives,
     * pour éviter l'erreur "Mixed Content" (Failed to fetch) si le reverse proxy
     * Nginx ne transmet pas correctement l'en-tête X-Forwarded-Proto.
     */
    @Bean
    public OpenApiCustomizer forceHttpsCustomizer() {
        return openApi -> openApi.setServers(List.of(
                new Server().url("https://api.coopachat.innovimpactdev.cloud").description("Serveur de Production (HTTPS)"),
                new Server().url("/").description("Serveur Relatif")
        ));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}

