package com.example.coopachat.services.geocoding;

import com.example.coopachat.dtos.geocoding.PlaceDetailsResult;
import com.example.coopachat.exceptions.GeocodingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;

/**
 * Implémentation du service Google Place Details.
 * Récupère l'adresse formatée + coordonnées GPS à partir d'un placeId (choix utilisateur type Yango/autocomplete).
 */
@Service
@Slf4j
public class PlacesServiceImpl implements PlacesService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    // RestTemplate → appeler l'API Google Place Details
    private final RestTemplate restTemplate = new RestTemplate();
    // ObjectMapper → lire le JSON retourné par Google
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PLACE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    // Champs demandés à Google (réduit la taille de la réponse et le coût)
    private static final String FIELDS = "formatted_address,geometry,place_id";

    @Override
    public PlaceDetailsResult getPlaceDetails(String placeId) {
        // Vérifier que le placeId est fourni (envoyé par le front après sélection d'une suggestion)
        if (placeId == null || placeId.isBlank()) {
            throw new GeocodingException("placeId obligatoire");
        }
        try {
            // Construire l'URL : Place Details avec place_id + champs limités + clé API
            String url = PLACE_DETAILS_URL + "?place_id=" + placeId.trim() + "&fields=" + FIELDS + "&key=" + apiKey;
            // Appeler Google
            String response = restTemplate.getForObject(URI.create(url), String.class);
            // Lire le JSON de la réponse
            JsonNode root = objectMapper.readTree(response);
            // Vérifier le statut (OK = succès)
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                throw new GeocodingException("Lieu introuvable (status: " + status + ")");
            }
            // Extraire result
            JsonNode result = root.path("result");
            // Adresse formatée complète (ex. "Mermoz, Villa 23, Dakar")
            String formattedAddress = result.path("formatted_address").asText(null);
            // Coordonnées GPS
            JsonNode location = result.path("geometry").path("location");
            BigDecimal lat = BigDecimal.valueOf(location.path("lat").asDouble());
            BigDecimal lng = BigDecimal.valueOf(location.path("lng").asDouble());
            // Retourner le DTO prêt à être enregistré dans Address
            return new PlaceDetailsResult(formattedAddress, lat, lng, placeId.trim());
        } catch (GeocodingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur Place Details pour placeId '{}': {}", placeId, e.getMessage());
            throw new GeocodingException("Erreur Place Details: " + e.getMessage(), e);
        }
    }
}