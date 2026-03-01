// =============================================================================
// SCRATCH : reportDeliveryIssue — version commentée (référence)
// =============================================================================
// Contexte : DeliveryDriverServiceImpl — le livreur signale un échec de livraison
// pour une commande (client absent, adresse introuvable, etc.).
// =============================================================================

@Override
@Transactional
public void reportDeliveryIssue(Long orderId, DeliveryIssueDTO dto) {

    // ----- 1. Chargement de la commande -----
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande introuvable"));

    // ----- 2. Vérification : le livreur connecté est bien assigné à cette commande -----
    Driver driver = getDriverOrThrow();
    if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
        throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
    }

    // ----- 3. Vérification du statut : seule une livraison EN_COURS ou ARRIVE peut être signalée en échec -----
    if (order.getStatus() != OrderStatus.EN_COURS && order.getStatus() != OrderStatus.ARRIVE) {
        throw new RuntimeException("Seule une livraison en cours peut être signalée");
    }

    // ----- 4. Passage en échec : raison (libellé enum), date de signalement, sauvegarde -----
    String reasonLabel = dto.getReason() != null ? dto.getReason().getLabel() : "Non précisée";
    order.setStatus(OrderStatus.ECHEC_LIVRAISON);
    order.setFailureReason(reasonLabel);
    order.setFailureReportedAt(LocalDateTime.now());
    orderRepository.save(order);

    // ----- 5. Notification au salarié (client) : email + n° du RL (créateur de la tournée) -----
    employeeNotificationService.notifyDeliveryFailed(order, reasonLabel);

    // ----- 6. Notification au RL : email détail (commande, client, adresse, livreur, raison, commentaire) -----
    Users rl = order.getDeliveryTour().getCreatedBy();
    if (rl != null && rl.getEmail() != null && !rl.getEmail().isBlank()) {
        String deliveryAddress = getDeliveryAddressFromOrder(order);
        driverNotificationService.notifyLogisticsManagerOfDeliveryFailure(
                order,
                reasonLabel,
                dto.getComment() != null ? dto.getComment() : "",
                deliveryAddress != null ? deliveryAddress : "Non renseignée"
        );
    }

    // ----- 7. Si toutes les commandes de la tournée sont terminées (LIVREE ou ECHEC), tournée TERMINEE -----
    checkTourCompletion(order.getDeliveryTour());

    log.info("Échec livraison {} signalé : {}", order.getOrderNumber(), reasonLabel);
}
