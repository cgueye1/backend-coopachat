package com.example.coopachat.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Sert les fichiers uploadés (images, etc.) depuis le répertoire configuré (app.files.dir). */
@RestController
@RequestMapping({"/files", "/api/files", "/uploads"})
public class FileController {

    @Value("${app.files.dir:files}")
    private String filesDir;

    private Path getFileUploadDir() {
        return Paths.get(filesDir).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path baseDir = getFileUploadDir();
            Path filePath = baseDir.resolve(filename).normalize();
            if (!filePath.startsWith(baseDir)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] data = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .body(new ByteArrayResource(data));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
