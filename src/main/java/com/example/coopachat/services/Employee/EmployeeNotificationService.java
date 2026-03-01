package com.example.coopachat.services.Employee;

import com.example.coopachat.entities.Address;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Order;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Service dédié aux notifications envoyées au salarié (email) :
 * commande enregistrée, livraison planifiée, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeNotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    /**
     * Notifie le salarié par email que sa commande a été enregistrée.
     * Contenu : numéro de commande, montant total, date de livraison.
     *
     * @param order Commande concernée (doit avoir employee et user avec email)
     */
    public void notifyOrderCreated(Order order) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            log.warn("order ou employee/user manquant, notification ignorée");
            return;
        }
        String email = order.getEmployee().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le salarié, notification ignorée");
            return;
        }
        String firstName = Optional.ofNullable(order.getEmployee().getUser().getFirstName()).orElse("Salarié");
        String orderNumber = Optional.ofNullable(order.getOrderNumber()).orElse("-");
        String amount = formatAmount(order.getTotalPrice());
        String deliveryDateStr = order.getDeliveryDate() != null
                ? order.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";

        String subject = "Commande enregistrée - " + orderNumber;
        String body = String.format(
                "Bonjour %s,%n%nVotre commande a bien été enregistrée.%n%nNuméro de commande : %s%nMontant total : %s%nDate de livraison (estimation) : %s%n%nCette date est une estimation. Vous serez notifié par email dès que votre commande sera validée et qu'une date de livraison définitive vous sera assignée.%n%nMerci pour votre confiance.%nL'équipe CoopAchat",
                firstName, orderNumber, amount, deliveryDateStr);

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'commande enregistrée' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le salarié par email que sa livraison est planifiée.
     * Contenu : date, créneau (option de livraison), adresse de livraison.
     *
     * @param order Commande concernée (doit avoir employee avec adresse si possible)
     */
    public void notifyOrderScheduled(Order order) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            log.warn(" order ou employee/user manquant, notification ignorée");
            return;
        }
        String email = order.getEmployee().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le salarié, notification ignorée");
            return;
        }
        String firstName = Optional.ofNullable(order.getEmployee().getUser().getFirstName()).orElse("Salarié");
        String orderNumber = Optional.ofNullable(order.getOrderNumber()).orElse("-");
        String deliveryDateStr = order.getDeliveryDate() != null
                ? order.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";
        String timeSlot = order.getDeliveryOption() != null && order.getDeliveryOption().getName() != null
                ? order.getDeliveryOption().getName()
                : "À définir";
        String address = getPrimaryAddressFormatted(order.getEmployee());

        String subject = "Livraison planifiée - " + orderNumber;
        String body = String.format("Bonjour %s,%n%nVotre livraison pour la commande %s est planifiée.%n%nDate : %s%nCréneau : %s%nAdresse de livraison : %s%n%nL'équipe CoopAchat", firstName, orderNumber, deliveryDateStr, timeSlot, address);

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'livraison planifiée' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le salarié qu'une livraison n'a pas pu être effectuée (échec signalé par le livreur).
     * Affiche le numéro de téléphone du RL qui a créé la tournée pour recontact.
     */
    public void notifyDeliveryFailed(Order order, String reason) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            log.warn("Order ou employee/user manquant, notification échec livraison ignorée");
            return;
        }
        String email = order.getEmployee().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le salarié, notification échec livraison ignorée");
            return;
        }
        String firstName = Optional.ofNullable(order.getEmployee().getUser().getFirstName()).orElse("Salarié");
        String orderNumber = Optional.ofNullable(order.getOrderNumber()).orElse("-");
        String reasonStr = reason != null && !reason.isBlank() ? reason : "Non précisée";
        String deliveryDateStr = order.getDeliveryDate() != null
                ? order.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";
        String contactPhone = null;
        if (order.getDeliveryTour() != null && order.getDeliveryTour().getCreatedBy() != null
                && order.getDeliveryTour().getCreatedBy().getPhone() != null
                && !order.getDeliveryTour().getCreatedBy().getPhone().isBlank()) {
            contactPhone = order.getDeliveryTour().getCreatedBy().getPhone();
        }
        //afficher le numéro de téléphone s'il est pas null sinon on affiche l'autre message
        String contactLine = (contactPhone != null)
                ? "Vous pouvez également nous joindre au " + contactPhone + "."
                : "Vous pouvez nous contacter via votre espace ou par email.";

        String subject = "Problème avec votre livraison";
        String body = String.format(
                "Bonjour %s,%n%nVotre commande %s n'a malheureusement pas pu être livrée.%n%nRaison : %s%nDate tentative : %s%n%nNotre équipe vous recontactera dans les 24h pour replanifier.%n%n%s%n%nDésolé pour le désagrément.%n%nCordialement,%nL'équipe Coop Achat Salarié",
                firstName, orderNumber, reasonStr, deliveryDateStr, contactLine);

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'échec livraison' à {}: {}", email, e.getMessage());
        }
    }

    /**
     * Notifie le salarié qu'une livraison a été reportée (tournée annulée).
     * Affiche le numéro du RL qui a créé la tournée pour contact.
     */
    public void notifyTourCancelled(Order order, String reason) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            log.warn("Order ou employee/user manquant, notification tournée annulée ignorée");
            return;
        }
        String email = order.getEmployee().getUser().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Pas d'email pour le salarié, notification tournée annulée ignorée");
            return;
        }
        String firstName = Optional.ofNullable(order.getEmployee().getUser().getFirstName()).orElse("Salarié");
        String orderNumber = Optional.ofNullable(order.getOrderNumber()).orElse("-");
        String reasonStr = reason != null && !reason.isBlank() ? reason : "Non précisée";
        String deliveryDateStr = order.getDeliveryDate() != null
                ? order.getDeliveryDate().format(DATE_FORMAT)
                : "À définir";
        String contactPhone = null;
        if (order.getDeliveryTour() != null && order.getDeliveryTour().getCreatedBy() != null
                && order.getDeliveryTour().getCreatedBy().getPhone() != null
                && !order.getDeliveryTour().getCreatedBy().getPhone().isBlank()) {
            contactPhone = order.getDeliveryTour().getCreatedBy().getPhone();
        }
        String contactLine = (contactPhone != null)
                ? "Vous pouvez nous joindre au " + contactPhone + " pour toute question."
                : "";

        String subject = "Votre livraison a été reportée";
        String body = String.format(
                "Bonjour %s,%n%nVotre livraison prévue le %s a été annulée.%n%nRaison : %s%n%nVotre commande %s sera automatiquement replanifiée.%nVous recevrez une nouvelle confirmation prochainement.%n%n%s%n%nDésolé pour ce contretemps.%n%nCordialement,%nL'équipe Coop Achat Salarié",
                firstName, deliveryDateStr, reasonStr, orderNumber, contactLine);

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Erreur envoi notification 'tournée annulée' à {}: {}", email, e.getMessage());
        }
    }

    private String formatAmount(BigDecimal totalPrice) {
        if (totalPrice == null) return "0 FCFA";
        return String.format("%.0f FCFA", totalPrice);
    }

    private String getPrimaryAddressFormatted(Employee employee) {
        if (employee == null || employee.getAddresses() == null) return "Non renseignée";
        // On cherche d'abord l'adresse principale
        for (Address a : employee.getAddresses()) {
            // Si l'adresse est principale et non vide, on la retourne
            if (a != null && a.isPrimary() && a.getFormattedAddress() != null && !a.getFormattedAddress().isBlank())
                return a.getFormattedAddress();
        }
        // Si aucune adresse principale n'est trouvée, on cherche une adresse non vide
        for (Address a : employee.getAddresses()) {
            if (a != null && a.getFormattedAddress() != null && !a.getFormattedAddress().isBlank())
            // Si on trouve une adresse non vide , on la retourne et on sort de la boucle
                return a.getFormattedAddress();
        }
        // Si aucune adresse non vide n'est trouvée, on retourne "Non renseignée"
        return "Non renseignée";
    }
}
