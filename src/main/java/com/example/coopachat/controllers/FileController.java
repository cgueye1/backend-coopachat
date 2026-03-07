package com.example.coopachat.controllers;

import com.example.coopachat.services.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

/**
 * Sert les fichiers (images, etc.) stockés dans MinIO.
 * URL : GET /api/files/{path} (ex. /api/files/products/uuid.jpg)
 * Public (permitAll) car les balises img ne peuvent pas envoyer le token JWT.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Fichiers", description = "Accès public aux fichiers (images produits, photos, etc.)")
public class FileController {

    private final MinioService minioService;

    @Operation(summary = "Récupérer un fichier", description = "Stream le fichier depuis MinIO. Chemin ex. : products/uuid.jpg, profiles/xxx.png. Si path = uuid.jpg (sans dossier), essaie products/uuid.jpg pour compatibilité.")
    @GetMapping("/{path:.+}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            InputStream stream = minioService.getFile(path);
            // Fallback : si path sans "/" (ex. uuid.jpg), essayer products/ pour les images produits (données legacy)
            if (stream == null && !path.contains("/")) {
                stream = minioService.getFile("products/" + path);
            }
            if (stream == null) {
                return ResponseEntity.notFound().build();
            }
            String contentType = guessContentType(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String guessContentType(String path) {
        if (path == null) return "application/octet-stream";
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
