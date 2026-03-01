package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;


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
     * Liste paginée des livraisons du livreur (simplifiée).
     * @param statusFilter ALL | TO_CONFIRM | IN_PROGRESS | COMPLETED
     * @param page page (0-based)
     * @param size taille de page
     * @return DriverDeliveriesResponseDTO avec liste de cartes + infos pagination
     */
    DriverDeliveriesResponseDTO getMyDeliveries(String statusFilter, int page, int size);

    /**
     * Indique si le livreur a une tournée en cours (statut EN_COURS).
     */
    boolean hasActiveTour();

    /** Livreur confirme la récupération des colis au dépôt → tournée EN_COURS. */
    void confirmPickup(Long tourId);

    /** Livreur lance la livraison (en route vers le client) → commande EN_COURS. */
    void startDelivery(Long orderId);

    /** Livreur confirme son arrivée sur place → commande ARRIVE. */
    void confirmArrival(Long orderId);

    /** Livreur finalise la remise du colis → commande LIVREE ; si toutes livrées → tournée TERMINEE. */
    void completeDelivery(Long orderId);

    /**
     * Livreur confirme avoir reçu le paiement en espèces du client (bouton "Confirmer le paiement").
     * Réservé au livreur : la commande doit être dans sa tournée. Crée ou met à jour le Payment en CASH / PAID.
     */
    void confirmCashPayment(Long orderId);

    /**
     * Détail simplifié d'une livraison pour l'écran livreur (commande, client, adresse, montant).
     * La commande doit appartenir à une tournée assignée au livreur connecté.
     */
    DeliveryDetailDTO getDeliveryDetail(Long orderId);

    /** Récupère l'adresse du livreur connecté (formattedAddress + lat/long). */
    DriverAddressDTO getMyAddress();

    /** Met à jour l'adresse du livreur (formattedAddress + lat/long, rempli par le mobile via Google). */
    void updateMyAddress(DriverAddressDTO dto);

    /**
     * Signaler un problème de livraison (échec) : commande passée en ECHEC_LIVRAISON,
     * notification au salarié et au RL (créateur de la tournée).
     */
    void reportDeliveryIssue(Long orderId, DeliveryIssueDTO dto);

    /**
     * Tableau de bord du livreur : livraisons aujourd'hui, total livraisons, satisfaction moyenne.
     */
    DriverDashboardDTO getDashboard();
}
