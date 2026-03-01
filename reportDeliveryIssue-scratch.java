// ========================================
// reportDeliveryIssue – Signalement échec livraison (scratch avec commentaires)
// ========================================

@Override
@Transactional
public void reportDeliveryIssue(Long orderId, DeliveryIssueDTO dto) {
    // 1. Charger la commande ou lever une exception si introuvable
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Commande introuvable"));

    // 2. Vérifier que le livreur connecté est bien celui assigné à cette commande
    Driver driver = getDriverOrThrow();
    if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
        throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
    }

    // 3. Vérifier que la commande est dans un statut permettant le signalement (en cours ou arrivé sur place)
    if (order.getStatus() != OrderStatus.EN_COURS && order.getStatus() != OrderStatus.ARRIVE) {
        throw new RuntimeException("Seule une livraison en cours peut être signalée");
    }

    // 4. Passer la commande en échec : libellé de la raison, date du signalement, sauvegarde
    String reasonLabel = dto.getReason() != null ? dto.getReason().getLabel() : "Non précisée";
    order.setStatus(OrderStatus.ECHEC_LIVRAISON);
    order.setFailureReason(reasonLabel);
    order.setFailureReportedAt(LocalDateTime.now());
    orderRepository.save(order);

    // 5. Notifier le salarié (client) par email : livraison non effectuée + contact RL
    employeeNotificationService.notifyDeliveryFailed(order, reasonLabel);

    // 6. Notifier le RL (créateur de la tournée) par email : détail commande, raison, commentaire livreur
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

    // 7. Vérifier si la tournée est terminée (toutes commandes livrées ou en échec) → statut TERMINEE si oui
    checkTourCompletion(order.getDeliveryTour());
    log.info("Échec livraison {} signalé : {}", order.getOrderNumber(), reasonLabel);
}
