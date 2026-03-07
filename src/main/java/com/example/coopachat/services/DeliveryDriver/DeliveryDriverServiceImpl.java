package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPerformanceItemDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveryCardDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.DeliveryIssueReportSource;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentMethodType;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.PaymentTimingType;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.Employee.EmployeeNotificationService;
import com.example.coopachat.services.fee.FeeService;
import com.example.coopachat.repositories.DriverEarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des actions du Livreur
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryDriverServiceImpl implements DeliveryDriverService{

    private final DeliveryDriverRepository deliveryDriverRepository;
    private final UserRepository userRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final OrderRepository orderRepository;
    private final DeliveryTourRepository deliveryTourRepository;
    private final FeeService feeService;
    private final PaymentRepository paymentRepository;
    private final DriverNotificationService driverNotificationService;
    private final EmployeeNotificationService employeeNotificationService;
    private final DriverReviewRepository driverReviewRepository;
    private final DeliveryIssueReportRepository deliveryIssueReportRepository;
    private final DriverEarningRepository driverEarningRepository;


    // ========================================
    // INFORMATIONS PERSONNELLES DU LIVREUR
    // ========================================

    @Override
    public DriverPersonalInfoDTO getPersonalInfo() {
        // 1. Récupérer l'utilisateur connecté
        Users user = getCurrentUser();
        // 2. Vérifier que c'est bien un livreur (et récupérer le driver)
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));
        // 3. Retourner nom, prénom, téléphone, email (vehicleType = tour assignée, pas dans le profil)
        return new DriverPersonalInfoDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail()
        );
    }

    @Override
    @Transactional
    public void updatePersonalInfo(DriverPersonalInfoDTO updateRequest) {
        Users user = getCurrentUser();
        Driver driver = getDriverOrThrow();
        // 2. Mettre à jour uniquement les champs fournis (prénom, nom, téléphone)
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }
        // 3. Sauvegarder
        userRepository.save(user);
        log.info("Mise à jour réussie pour le livreur : {} {}", user.getFirstName(), user.getLastName());
    }
    @Override
    @Transactional(readOnly = true)
    public DriverAddressDTO getMyAddress() {
        // 1. Récupérer le livreur connecté et retourner son adresse (formattedAddress + lat/long)
        Driver driver = getDriverOrThrow();
        return new DriverAddressDTO(
                driver.getFormattedAddress(),
                driver.getLatitude(),
                driver.getLongitude()
        );
    }

    @Override
    @Transactional
    public void updateMyAddress(DriverAddressDTO dto) {
        // 1. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();
        // 2. Mettre à jour l'adresse (formattedAddress + lat/long)
        driver.setFormattedAddress(dto.getFormattedAddress());
        driver.setLatitude(dto.getLatitude());
        driver.setLongitude(dto.getLongitude());
        // 3. Sauvegarder
        deliveryDriverRepository.save(driver);
        log.info("Adresse livreur mise à jour pour {}", driver.getUser().getEmail());
    }
    // ========================================//DÉMARRER UNE TOURNÉE//========================================
    
    //--------------------------------LISTE DES LIVRAISONS (SIMPLIFIÉ - PAGINÉ)--------------------------------
   

    @Override
    @Transactional(readOnly = true)
    public DriverDeliveriesResponseDTO getMyDeliveries(String statusFilter, int page, int size) {
        // 1. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();
        // 2. Créer la pagination (tri : date livraison, tournée, id commande)
        Pageable pageable = PageRequest.of(page, size, Sort.by("deliveryDate").ascending().and(Sort.by("deliveryTour.id").ascending()).and(Sort.by("id").ascending()));

        // 3. Récupérer les commandes selon le filtre (ALL, TO_CONFIRM, IN_PROGRESS, COMPLETED)
        Page<Order> ordersPage;
        if (statusFilter == null || "ALL".equals(statusFilter)) {
            ordersPage = orderRepository.findByDriverAndTourStatusIn(
                    driver.getId(),
                    List.of(DeliveryTourStatus.ASSIGNEE, DeliveryTourStatus.EN_COURS),
                    pageable);
        } else if ("TO_CONFIRM".equals(statusFilter)) {
            ordersPage = orderRepository.findByDriverAndTourStatus(
                    driver.getId(),
                    DeliveryTourStatus.ASSIGNEE,
                    pageable);
        } else if ("IN_PROGRESS".equals(statusFilter)) {
            ordersPage = orderRepository.findByDriverAndTourStatus(
                    driver.getId(),
                    DeliveryTourStatus.EN_COURS,
                    pageable);
        } else if ("COMPLETED".equals(statusFilter)) {
            ordersPage = orderRepository.findByDriverAndTourStatusIn(
                    driver.getId(),
                    List.of(DeliveryTourStatus.TERMINEE, DeliveryTourStatus.ANNULEE),
                    pageable);
        } else {
            ordersPage = orderRepository.findByDriverAndTourStatusIn(
                    driver.getId(),
                    List.of(DeliveryTourStatus.ASSIGNEE, DeliveryTourStatus.EN_COURS),
                    pageable);
        }

        // 4. Mapper les commandes vers DriverDeliveryCardDTO
        List<DriverDeliveryCardDTO> deliveries = ordersPage.getContent().stream()
                .map(this::mapToDriverDeliveryCardDTO)
                .collect(Collectors.toList());

        // 5. Retourner la réponse paginée
        return new DriverDeliveriesResponseDTO(
                deliveries,
                ordersPage.getTotalElements(),
                ordersPage.getTotalPages(),
                ordersPage.getNumber(),
                ordersPage.getSize(),
                ordersPage.hasNext(),
                ordersPage.hasPrevious());
    }

    //---------------------- Indique si le livreur a une tournée en cours-----------
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveTour() {
        // 1. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();
        // 2. Vérifier s'il existe une tournée EN_COURS pour ce livreur
        return deliveryTourRepository.existsByDriverAndStatus(driver, DeliveryTourStatus.EN_COURS);
    }

    //--------------------------------Confirmer la récupération d'une commande par le livreur (un swipe = une commande)--------------------------------
    @Override
    @Transactional
    public void confirmPickup(Long tourId, Long orderId) {
        // 1. Charger la tournée
        DeliveryTour tour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tournée introuvable"));

        // 2. Récupérer le livreur connecté
        Users currentUser = getCurrentUser();
        Driver driver = getDriverOrThrow();

        // 3. Vérifier que le livreur est bien assigné à cette tournée
        if (!tour.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette tournée");
        }

        // 4. Si la tournée est encore ASSIGNEE, vérifier qu'il n'a pas déjà une autre tournée en cours
        if (tour.getStatus() == DeliveryTourStatus.ASSIGNEE && hasActiveTour()) {
            throw new RuntimeException(
                    "Vous avez déjà une tournée en cours. " +
                            "Veuillez la terminer avant d'en commencer une nouvelle.");
        }

        // 5. Vérifier que la tournée accepte encore des confirmations : ASSIGNEE (1er swipe) ou EN_COURS (swipes suivants pour les autres commandes)
        if (tour.getStatus() != DeliveryTourStatus.ASSIGNEE && tour.getStatus() != DeliveryTourStatus.EN_COURS) {
            throw new RuntimeException("Cette tournée est terminée ou annulée ; aucune confirmation de récupération possible");
        }

        // 6. Charger la commande et vérifier qu'elle appartient à cette tournée
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getId().equals(tour.getId())) {
            throw new RuntimeException("Cette commande n'appartient pas à cette tournée");
        }

        // 7. Vérifier que la commande est bien VALIDEE (sinon déjà récupérée ou plus avancée)
        if (order.getStatus() != OrderStatus.VALIDEE) {
            throw new RuntimeException("Cette commande n'est plus en attente de récupération");
        }

        // 8. Passer la commande en EN_PREPARATION et enregistrer l'heure de récupération
        order.setStatus(OrderStatus.EN_PREPARATION);
        order.setPickupStartedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 9. Notifier le salarié que le livreur a récupéré sa commande
        employeeNotificationService.notifyPickupConfirmed(order);
        
        //     On vient d'en mettre une en EN_PREPARATION → s'il n'y en a qu'une au total, c'est la première :
        //     on passe la tournée en EN_COURS et on notifie le RL. Sinon (déjà d'autres en préparation), on ne fait que sauvegarder.
        long nbDejaRecuperees = tour.getOrders().stream()
                .filter(o -> o.getStatus() != OrderStatus.VALIDEE)
                .count();
        if (nbDejaRecuperees == 1) {
            tour.setStatus(DeliveryTourStatus.EN_COURS);
            tour.setStartedAt(LocalDateTime.now());
            deliveryTourRepository.save(tour);
            driverNotificationService.notifyLogisticsManagerTourStarted(tour);
            log.info("Tournée {} démarrée par {} (première récupération)", tour.getTourNumber(), currentUser.getEmail());
        } else {
            deliveryTourRepository.save(tour);
            log.info("Récupération commande {} confirmée par {} pour la tournée {}", order.getOrderNumber(), currentUser.getEmail(), tour.getTourNumber());
        }
    }

    //--------------------------------DÉMARRER LIVRAISON d'une commande spécifique--------------------------------

    @Override
    @Transactional
    public void startDelivery(Long orderId) {
        // 1. Charger la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Récupérer le livreur connecté
        Users currentUser = getCurrentUser();
        Driver driver = getDriverOrThrow();

        // 3. Vérifier que la commande appartient à une tournée assignée au livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 4. Vérifier que la commande est en EN_PREPARATION
        if (order.getStatus() != OrderStatus.EN_PREPARATION) {
            throw new RuntimeException("Seule une commande En préparation peut être démarrée");
        }

        // 5. Passer la commande en EN_COURS et enregistrer l'heure de départ
        order.setStatus(OrderStatus.EN_COURS);
        order.setDeliveryStartedAt(LocalDateTime.now());

        // 6. Sauvegarder la commande
        orderRepository.save(order);

        log.info("Livraison {} démarrée par {}", order.getOrderNumber(), currentUser.getEmail());
    }

    //--------------------------------📍 CONFIRMER ARRIVÉE--------------------------------

    @Override
    @Transactional
    public void confirmArrival(Long orderId) {
        // 1. Charger la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();

        // 3. Vérifier que la commande appartient à une tournée assignée au livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 4. Vérifier que la commande est en EN_COURS
        if (order.getStatus() != OrderStatus.EN_COURS) {
            throw new RuntimeException("Seule une livraison En cours peut confirmer l'arrivée");
        }

        // 5. Passer la commande en ARRIVE et enregistrer l'heure d'arrivée
        order.setStatus(OrderStatus.ARRIVE);
        order.setDeliveryArrivedAt(LocalDateTime.now());

        // 6. Sauvegarder la commande
        orderRepository.save(order);
        log.info("Arrivée confirmée pour {}", order.getOrderNumber());
    }

    //--------------------------------✅ FINALISER LIVRAISON--------------------------------

    @Override
    @Transactional
    public void completeDelivery(Long orderId) {
        // 1. Charger la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();

        // 3. Vérifier que la commande appartient à une tournée assignée au livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 4. Vérifier que la commande est au statut ARRIVE
        if (order.getStatus() != OrderStatus.ARRIVE) {
            throw new RuntimeException("Seule une livraison avec statut Arrivé peut être finalisée");
        }

        // 5. Vérifier que la commande est payée
        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("La commande doit être payée avant finalisation");
        }

        // 6. Passer la commande en LIVREE et enregistrer l'heure de remise
        order.setStatus(OrderStatus.LIVREE);
        order.setDeliveryCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 6bis. Créditer le compte livreur (tarif par livraison, ex. 500 F)
        creditDriverEarning(driver, order);

        // 7. Vérifier si toutes les commandes de la tournée sont dans un état final (LIVREE ou ECHEC_LIVRAISON)
        DeliveryTour tour = order.getDeliveryTour();
        checkTourCompletion(tour);

        log.info("Livraison {} finalisée", order.getOrderNumber());
    }

    //--------------------------------CONFIRMER PAIEMENT ESPÈCES--------------------------------

    @Override
    @Transactional
    public void confirmCashPayment(Long orderId) {
        // 1. Récupérer le livreur connecté et charger la commande
        Driver driver = getDriverOrThrow();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Vérifier que la commande appartient à une tournée assignée au livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 3. Récupérer ou créer le paiement
        Payment payment = order.getPayment();

        // 4. Vérifier que la commande n'est pas déjà payée
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Cette commande est déjà payée");
        }

        // 5. Créer un nouveau paiement si absent
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod(PaymentMethodType.CASH);
            payment.setPaymentTiming(PaymentTimingType.ON_DELIVERY);
        }

        // 6. Générer la référence, marquer payé et enregistrer l'heure
        String ref = "ESP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        payment.setTransactionReference(ref);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        order.setPayment(payment);
        log.info("💵 Paiement en espèces confirmé par le livreur pour commande {} - Ref: {}", order.getOrderNumber(), ref);
    }

    // ========================================
    // DÉTAIL D'UNE LIVRAISON (écran livreur)
    // ========================================

    @Override
    public DeliveryDetailDTO getDeliveryDetail(Long orderId) {
        // 1. Récupérer la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Vérifier que le livreur connecté est assigné à cette commande
        Driver driver = getDriverOrThrow();
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 3. Créer le DTO et remplir les infos principales
        DeliveryDetailDTO dto = new DeliveryDetailDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatusLabel(order.getStatus() != null ? order.getStatus().getLabel() : "");

        // 4. Infos client (employé)
        Employee employee = order.getEmployee();
        if (employee != null && employee.getUser() != null) {
            String first = employee.getUser().getFirstName() != null ? employee.getUser().getFirstName() : "";
            String last = employee.getUser().getLastName() != null ? employee.getUser().getLastName() : "";
            dto.setCustomerName((first + " " + last).trim());
            dto.setEmployeeId("ID: " + employee.getId());
            dto.setCustomerPhone(employee.getUser().getPhone());
            dto.setPhoto(employee.getUser().getProfilePhotoUrl());
        }

        // 5. Adresse de livraison (texte + lat/long)
        dto.setDeliveryAddress(getDeliveryAddressFromOrder(order));
        Address primaryAddress = getPrimaryAddressFromOrder(order);
        if (primaryAddress != null) {
            dto.setDeliveryLatitude(primaryAddress.getLatitude() != null ? primaryAddress.getLatitude().doubleValue() : null);//On récupère la latitude de l'adresse principale convertie en double
            dto.setDeliveryLongitude(primaryAddress.getLongitude() != null ? primaryAddress.getLongitude().doubleValue() : null);//On récupère la longitude de l'adresse principale convertie en double
        }

        // 6. Montant total
        dto.setTotalAmount(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);

        return dto;
    }

    /**
     * Récupère l'adresse principale de l'employé de la commande (pour lat/long).
     */
    private Address getPrimaryAddressFromOrder(Order order) {
        if (order.getEmployee() == null || order.getEmployee().getAddresses() == null || order.getEmployee().getAddresses().isEmpty()) {
            return null;
        }
        return order.getEmployee().getAddresses().stream()
                .filter(Address::isPrimary)
                .findFirst()
                .orElse(order.getEmployee().getAddresses().stream().findFirst().orElse(null));
    }

 
    //--------------------------------SIGNALER UN PROBLÈME (échec livraison)--------------------------------
    @Override
    @Transactional
    public void reportDeliveryIssue(Long orderId, DeliveryIssueDTO dto) {
        // 1. Charger la commande ou lever une exception si introuvable
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // 2. Vérifier que le livreur connecté est bien assigné à la tournée de cette commande
        Driver driver = getDriverOrThrow();
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Vous n'êtes pas assigné à cette commande");
        }

        // 3. Vérifier que la commande est en préparation ou en cours (EN_PREPARATION, EN_COURS ou ARRIVE)
        if (order.getStatus() != OrderStatus.EN_PREPARATION && order.getStatus() != OrderStatus.EN_COURS && order.getStatus() != OrderStatus.ARRIVE) {
            throw new RuntimeException("Seule une livraison en préparation ou en cours peut être signalée");
        }

        // 1. Créer le signalement
        DeliveryIssueReport report = new DeliveryIssueReport();
        report.setOrder(order);
        report.setReportedBy(driver.getUser());
        report.setReportSource(DeliveryIssueReportSource.DRIVER);
        report.setReason(dto.getReason() != null ? dto.getReason().getLabel() : "Non précisée");
        report.setComment(dto.getComment());
        deliveryIssueReportRepository.save(report);

        // 2. Mettre la commande en échec
        order.setStatus(OrderStatus.ECHEC_LIVRAISON);
        order.setFailureReason(dto.getReason().getLabel());
        order.setFailureReportedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 3. Notifier le salarié
        employeeNotificationService.notifyDeliveryFailed(order, dto.getReason().getLabel());


        // 4. Notifier le RL
        Users rl = order.getDeliveryTour() != null ? order.getDeliveryTour().getCreatedBy() : null;
        if (rl != null && rl.getEmail() != null && !rl.getEmail().isBlank()) {
            String deliveryAddress = getDeliveryAddressFromOrder(order);
            driverNotificationService.notifyLogisticsManagerOfDeliveryFailure(
                    order,
                    dto.getReason().getLabel(),
                    dto.getComment() != null ? dto.getComment() : "",
                    deliveryAddress != null ? deliveryAddress : "Non renseignée"
            );
        }

        // 5. Vérifier fin de tournée
        if (order.getDeliveryTour() != null) {
            checkTourCompletion(order.getDeliveryTour());
        }

        log.info("Échec livraison {} signalé par {} : {}", order.getOrderNumber(), DeliveryIssueReportSource.DRIVER,dto.getReason().getLabel());
    }

    // ========================================
    // TABLEAU DE BORD LIVREUR
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public DriverDashboardDTO getDashboard(String period) {
        Driver driver = getDriverOrThrow(); //On récupère le livreur connecté
        Users user = driver.getUser();//On récupère l'utilisateur connecté
        LocalDate today = LocalDate.now();//on récupère la date d'aujourd'hui
        LocalDateTime dayStart = today.atStartOfDay();//on récupère le début de la journée
        LocalDateTime dayEnd = today.atTime(23, 59, 59, 999_999_999);//on récupère la fin de la journée

        // Livraisons aujourd'hui : celles effectivement complétées aujourd'hui
        long livraisonsAujourdhui = orderRepository.countByDeliveryTourDriverIdAndStatusAndDeliveryCompletedAtBetween(
                driver.getId(), OrderStatus.LIVREE, dayStart, dayEnd);//on compte le nombre de livraisons effectivement complétées aujourd'hui
        long totalLivraisons = orderRepository.countByDeliveryTourDriverIdAndStatus(
                driver.getId(), OrderStatus.LIVREE);//on compte le nombre total de livraisons

        // Gains aujourd'hui (somme des DriverEarning du jour)
        BigDecimal gainsAujourdhui = driverEarningRepository.sumAmountByDriverIdAndEarnedAtBetween(
                driver.getId(), dayStart, dayEnd);//on récupère la somme des gains aujourd'hui
        BigDecimal tarifParLivraison = feeService.getDriverRatePerDelivery();

        Double satisfactionMoyenne = driverReviewRepository.getAverageRatingByDriverId(driver.getId());

        // Performances : selon le filtre (SEMAINE=4 semaines, MOIS=S1-S4, ANNEE=12 mois)
        List<DriverPerformanceItemDTO> performances = buildPerformances(driver.getId(), period);

        DriverDashboardDTO dto = new DriverDashboardDTO();
        dto.setFirstName(user != null ? user.getFirstName() : null);
        dto.setLastName(user != null ? user.getLastName() : null);
        dto.setPhotoUrl(user != null ? user.getProfilePhotoUrl() : null);
        // En ligne / Hors ligne = user.isActive (actif/inactif)
        dto.setIsOnline(user != null && Boolean.TRUE.equals(user.getIsActive()));
        // Véhicule = celui de la tournée assignée (vehicleTypePlate de la tournée en cours ou assignée)
        dto.setVehicleType(getVehicleTypeFromAssignedTour(driver));
        dto.setLivraisonsAujourdhui(livraisonsAujourdhui);
        dto.setTotalLivraisons(totalLivraisons);
        dto.setGainsAujourdhui(gainsAujourdhui != null ? gainsAujourdhui : BigDecimal.ZERO);
        dto.setTarifParLivraison(tarifParLivraison != null ? tarifParLivraison : BigDecimal.ZERO);
        dto.setSatisfactionMoyenne(satisfactionMoyenne);
        dto.setPerformances(performances);
        return dto;
    }

    /**
     * Construit les données du graphique "Performances" selon le filtre choisi.
     * Chaque point = nombre de livraisons (DriverEarning) du livreur sur la période.
     *
     * @param driverId ID du livreur
     * @param period   SEMAINE | MOIS | ANNEE (insensible à la casse, défaut MOIS si invalide)
     * @return Liste de { label, count } pour affichage (1 barre orange par point = ses livraisons uniquement)
     */
    private List<DriverPerformanceItemDTO> buildPerformances(Long driverId, String period) {
        String p = (period != null) ? period.toUpperCase() : "MOIS";
        if ("SEMAINE".equals(p)) {
            return buildPerformancesSemaine(driverId);
        }
        if ("ANNEE".equals(p)) {
            return buildPerformancesAnnee(driverId);
        }
        // MOIS par défaut
        return buildPerformancesMois(driverId);
    }

    /**
     * SEMAINE : 4 dernières semaines (lundi → dimanche).
     * S1 = il y a 3 semaines, S2 = il y a 2 sem, S3 = semaine dernière, S4 = semaine courante.
     */
    private List<DriverPerformanceItemDTO> buildPerformancesSemaine(Long driverId) {
        LocalDate today = LocalDate.now();
        // Lundi de la semaine courante (si aujourd'hui = lundi, on prend aujourd'hui)
        LocalDate mondayThisWeek = today.with(DayOfWeek.MONDAY);
        List<DriverPerformanceItemDTO> list = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            // On remonte de i semaines : semaine courante (i=0), -1, -2, -3
            LocalDate weekStart = mondayThisWeek.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.atTime(23, 59, 59, 999_999_999);
            long count = driverEarningRepository.countByDriverIdAndEarnedAtBetween(driverId, start, end);
            list.add(new DriverPerformanceItemDTO("S" + (4 - i), count));
        }
        return list;
    }

    /**
     * MOIS : S1, S2, S3, S4 du mois en cours.
     * Découpage par tranches de 7 jours : S1 = jours 1-7, S2 = 8-14, S3 = 15-21, S4 = 22-fin du mois.
     */
    private List<DriverPerformanceItemDTO> buildPerformancesMois(Long driverId) {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = LocalDate.of(now.getYear(), now.getMonth(), 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        List<DriverPerformanceItemDTO> list = new ArrayList<>();
        int weekNum = 1;
        LocalDate curr = firstDay;
        while (!curr.isAfter(lastDay)) {
            // Fin de la tranche = curr + 6 jours, ou dernier jour du mois si on dépasse
            LocalDate chunkEnd = curr.plusDays(6);
            if (chunkEnd.isAfter(lastDay)) chunkEnd = lastDay;
            LocalDateTime start = curr.atStartOfDay();
            LocalDateTime end = chunkEnd.atTime(23, 59, 59, 999_999_999);
            long count = driverEarningRepository.countByDriverIdAndEarnedAtBetween(driverId, start, end);
            list.add(new DriverPerformanceItemDTO("S" + weekNum, count));
            curr = curr.plusDays(7);
            weekNum++;
        }
        return list;
    }

    /**
     * ANNEE : 12 mois (Jan, Fév, ... Déc) de l'année en cours.
     * Chaque point = nombre de livraisons du livreur ce mois-là.
     */
    private List<DriverPerformanceItemDTO> buildPerformancesAnnee(Long driverId) {
        int year = LocalDate.now().getYear();
        String[] moisLabels = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};
        List<DriverPerformanceItemDTO> list = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate firstDay = LocalDate.of(year, m, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            LocalDateTime start = firstDay.atStartOfDay();
            LocalDateTime end = lastDay.atTime(23, 59, 59, 999_999_999);
            long count = driverEarningRepository.countByDriverIdAndEarnedAtBetween(driverId, start, end);
            list.add(new DriverPerformanceItemDTO(moisLabels[m - 1], count));
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════
    // ___________GAINS_________________
    // Méthodes liées au tarif livreur et aux gains par livraison
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Crédite le compte du livreur quand une commande passe en LIVREE.
     * Montant = tarif par livraison (configuré par l'admin via frais "Tarif livreur").
     */
    private void creditDriverEarning(Driver driver, Order order) {
        BigDecimal rate = feeService.getDriverRatePerDelivery();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return;
        DriverEarning earning = new DriverEarning();
        earning.setDriver(driver);
        earning.setOrder(order);
        earning.setAmount(rate);
        driverEarningRepository.save(earning);
        log.info("Gain livreur +{} F pour livraison {}", rate, order.getOrderNumber());
    }

    /** Récupère le véhicule de la tournée assignée au livreur (ASSIGNEE ou EN_COURS). */
    private String getVehicleTypeFromAssignedTour(Driver driver) {
        return deliveryTourRepository.findFirstByDriverAndStatusInOrderByCreatedAtDesc(
                        driver, List.of(DeliveryTourStatus.ASSIGNEE, DeliveryTourStatus.EN_COURS))
                .map(DeliveryTour::getVehicleTypePlate)
                .orElse(null);
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    /**
     * Récupérer le créneau horaire
     */
    private String getTimeSlot(Order order) {
        EmployeeDeliveryPreference pref =
                order.getEmployee().getEmployeeDeliveryPreference();

        if (pref != null && pref.getPreferredTimeSlot() != null) {
            return pref.getPreferredTimeSlot().getDisplayName();
        }

        return "Toute la journée";
    }
    /**
     * Vérifie si la tournée est terminée (toutes les commandes en état final : LIVREE ou ECHEC_LIVRAISON).
     * Si oui, passe la tournée en TERMINEE et enregistre l'heure de fin.
     */
    private void checkTourCompletion(DeliveryTour tour) {
        if (tour == null || tour.getOrders() == null) return;
        long totalOrders = tour.getOrders().size();
        long finalOrders = tour.getOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.LIVREE || o.getStatus() == OrderStatus.ECHEC_LIVRAISON)
                .count();
        if (totalOrders == finalOrders) {
            tour.setStatus(DeliveryTourStatus.TERMINEE);
            tour.setCompletedAt(LocalDateTime.now());
            deliveryTourRepository.save(tour);
            log.info("Tournée {} terminée", tour.getTourNumber());
        }
    }

    /**
     * Mappe une commande vers DriverDeliveryCardDTO (carte liste livreur).
     */
    private DriverDeliveryCardDTO mapToDriverDeliveryCardDTO(Order order) {
        
        DriverDeliveryCardDTO dto = new DriverDeliveryCardDTO();//On crée un nouveau DriverDeliveryCardDTO

        dto.setOrderId(order.getId());//Id de la commande
        dto.setTourId(order.getDeliveryTour().getId());
        dto.setOrderNumber(order.getOrderNumber());//Numéro de la commande
        dto.setStatusLabel(order.getStatus() != null ? order.getStatus().getLabel() : null);//Statut de la commande
        dto.setDeliveryDate(order.getDeliveryDate());//Date de livraison estimée
        dto.setTimeSlot(getTimeSlot(order));//créneau horaire
        //On récupère le nom du client et le nom de la société
        if (order.getEmployee() != null) {
            dto.setCustomerName(
        
                    (order.getEmployee().getUser().getFirstName() != null ? order.getEmployee().getUser().getFirstName() : "")
                            + " "
                            + (order.getEmployee().getUser().getLastName() != null ? order.getEmployee().getUser().getLastName() : ""));
            if (order.getEmployee().getCompany() != null) {
                dto.setCompanyName(order.getEmployee().getCompany().getName());
            }
        }

        String formattedAddr = getDeliveryAddressFromOrder(order);//On récupère l'adresse de livraison
        dto.setFormattedAddress(formattedAddr);//On set l'adresse de livraison formatée
       

        if (order.getEmployee() != null && order.getEmployee().getAddresses() != null && !order.getEmployee().getAddresses().isEmpty()) {
            Address addr = order.getEmployee().getAddresses().stream()
                    .filter(Address::isPrimary)//on filtre pour avoir que les adresses principales (même si c'est une seule parmi toutes les adresses)
                    .findFirst()//on récupère la première adresse principale
                    .orElse(order.getEmployee().getAddresses().get(0));//sinon on prend la 1 ere adresse de l'employé
            dto.setLatitude(addr.getLatitude() != null ? addr.getLatitude().doubleValue() : null);
            dto.setLongitude(addr.getLongitude() != null ? addr.getLongitude().doubleValue() : null);
        }
        return dto;//On retourne le DriverDeliveryCardDTO
    }


    /**
     * Récupère l'utilisateur actuellement connecté
     * @return Users l'utilisateur connecté
     * @throws RuntimeException si aucun utilisateur n'est authentifié
     */
    private Users getCurrentUser() {

        // 1. Récupérer l'authentification Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Vérifier que l'utilisateur est bien authentifié
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        // 3. Récupérer l'email (username) de l'utilisateur
        String userEmail = authentication.getName();

        // 4. Chercher l'utilisateur dans la base de données
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable avec email: " + userEmail
                ));
    }
    /**
     * Récupère l'adresse de livraison (texte) à partir de l'adresse principale de l'employé de la commande.
     */
    private String getDeliveryAddressFromOrder(Order order) {
        if (order.getEmployee() == null || order.getEmployee().getAddresses() == null || order.getEmployee().getAddresses().isEmpty()) {
            return null;
        }
        Address addr = order.getEmployee().getAddresses().stream()
                .filter(Address::isPrimary)//on filtre pour avoir que les adresses principales (même si c'est une seule parmi toutes les adresses)
                .findFirst()//on récupère la première adresse principale
                .orElse(null);
        if (addr == null) return null;//si il n'y en a pas, on retourne null
        return (addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank()) ? addr.getFormattedAddress() : null;
    }

    private Driver getDriverOrThrow() {
        Users user = getCurrentUser();
        return deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));
    }

}
