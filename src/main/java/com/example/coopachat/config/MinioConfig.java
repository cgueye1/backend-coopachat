package com.example.coopachat.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Configuration de l'instance MinIO
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;//url de l'instance MinIO

    @Value("${minio.access-key}")
    private String accessKey;//clé d'accès à l'instance MinIO

    @Value("${minio.secret-key}")
    private String secretKey;//clé secrète à l'instance MinIO

    @Bean
    public MinioClient minioClient() {//configuration de l'instance MinIO
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
