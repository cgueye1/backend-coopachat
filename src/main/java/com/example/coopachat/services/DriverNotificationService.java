package com.example.coopachat.services;

import com.example.coopachat.entities.DeliveryTour;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service dédié aux notifications envoyées au livreur (email) :
 * tournée assignée, annulation, etc.
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
        if (tour == null || tour.getDriver() == null || tour.getDriver().getUser() == null) {
            log.warn("Tournée ou livreur/user manquant, notification ignorée");
            return;//on sort de la méthode si la tournée ou le livreur/user est manquant
        }
        String email = tour.getDriver().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le livreur, notification ignorée");
            return;
        }
        String firstName = Optional.ofNullable(tour.getDriver().getUser().getFirstName()).orElse("Livreur");
        String tourNumber = Optional.ofNullable(tour.getTourNumber()).orElse("-");
        String deliveryDateStr = tour.getDeliveryDate() != null
                ? tour.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";

        String subject = "Tournée assignée - " + tourNumber;
        String body = String.format(
                "Bonjour %s,n%n%nUne tournée vous a été assignée.%n%nNuméro de tournée : %s%nDate de livraison : %s%nNombre de commandes : %d%n%nConnectez-vous à votre espace livreur pour consulter les détails et l'itinéraire.%n%nL'équipe CoopAchat",
                firstName, tourNumber, deliveryDateStr, orderCount);

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
        if (tour == null || tour.getDriver() == null || tour.getDriver().getUser() == null) {
            log.warn("Tournée ou livreur/user manquant, notification annulation ignorée");
            return;
        }
        String email = tour.getDriver().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le livreur, notification annulation ignorée");
            return;
        }
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

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'tournée annulée' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le responsable logistique par email lorsqu'un livreur soumet un signalement.
     */
    public void notifyLogisticsManagerOfDriverReport(String rlEmail, String driverName, String reportTypeLabel, String comment, String orderNumber) {
        if (rlEmail == null || rlEmail.isBlank()) {
            log.warn("Pas d'email RL pour notification signalement livreur");
            return;
        }
        String subject = "Signalement livreur - " + (reportTypeLabel != null ? reportTypeLabel : "") + " - CoopAchat";
        String orderLine = (orderNumber != null && !orderNumber.isBlank())
                ? "Commande : " + orderNumber + "\n\n"
                : "";
        String commentLine = (comment != null && !comment.isBlank())
                ? "Commentaire : " + comment + "\n\n"
                : "";
        String body = String.format(
                "Un livreur a soumis un signalement.%n%nLivreur : %s%nNature : %s%n%n%s%sMerci de prendre les mesures nécessaires.%n%nL'équipe CoopAchat",
                driverName != null ? driverName : "-",
                reportTypeLabel != null ? reportTypeLabel : "-",
                orderLine,
                commentLine);

        try {
            emailService.sendEmail(rlEmail, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification signalement livreur au RL {}: {}", rlEmail, e.getMessage());
        }
    }
}
