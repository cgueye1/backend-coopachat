package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DeliveryDriverPreferenceDTO;
import com.example.coopachat.dtos.DeliveryDriver.DeliveryZoneDTO;
import com.example.coopachat.dtos.DeliveryDriver.DeliveryZoneResponseDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.entities.DeliveryDriverZone;
import com.example.coopachat.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryDriverServiceImpl implements DeliveryDriverService{

    private final DeliveryDriverRepository deliveryDriverRepository;
    private final UserRepository userRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final DeliveryDriverZoneRepository deliveryDriverZoneRepository;
    private final ZoneReferenceRepository zoneReferenceRepository;


    @Override
    public DriverPersonalInfoDTO getPersonalInfo() {

        Users user = getCurrentUser();

        // Récupérer le livreur associé à cet utilisateur
       Driver driver = deliveryDriverRepository.findByUser(user)
               .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        return new DriverPersonalInfoDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail()
        );
    }

    @Override
    @Transactional
    public void updatePersonalInfo(DriverPersonalInfoDTO updateRequest) {

        Users user = getCurrentUser();

        // Mettre à jour uniquement les champs autorisés
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }

        userRepository.save(user);
        log.info("Mise à jour réussie pour le livreur : {} {}",
                user.getFirstName(), user.getLastName());
    }

    @Override
    @Transactional
    public void saveAvailabilityPreference(DeliveryDriverPreferenceDTO dto) {

        // Récupérer user et driver
        Users user = getCurrentUser();
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        // Chercher ou créer ses disponibilités
        DriverAvailability availability = driverAvailabilityRepository.findByDriver(driver)
                .orElse(new DriverAvailability());

        // Mettre à jour
        availability.setDriver(driver);
        availability.setAvailableDays(dto.getPreferredDays());
        availability.setPreferredTimeSlot(dto.getPreferredTimeSlot());

        // Sauvegarder
        driverAvailabilityRepository.save(availability);
        log.info("Disponibilités sauvegardées pour {}", user.getEmail());
    }



    @Override
    @Transactional
    public DeliveryDriverPreferenceDTO getAvailabilityPreference() {
        Users user = getCurrentUser();
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        DriverAvailability availability = driverAvailabilityRepository.findByDriver(driver)
                .orElseThrow(() -> new RuntimeException("Aucune disponibilité trouvée"));

        log.info("Disponibilités récupérées pour {}", user.getEmail());

        return new DeliveryDriverPreferenceDTO(
                availability.getId(),
                availability.getAvailableDays(),
                availability.getPreferredTimeSlot()
        );
    }

    /**
     * Récupère les détails complets des zones de livraison du livreur
     * afin de les afficher dans l'application (mobile / web)
     */
    @Override
    public DeliveryZoneResponseDTO getMyZonesWithDetails() {

        // Récupération de l'utilisateur actuellement connecté
        Users user = getCurrentUser();

        // Recherche du livreur associé à cet utilisateur
        // Si aucun livreur n'est trouvé, on lève une exception
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        //Récupération de la configuration des zones de livraison du livreur
        DeliveryDriverZone deliveryDriverZone = deliveryDriverZoneRepository
                .findByDriver(driver)
                .orElse(null);

        //Cas où le livreur n'a aucune zone définie ou que la liste est vide
        if (deliveryDriverZone == null || deliveryDriverZone.getZones().isEmpty()) {
            log.info("Aucune zone pour {}", user.getEmail());

            //Crée un DTO avec une liste vide pour le frontend
            DeliveryZoneResponseDTO emptyResponse = new DeliveryZoneResponseDTO();
            emptyResponse.setZones(new HashSet<>()); // liste vide
            return emptyResponse;
        }

        // Transformation des entités Zone en objets ZoneDetail
        Set<DeliveryZoneResponseDTO.ZoneDetail> zoneDetails = new HashSet<>();
        for (DeliveryZoneReference zone : deliveryDriverZone.getZones()) {

            // Crée un objet ZoneDetail pour chaque zone
            DeliveryZoneResponseDTO.ZoneDetail detail = new DeliveryZoneResponseDTO.ZoneDetail();
            detail.setId(zone.getId());                 // ID de la zone
            detail.setZoneName(zone.getZoneName());     // Nom de la zone
            detail.setDescription(zone.getDescription());// Description
            detail.setActive(zone.getActive());         // Statut (active ou non)

            zoneDetails.add(detail);
        }

        //Construction de la réponse finale pour le frontend
        DeliveryZoneResponseDTO response = new DeliveryZoneResponseDTO();
        response.setId(deliveryDriverZone.getId());  // ID
        response.setZones(zoneDetails);              // Liste des zones détaillées

        return response;
    }


    /**
     * Crée les zones de livraison du livreur
     */
    @Transactional
    public void createZones(DeliveryZoneDTO dto) {

        // Validation basique
        if (dto.getZoneIds() == null || dto.getZoneIds().isEmpty()) {
            throw new RuntimeException("Veuillez sélectionner au moins une zone");
        }

        // Récupérer le driver connecté
        Users user = getCurrentUser();
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        // Vérifier qu'il n'a pas déjà des zones
        if (deliveryDriverZoneRepository.existsByDriver(driver)) {
            throw new RuntimeException("Le livreur a déjà des zones configurées");
        }

        // Créer la nouvelle entrée
        DeliveryDriverZone newDriverZone = new DeliveryDriverZone();
        newDriverZone.setDriver(driver);

        // Récupérer les zones de référence
        Set<DeliveryZoneReference> zones = new HashSet<>();

        for (Long zoneId : dto.getZoneIds()) {
            // vérifier pour chaque zone s'il existe d'abord et s'il est active
            DeliveryZoneReference zone = zoneReferenceRepository.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Zone introuvable: " + zoneId));

            if (!zone.getActive()) {
                throw new RuntimeException("La zone " + zone.getZoneName() + " n'est pas active");
            }

            zones.add(zone);
        }

        newDriverZone.setZones(zones);// ajouter la liste des zones

        // Sauvegarder
        deliveryDriverZoneRepository.save(newDriverZone);

        log.info("Zones créées pour {}: {} zones", user.getEmail(), zones.size());
    }

    /**
     * Récupère l'utilisateur actuellement connecté
     * @return Users l'utilisateur connecté
     * @throws RuntimeException si aucun utilisateur n'est authentifié
     */
    private Users getCurrentUser() {

        // 1. Récupérer l'authentification Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Vérifier que l'utilisateur est bien authentifié
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        // 3. Récupérer l'email (username) de l'utilisateur
        String userEmail = authentication.getName();

        // 4. Chercher l'utilisateur dans la base de données
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable avec email: " + userEmail
                ));
    }
}
