package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DriverDeliveredOrderDetailsDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;
import com.example.coopachat.dtos.reference.ReferenceItemDTO;

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

    /** Livreur confirme la récupération d'une commande (swipe par commande). Commande VALIDEE → EN_PREPARATION ; si première de la tournée → tournée EN_COURS et notification RL. */
    void confirmPickup(Long tourId, Long orderId);

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
     * Livreur confirme le paiement en ligne : vérifie que le salarié a bien payé (status = PAID).
     * Si oui → succès ; sinon → erreur. Bouton "Confirmer paiement" côté livreur pour paiement en ligne.
     */
    void confirmOnlinePayment(Long orderId);

    /**
     * Détail simplifié d'une livraison pour l'écran livreur (commande, client, adresse, montant).
     * La commande doit appartenir à une tournée assignée au livreur connecté.
     */
    DeliveryDetailDTO getDeliveryDetail(Long orderId);

    /** Détail complet d'une commande LIVREE pour le livreur connecté. */
    DriverDeliveredOrderDetailsDTO getDeliveredOrderDetails(Long orderId);

    /** Récupère l'adresse du livreur connecté (formattedAddress + lat/long). */
    DriverAddressDTO getMyAddress();

    /** Met à jour l'adresse du livreur (formattedAddress + lat/long, rempli par le mobile via Google). */
    void updateMyAddress(DriverAddressDTO dto);

    /**
     * Liste des raisons d'échec livraison pour le formulaire « Signaler un problème » (dropdown, livreur).
     */
    List<ReferenceItemDTO> getDeliveryIssueReasons();

    /**
     * Signaler un problème de livraison (échec) : commande passée en ECHEC_LIVRAISON,
     * notification au salarié et au RL (créateur de la tournée).
     */
    void reportDeliveryIssue(Long orderId, DeliveryIssueDTO dto);

    /**
     * Tableau de bord du livreur : livraisons aujourd'hui, total livraisons, gains, satisfaction.
     * Performances : découpage du mois en cours uniquement (S1, S2, … par blocs de 7 jours).
     */
    DriverDashboardDTO getDashboard();
}
