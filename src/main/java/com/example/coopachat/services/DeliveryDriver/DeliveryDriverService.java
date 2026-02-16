package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;


/**
 * Interface pour le service de gestion des actions du Livreur
 */
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




}
