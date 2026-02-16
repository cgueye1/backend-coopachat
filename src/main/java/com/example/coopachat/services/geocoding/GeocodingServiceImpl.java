package com.example.coopachat.services.geocoding;

import com.example.coopachat.entities.Coordinates;
import com.example.coopachat.exceptions.GeocodingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Slf4j

@Service
public class GeocodingServiceImpl implements GeocodingService{

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper(); //ObjectMapper sert à : ➡ lire le JSON retourné par Google
    private final RestTemplate restTemplate = new RestTemplate();//RestTemplate sert à :➡ appeler une API externe 👇🏾
    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";



    @Override
    public Coordinates getCoordinatesFromAddress(String address) {
        //On vérifie que l’adresse existe
        if (address == null || address.isBlank()) {
            throw new GeocodingException("L'adresse ne peut pas être vide");
        }
        try {
            //Encoder l’adresse , Transforme :Dakar Sénégal -> Dakar%20S%C3%A9n%C3%A9gal
            String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            //Construire l’URL
            String url = GEOCODE_URL + "?address=" + encodedAddress + "&key=" + apiKey;
            //Appeler Google
            String response = restTemplate.getForObject(URI.create(url), String.class);
            //Lire le JSON, sa réponse
            JsonNode root = objectMapper.readTree(response);
            //Vérifier le statut
            String status = root.path("status").asText();
            //Google retourne : OK sinon on lance une exception
            if (!"OK".equals(status)) {
                throw new GeocodingException("Adresse introuvable ou invalide (status: " + status + ")");
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                throw new GeocodingException("Aucun résultat pour cette adresse");
            }
            //En cas de succès, Récupérer latitude et longitude et on remplit les variables de coordonnées
            JsonNode location = results.get(0).path("geometry").path("location");
            BigDecimal lat = BigDecimal.valueOf(location.path("lat").asDouble());
            BigDecimal lng = BigDecimal.valueOf(location.path("lng").asDouble());
            return new Coordinates(lat, lng);
        } catch (GeocodingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur géocodage pour '{}': {}", address, e.getMessage());
            throw new GeocodingException("Erreur lors du géocodage: " + e.getMessage(), e);
        }
    }
}
