package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DeliveryDriverPreferenceDTO;
import com.example.coopachat.dtos.DeliveryDriver.DeliveryZoneDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.entities.DeliveryZone;
import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.DriverAvailability;
import com.example.coopachat.entities.Users;
import com.example.coopachat.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryDriverServiceImpl implements DeliveryDriverService{

    private final DeliveryDriverRepository deliveryDriverRepository;
    private final UserRepository userRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;


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

    @Override
    public DeliveryZoneDTO getAllZones() {

        Users user = getCurrentUser();
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        DeliveryZone deliveryZone = deliveryZoneRepository.findByDriver(driver)
                .orElseThrow(() -> new RuntimeException("Aucune Zone trouvée pour ce livreur"));

        log.info("Zones de livraison récupérées pour {}", user.getEmail());
        return new DeliveryZoneDTO(
                deliveryZone.getId(),
                deliveryZone.getDeliveryZones()
        );
    }

    @Override
    public void saveZones(DeliveryZoneDTO dto) {

        // Récupérer user et driver
        Users user = getCurrentUser();
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        // Chercher ou créer ses Zones de livraison
        DeliveryZone deliveryZone = deliveryZoneRepository.findByDriver(driver)
                .orElse(new DeliveryZone());

        // Mettre à jour
       deliveryZone.setDriver(driver);
       deliveryZone.setDeliveryZones(dto.getDeliveryZones());

        // Sauvegarder
       deliveryZoneRepository.save(deliveryZone);
        log.info("Zones de livraison sauvegardées pour {}", user.getEmail());
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
