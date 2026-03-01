package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.OrderRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service dédié aux notifications envoyées au(x) Responsable(s) Logistique (RL) :
 * rappel quotidien des commandes en retard, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsManagerNotificationService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Notification quotidienne à 9h : liste des commandes en retard (EN_ATTENTE, date de livraison dépassée).
     * Envoi à tous les RL actifs ayant un email.
     */
    @Scheduled(cron = "0 0 9 * * *")//(0 0 9 * * *) = 9h00 tous les jours
    public void notifyLateOrders() {
        // 1. Récupérer les commandes en attente dont la date de livraison est dépassée
        List<Order> lateOrders = orderRepository.findByStatusAndDeliveryDateBefore(
                OrderStatus.EN_ATTENTE,
                LocalDate.now()
        );

        // Si aucune commande en retard, on sort de la méthode
        if (lateOrders.isEmpty()) {
            return;
        }

        // 2. Construire le sujet et le corps de l'email (nombre seulement, pas la liste)
        int count = lateOrders.size();
        String subject = "⚠️ " + count + " commande(s) en retard";
        String body = "Bonjour,\n\n"
                + count + " commande(s) sont en retard (date de livraison dépassée).\n\n"
                + "Action requise : Créer une tournée pour ces commandes.\n";

        // 3. Envoyer l'email à chaque RL actif
        List<Users> rlUsers = userRepository.findByRoleAndIsActiveTrue(UserRole.LOGISTICS_MANAGER);
        for (Users rl : rlUsers) {
            if (rl.getEmail() != null && !rl.getEmail().isBlank()) {
                try {
                    emailService.sendEmail(rl.getEmail(), subject, body);
                } catch (Exception e) {
                    log.error("Erreur envoi notification commandes en retard à {}: {}", rl.getEmail(), e.getMessage());
                }
            }
        }

        log.info("Notification commandes en retard envoyée à {} RL(s), {} commande(s)", rlUsers.size(), lateOrders.size());
    }
}
