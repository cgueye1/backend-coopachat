package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverDeliveryListItemDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;


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

    /**
     * Liste des livraisons du livreur connecté (commandes de ses tournées), avec filtres optionnels.
     * @param deliveryDate date de livraison (optionnel, ex. aujourd'hui)
     * @param status statut de la commande (optionnel : À livrer / En cours / Livrée)
     * @param search recherche par numéro de commande ou nom du client (optionnel)
     * @return liste de DriverDeliveryListItemDTO
     */
    List<DriverDeliveryListItemDTO> getMyDeliveries(LocalDate deliveryDate, OrderStatus status, String search);




}
