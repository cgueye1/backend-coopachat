package com.example.coopachat.services.minio;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

//-----------------------------------UPLOAD d'un fichier unique-----------------------------------
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        // Upload un fichier dans le bucket, avec optionnellement un préfixe (folder).
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());//vérifie si le bucket existe
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());//crée le bucket si il n'existe pas
            }

            String originalFilename = file.getOriginalFilename();//récupère le nom du fichier
            String extension = StringUtils.getFilenameExtension(originalFilename);//récupère l'extension du fichier
            String fileName = UUID.randomUUID() + (extension != null ? "." + extension : "");//génère un nom de fichier unique
            String objectKey = (StringUtils.hasText(folder)) ? folder + "/" + fileName : fileName;//ajoute le préfixe au nom du fichier

          //upload le fichier dans le bucket avec les informations du fichier
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            return objectKey;
        } catch (Exception e) {
            log.error("Error uploading file to Minio", e);
            throw new RuntimeException("Erreur lors de l'upload du fichier : " + e.getMessage());
        }
    }

    //-----------------------------------UPLOAD de plusieurs fichiers-----------------------------------
    @Override
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String folder) {
        // Boucle sur les fichiers et appelle uploadFile pour chacun.
        List<String> fileNames = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String fileName = uploadFile(file, folder);
                if (fileName != null) {
                    fileNames.add(fileName);
                }
            }
        }
        return fileNames;
    }


    //-----------------------------------GET l'URL d'un fichier-----------------------------------
    @Override
    public String getFileUrl(String fileName) {
       
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return minioUrl + "/" + bucket + "/" + fileName;//construit l'URL publique du fichier
    }

    // Télécharge le fichier depuis MinIO et retourne son InputStream.
    @Override
    public InputStream getFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Chemin fichier vide");
        }
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            // Fallback : si le chemin n'a pas de dossier (ex. uuid.jpg) et que l'objet n'existe pas,
            // réessayer avec products/ (images produits souvent stockées sans préfixe en BDD)
            if (!fileName.contains("/")) {
                try {
                    return minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucket)
                                    .object("products/" + fileName)
                                    .build());
                } catch (Exception e2) {
                    log.error("Error getting file from Minio (path={}, products/fallback failed)", fileName, e2);
                    throw new RuntimeException("Erreur lors de la récupération du fichier : " + e.getMessage());
                }
            }
            log.error("Error getting file from Minio", e);
            throw new RuntimeException("Erreur lors de la récupération du fichier : " + e.getMessage());
        }
    }


    //-----------------------------------DELETE un fichier-----------------------------------
    @Override
    public void deleteFile(String fileName) {
        try {
            //supprime le fichier dans le bucket (conteneur racine)
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Error deleting file from Minio", e);
            throw new RuntimeException("Erreur lors de la suppression du fichier : " + e.getMessage());
        }
    }
}
