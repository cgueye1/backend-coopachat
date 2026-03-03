package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.entities.DeliveryTour;
import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Users;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service dédié aux notifications par email :
 * - au livreur : tournée assignée, tournée annulée ;
 * - au RL (créateur de la tournée) : échec de livraison signalé par le livreur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverNotificationService {

    private static final java.time.format.DateTimeFormatter DATE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    /**
     * Notifie le livreur par email qu'une tournée lui a été assignée.
     * Contenu : numéro de tournée, date de livraison, nombre de commandes.
     *
     * @param tour       Tournée concernée (doit avoir driver avec user et email)
     * @param orderCount Nombre de commandes dans la tournée (passé par l'appelant si connu)
     */
    public void notifyTourAssigned(DeliveryTour tour, int orderCount) {
        // Vérifier que la tournée et le livreur (avec user) sont présents
        if (tour == null || tour.getDriver() == null || tour.getDriver().getUser() == null) {
            log.warn("Tournée ou livreur/user manquant, notification ignorée");
            return;
        }
        // Vérifier que le livreur a un email
        String email = tour.getDriver().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le livreur, notification ignorée");
            return;
        }
        // Construire les données pour l'email (prénom, numéro tournée, date)
        String firstName = Optional.ofNullable(tour.getDriver().getUser().getFirstName()).orElse("Livreur");
        String tourNumber = Optional.ofNullable(tour.getTourNumber()).orElse("-");
        String deliveryDateStr = tour.getDeliveryDate() != null
                ? tour.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";

        String subject = "Tournée assignée - " + tourNumber;
        String body = String.format(
                "Bonjour %s,n%n%nUne tournée vous a été assignée.%n%nNuméro de tournée : %s%nDate de livraison : %s%nNombre de commandes : %d%n%nConnectez-vous à votre espace livreur pour consulter les détails et l'itinéraire.%n%nL'équipe CoopAchat",
                firstName, tourNumber, deliveryDateStr, orderCount);

        // Envoyer l'email au livreur
        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'tournée assignée' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le livreur par email qu'une tournée a été annulée.
     *
     * @param tour Tournée annulée (doit avoir driver avec email et cancellationReason)
     */
    public void notifyTourCancelled(DeliveryTour tour) {
        // Vérifier que la tournée et le livreur (avec user) sont présents
        if (tour == null || tour.getDriver() == null || tour.getDriver().getUser() == null) {
            log.warn("Tournée ou livreur/user manquant, notification annulation ignorée");
            return;
        }
        // Vérifier que le livreur a un email
        String email = tour.getDriver().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le livreur, notification annulation ignorée");
            return;
        }
        // Construire nom du livreur, numéro de tournée et motif d'annulation
        String driverName = Optional.ofNullable(tour.getDriver().getUser().getFirstName()).orElse("") + " "
                + Optional.ofNullable(tour.getDriver().getUser().getLastName()).orElse("").trim();
        if (driverName.isBlank()) driverName = "Livreur";
        String tourNumber = Optional.ofNullable(tour.getTourNumber()).orElse("-");
        String reason = tour.getCancellationReason() != null && !tour.getCancellationReason().isBlank()
                ? tour.getCancellationReason() : "Non précisé";

        String subject = "Tournée annulée - " + tourNumber;
        String body = String.format(
                "Bonjour %s,%n%nNous vous informons que la tournée %s a été annulée par le responsable logistique.%n%nMotif : %s%n%nCette tournée a été retirée de votre planning.%n%nL'équipe CoopAchat",
                driverName, tourNumber, reason);

        // Envoyer l'email au livreur
        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'tournée annulée' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le RL (créateur de la tournée) que la tournée a démarré (première récupération confirmée par le livreur).
     */
    public void notifyLogisticsManagerTourStarted(DeliveryTour tour) {
        if (tour == null || tour.getCreatedBy() == null) {
            log.warn("Tour ou créateur manquant, notification tournée démarrée ignorée");
            return;
        }
        String rlEmail = tour.getCreatedBy().getEmail();
        if (rlEmail == null || rlEmail.isBlank()) {
            log.warn("Pas d'email RL pour notification tournée démarrée");
            return;
        }
        String tourNumber = Optional.ofNullable(tour.getTourNumber()).orElse("-");
        String driverName = tour.getDriver() != null && tour.getDriver().getUser() != null
                ? (Optional.ofNullable(tour.getDriver().getUser().getFirstName()).orElse("") + " "
                + Optional.ofNullable(tour.getDriver().getUser().getLastName()).orElse("")).trim()
                : "Livreur";
        if (driverName.isEmpty()) driverName = "Livreur";
        String deliveryDateStr = tour.getDeliveryDate() != null
                ? tour.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";

        String subject = "Tournée démarrée - " + tourNumber;
        String body = String.format(
                "Bonjour,%n%nLa tournée %s a démarré. Le livreur %s a confirmé la récupération des colis.%n%nDate de livraison prévue : %s%n%nCordialement,%nSystème automatique",
                tourNumber, driverName, deliveryDateStr);

        try {
            emailService.sendEmail(rlEmail, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'tournée démarrée' au RL {}: {}", rlEmail, e.getMessage());
        }
    }

    /**
     * Notifie le RL (créateur de la tournée) qu'une livraison est en échec.
     * Envoi au créateur de la tournée (createdBy).
     */
    public void notifyLogisticsManagerOfDeliveryFailure(Order order, String reason, String comment, String deliveryAddress) {
        // Vérifier que la commande, la tournée et le créateur (RL) sont présents
        if (order == null || order.getDeliveryTour() == null || order.getDeliveryTour().getCreatedBy() == null) {
            log.warn("Order ou tour/créateur manquant, notification échec livraison RL ignorée");
            return;
        }
        // Récupérer l'email du RL (créateur de la tournée)
        String rlEmail = order.getDeliveryTour().getCreatedBy().getEmail();
        if (rlEmail == null || rlEmail.isBlank()) {
            log.warn("Pas d'email RL pour notification échec livraison");
            return;
        }
        // Construire les infos pour le corps de l'email : commande, client, adresse, livreur, raison, commentaire
        String orderNumber = Optional.ofNullable(order.getOrderNumber()).orElse("-");
        // getFirstName/getLastName peuvent être null → on évite de passer null au format
        Users clientUser = order.getEmployee() != null ? order.getEmployee().getUser() : null;
        String clientFirst = clientUser != null && clientUser.getFirstName() != null ? clientUser.getFirstName() : "";
        String clientLast = clientUser != null && clientUser.getLastName() != null ? clientUser.getLastName() : "";
        String driverName = order.getDeliveryTour().getDriver() != null && order.getDeliveryTour().getDriver().getUser() != null
                ? (Optional.ofNullable(order.getDeliveryTour().getDriver().getUser().getFirstName()).orElse("") + " "
                + Optional.ofNullable(order.getDeliveryTour().getDriver().getUser().getLastName()).orElse("")).trim()
                : "-";
        if (driverName.isEmpty()) driverName = "-";
        String address = deliveryAddress != null && !deliveryAddress.isBlank() ? deliveryAddress : "Non renseignée";
        String reasonStr = reason != null && !reason.isBlank() ? reason : "Non précisée";
        String commentStr = comment != null && !comment.isBlank() ? comment : "-";

        String subject = "Échec livraison - " + orderNumber;
        String body = String.format(
                "Bonjour,%n%nUne livraison n'a pas pu être effectuée :%n%nCommande : %s%nClient : %s %s%nAdresse : %s%nLivreur : %s%nRaison : %s%nCommentaire : %s%n%nAction requise : Replanifier cette livraison.%n%nCordialement,%nSystème automatique",
                orderNumber, clientFirst, clientLast, address, driverName, reasonStr, commentStr);

        // Envoyer l'email au RL
        try {
            emailService.sendEmail(rlEmail, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification échec livraison au RL {}: {}", rlEmail, e.getMessage());
        }
    }
}
