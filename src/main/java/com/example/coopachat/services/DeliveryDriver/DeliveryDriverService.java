package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DeliveryDriverPreferenceDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;

public interface DeliveryDriverService {

    /**
     * Récupère les informations personnelles du livreur connecté
     * @return DriverPersonalInfoDTO contenant les informations personnelles
     */
    DriverPersonalInfoDTO getPersonalInfo();

    /**
     * Met à jour les informations personnelles du livreur connecté
     * (uniquement nom, prénom et téléphone)
     * @param updateRequest DTO contenant les nouvelles valeurs
     */
    void updatePersonalInfo(DriverPersonalInfoDTO updateRequest);

    // ============================================================================
    // 📅 PRÉFÉRENCES DE DISPONIBILITÉ
    // ============================================================================

    /**
     * Récupère les préférences de disponibilité du livreur connecté
     *
     * @return DeliveryDriverPreferenceDTO les préférences du livreur
     * @throws RuntimeException si livreur non trouvé
     */
    DeliveryDriverPreferenceDTO getAvailabilityPreference();

    /**
     * Crée ou met à jour les préférences de disponibilité du livreur connecté
     *
     * @param dto DTO contenant les préférences (jours, créneaux)
     * @throws RuntimeException si livreur non trouvé
     */
    void saveAvailabilityPreference(DeliveryDriverPreferenceDTO dto);
}
