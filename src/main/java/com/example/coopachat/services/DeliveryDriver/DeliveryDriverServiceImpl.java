package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDashboardDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.driver.DeliveryDetailDTO;
import com.example.coopachat.dtos.driver.DeliveryIssueDTO;
import com.example.coopachat.dtos.driver.DriverDeliveryCardDTO;
import com.example.coopachat.dtos.driver.DriverDeliveriesResponseDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentMethodType;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.PaymentTimingType;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.Employee.EmployeeNotificationService;
import com.example.coopachat.services.fee.FeeService;
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
        // 3. Retourner nom, prénom, téléphone, email
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
        // 1. Récupérer l'utilisateur connecté
        Users user = getCurrentUser();
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

    //--------------------------------Confirmer la récupération des commandes par le livreur--------------------------------
    @Override
    @Transactional
    public void confirmPickup(Long tourId) {
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

        // 4. Vérifier qu'il n'a pas déjà une tournée en cours
        if (hasActiveTour()) {
            throw new RuntimeException(
                    "Vous avez déjà une tournée en cours. " +
                            "Veuillez la terminer avant d'en commencer une nouvelle.");
        }

        // 5. Vérifier que la tournée est au statut ASSIGNEE
        if (tour.getStatus() != DeliveryTourStatus.ASSIGNEE) {
            throw new RuntimeException("Cette tournée ne peut pas être démarrée");
        }

        // 6. Passer la tournée en EN_COURS et enregistrer l'heure de démarrage
        tour.setStatus(DeliveryTourStatus.EN_COURS);
        tour.setStartedAt(LocalDateTime.now());

        // 7. Passer toutes les commandes VALIDEE en EN_PREPARATION
        for (Order order : tour.getOrders()) {
            if (order.getStatus() == OrderStatus.VALIDEE) {
                order.setStatus(OrderStatus.EN_PREPARATION);

                orderRepository.save(order);
            }
        }

        // 8. Sauvegarder la tournée
        deliveryTourRepository.save(tour);

        log.info("Tournée {} démarrée par {}", tour.getTourNumber(), currentUser.getEmail());
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

        // 3. Vérifier que la commande est en cours de livraison (EN_COURS ou ARRIVE)
        if (order.getStatus() != OrderStatus.EN_COURS && order.getStatus() != OrderStatus.ARRIVE) {
            throw new RuntimeException("Seule une livraison en cours peut être signalée");
        }

        // 4. Passer la commande en échec : statut, raison (libellé), date du signalement, sauvegarde
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

        // 7. Vérifier si la tournée est terminée (toutes commandes livrées ou en échec) → TERMINEE si oui
        checkTourCompletion(order.getDeliveryTour());
        log.info("Échec livraison {} signalé : {}", order.getOrderNumber(), reasonLabel);
    }

    // ========================================
    // TABLEAU DE BORD LIVREUR
    // ========================================
    /** Livraisons aujourd'hui, total livraisons (statut LIVREE), et moyenne des notes des avis clients. */

    @Override
    @Transactional(readOnly = true)
    public DriverDashboardDTO getDashboard() {
        // 1. Récupérer le livreur connecté
        Driver driver = getDriverOrThrow();
        LocalDate today = LocalDate.now();

        // 2. Nombre de commandes livrées aujourd'hui par ce livreur
        long livraisonsAujourdhui = orderRepository.countByDeliveryTourDriverIdAndStatusAndDeliveryDate(
                driver.getId(), OrderStatus.LIVREE, today);

        // 3. Nombre total de commandes livrées par ce livreur (toutes dates)
        long totalLivraisons = orderRepository.countByDeliveryTourDriverIdAndStatus(
                driver.getId(), OrderStatus.LIVREE);

        // 4. Moyenne des notes (1 à 5) des avis clients ; null si aucun avis
        Double satisfactionMoyenne = driverReviewRepository.getAverageRatingByDriverId(driver.getId());

        return new DriverDashboardDTO(livraisonsAujourdhui, totalLivraisons, satisfactionMoyenne);
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
