package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverAddressDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverDeliveryListItemDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.dtos.DeliveryDriver.OrderDetailsForDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.OrderItemForDriverDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.enums.PaymentMethodType;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.enums.PaymentTimingType;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.fee.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    //---------------------- Récupère les informations personnelles du livreur-----------
    @Override
    public DriverPersonalInfoDTO getPersonalInfo() {

        Users user = getCurrentUser();

        // Récupérer le livreur associé à cet utilisateur
       Driver driver = deliveryDriverRepository.findByUser(user)
               .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        return new DriverPersonalInfoDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail()
        );
    }
    //---------------------- Met à jour les informations personnelles du livreur-----------
    @Override
    @Transactional
    public void updatePersonalInfo(DriverPersonalInfoDTO updateRequest) {

        Users user = getCurrentUser();

        // Mettre à jour uniquement les champs autorisés
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }

        userRepository.save(user);
        log.info("Mise à jour réussie pour le livreur : {} {}",
                user.getFirstName(), user.getLastName());
    }
    //---------------------- Récupère les livraisons du livreur-----------

    @Override
    public List<DriverDeliveryListItemDTO> getMyDeliveries(LocalDate deliveryDate, OrderStatus status, String search) {
        Users user = getCurrentUser();
        //Récupérer le livreur associé à cet utilisateur
        Driver driver = deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));

        //Normaliser le terme de recherche si non null et non vide
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;


         List<Order> orders = orderRepository.findDriverDeliveries(driver.getId(), deliveryDate, status, searchTerm);


        return orders.stream()
            .map(this::mapOrderToDriverDeliveryListItemDTO)
            .toList();
    }

    //---------------------- Confirme la récupération d'une tournée-----------
    @Override
    @Transactional
    public void confirmPickup(Long tourId) {
        Driver driver = getDriverOrThrow();
        DeliveryTour tour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tournée introuvable"));
        if (!tour.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette tournée ne vous est pas assignée");
        }
        if (tour.getStatus() != DeliveryTourStatus.ASSIGNEE) {
            throw new RuntimeException("Seule une tournée au statut Assignée peut être confirmée en récupération");
        }
        tour.setStatus(DeliveryTourStatus.EN_COURS);
        tour.setStartedAt(LocalDateTime.now());
        deliveryTourRepository.save(tour);
        log.info("Livreur {} a confirmé la récupération pour la tournée {}", driver.getUser().getEmail(), tour.getTourNumber());
    }

    //---------------------- Démarre la livraison d'une commande-----------
    @Override
    @Transactional
    public void startDelivery(Long orderId) {
        Driver driver = getDriverOrThrow();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette commande ne fait pas partie de vos livraisons");
        }
        if (order.getDeliveryTour().getStatus() != DeliveryTourStatus.EN_COURS) {
            throw new RuntimeException("La tournée doit être en cours (récupération confirmée)");
        }
        // On ne démarre que si la commande est validée (mise en tournée par le RL)
        if (order.getStatus() != OrderStatus.VALIDEE) {
            throw new RuntimeException("Seule une commande validée peut être démarrée");
        }
        order.setStatus(OrderStatus.EN_COURS);
        order.setDeliveryStartedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Livreur a démarré la livraison pour la commande {}", order.getOrderNumber());
    }
    //---------------------- Confirme l'arrivée d'une commande-----------

    @Override
    @Transactional
    public void confirmArrival(Long orderId) {
        Driver driver = getDriverOrThrow();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette commande ne fait pas partie de vos livraisons");
        }
        if (order.getStatus() != OrderStatus.EN_COURS) {
            throw new RuntimeException("La commande doit être En cours pour confirmer l'arrivée");
        }
        order.setStatus(OrderStatus.ARRIVE);
        order.setDeliveryArrivedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Livreur a confirmé l'arrivée pour la commande {}", order.getOrderNumber());
    }
    //---------------------- Finalise la livraison d'une commande-----------

    @Override
    @Transactional
    public void completeDelivery(Long orderId) {
        // 1. Vérifier que l'appelant est bien le livreur connecté
        Driver driver = getDriverOrThrow();
        // 2. Charger la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        // 3. S'assurer que la commande appartient à une tournée assignée à ce livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette commande ne fait pas partie de vos livraisons");
        }
        // 4. La commande doit être au statut "Arrivé" pour pouvoir finaliser
        if (order.getStatus() != OrderStatus.ARRIVE) {
            throw new RuntimeException("La commande doit être Arrivé pour finaliser la livraison");
        }
        // 5. Vérifier le statut de paiement : si un paiement existe et n'est pas payé, le livreur doit d'abord confirmer (ex. espèces)
        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("La commande doit être payée avant de finaliser la livraison. Confirmez le paiement en espèces si applicable.");
        }
        // 6. Marquer la commande comme livrée et enregistrer l'heure
        order.setStatus(OrderStatus.LIVREE);
        order.setDeliveryCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        // 7. Vérifier si toutes les commandes de la tournée sont maintenant livrées
        DeliveryTour tour = order.getDeliveryTour();
        boolean allDelivered = tour.getOrders().stream().allMatch(o -> o.getStatus() == OrderStatus.LIVREE);
        // 8. Si oui, clôturer la tournée et enregistrer l'heure de fin
        if (allDelivered) {
            tour.setStatus(DeliveryTourStatus.TERMINEE);
            tour.setCompletedAt(LocalDateTime.now());
            deliveryTourRepository.save(tour);
            log.info("Tournée {} terminée (toutes les commandes livrées)", tour.getTourNumber());
        }
        log.info("Livraison finalisée pour la commande {}", order.getOrderNumber());
    }

    //---------------------- Confirme un paiement en espèces pour une commande-----------
    @Override
    @Transactional
    public void confirmCashPayment(Long orderId) {

        Driver driver = getDriverOrThrow();//On récupère le livreur connecté

        //On récupère la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        //On vérifie si la commande fait partie de la tournée du livreur connecté
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette commande ne fait pas partie de vos livraisons");
        }

        //On récupère le paiement de la commande
        Payment payment = order.getPayment();

        //On vérifie si le paiement est déjà payé
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Cette commande est déjà payée");
        }
        //Si le paiement est nul, on en crée un nouveau
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod(PaymentMethodType.CASH);
            payment.setPaymentTiming(PaymentTimingType.ON_DELIVERY);
        }
        //On génère une référence de paiement 
        String ref = "ESP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        payment.setTransactionReference(ref);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        order.setPayment(payment);
        log.info("💵 Paiement en espèces confirmé par le livreur pour commande {} - Ref: {}", order.getOrderNumber(), ref);
    }

    //---------------------- Récupère les détails d'une commande pour le livreur-----------
    @Override
    public OrderDetailsForDriverDTO getOrderDetails(Long orderId) {
        // 1. S'assurer que l'appelant est le livreur connecté
        Driver driver = getDriverOrThrow();
        // 2. Charger la commande (avec les lignes et le produit pour chaque ligne)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        // 3. Vérifier que cette commande fait partie d'une tournée assignée à ce livreur
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Cette commande ne fait pas partie de vos livraisons");
        }

        // 4. Date de commande (date de création)
        LocalDate orderDate = order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null;

        // 5. Libellé du statut pour l'affichage (ex. "Livrée", "En cours")
        String statusLabel = order.getStatus() != null ? order.getStatus().getLabel() : "";

        // 6. Liste des produits commandés (nom, quantité, prix unitaire, image)
        List<OrderItemForDriverDTO> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct() != null ? item.getProduct().getName() : "";
                BigDecimal unitPrice = item.getPromoPrice() != null ? item.getPromoPrice() : item.getUnitPrice();
                String imageUrl = item.getProduct() != null ? item.getProduct().getImage() : null;
                items.add(new OrderItemForDriverDTO(productName, item.getQuantity(), unitPrice, imageUrl));
            }
        }

        // 7. Nom du client (employé qui a passé la commande)
        String clientName = order.getEmployee() != null && order.getEmployee().getUser() != null
                ? order.getEmployee().getUser().getFirstName() + " " + order.getEmployee().getUser().getLastName()
                : "";

        // 8. Adresse de livraison (adresse principale de l'employé)
        String deliveryAddress = getDeliveryAddressFromOrder(order);

        // 9. Construire le DTO avec toutes les infos + timeline (createdAt, deliveryStartedAt, etc.)
        BigDecimal productsTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;//On récupère le sous-total de la commande
        BigDecimal deliveryAmount = feeService.calculateTotalFees();//On calcule les frais de service
        if (deliveryAmount == null) deliveryAmount = BigDecimal.ZERO;//Si les frais de service sont nuls, on met 0
        BigDecimal totalToReceive = productsTotal.add(deliveryAmount);//On calcule le total à percevoir

        OrderDetailsForDriverDTO dto = new OrderDetailsForDriverDTO();
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(orderDate);
        dto.setStatusLabel(statusLabel);
        dto.setProductCount(order.getItems() != null ? order.getItems().size() : 0);
        dto.setProductsTotal(productsTotal);
        dto.setDeliveryAmount(deliveryAmount);
        dto.setTotalAmount(totalToReceive);
        dto.setItems(items);
        dto.setClientName(clientName);
        dto.setDeliveryAddress(deliveryAddress);
        dto.setDeliveryDate(order.getDeliveryDate());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setValidatedAt(order.getValidatedAt());
        dto.setDeliveryStartedAt(order.getDeliveryStartedAt());
        dto.setDeliveryArrivedAt(order.getDeliveryArrivedAt());
        dto.setDeliveryCompletedAt(order.getDeliveryCompletedAt());
        // 10. Type de paiement et statut (pour "Informations de paiement" et vérification avant finalisation)
        if (order.getPayment() != null) {
            dto.setPaymentMethodLabel(order.getPayment().getPaymentMethod() != null ? order.getPayment().getPaymentMethod().getLabel() : null);
            dto.setPaymentStatusLabel(order.getPayment().getStatus() != null ? order.getPayment().getStatus().getLabel() : "Impayé");
        } else {
            dto.setPaymentMethodLabel(null);
            dto.setPaymentStatusLabel("Impayé");
        }
        return dto;
    }

    //---------------------- Récupère l'adresse du livreur-----------
    @Override
    @Transactional(readOnly = true)
    public DriverAddressDTO getMyAddress() {
        Driver driver = getDriverOrThrow();
        return new DriverAddressDTO(
                driver.getFormattedAddress(),
                driver.getLatitude(),
                driver.getLongitude()
        );
    }

    //---------------------- Met à jour l'adresse du livreur-----------
    @Override
    @Transactional
    public void updateMyAddress(DriverAddressDTO dto) {
        Driver driver = getDriverOrThrow();
        driver.setFormattedAddress(dto.getFormattedAddress());
        driver.setLatitude(dto.getLatitude());
        driver.setLongitude(dto.getLongitude());
        deliveryDriverRepository.save(driver);
        log.info("Adresse livreur mise à jour pour {}", driver.getUser().getEmail());
    }

//---------------- Les méthodes Utilitaires -------------------

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
                .filter(Address::isPrimary)
                .findFirst()
                .orElse(null);
        if (addr == null) return null;
        return (addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank()) ? addr.getFormattedAddress() : null;
    }

    private Driver getDriverOrThrow() {
        Users user = getCurrentUser();
        return deliveryDriverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé"));
    }

    /**
     * Mappe une commande vers le DTO "Mes livraisons" (adresse = adresse principale de l'employé).
     */
    private DriverDeliveryListItemDTO mapOrderToDriverDeliveryListItemDTO(Order order) {
        //Récupérer le nom du client
        String clientName = order.getEmployee().getUser().getFirstName() + " " + order.getEmployee().getUser().getLastName();
        //Récupérer l'adresse de livraison
        String address = null;
        //Récupérer la latitude de l'adresse de livraison
        Double latitude = null;
        //Récupérer la longitude de l'adresse de livraison
        Double longitude = null;

        // Une seule adresse principale par employé
        // On parcourt les adresses de l'employé et on garde celle marquée "principale"
        Address addr = order.getEmployee().getAddresses().stream()
                .filter(Address::isPrimary)   // ne garder que l'adresse principale
                .findFirst()                 // en prendre une seule (il n'y en a qu'une)
                .orElse(null);               // si aucune trouvée, addr = null
        if (addr != null) {
            // Adresse : on utilise uniquement formattedAddress s'il est présent, sinon null
            address = (addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank()) ? addr.getFormattedAddress() : null;
            // Coordonnées GPS pour la carte / navigation (null si non renseignées)
            latitude = addr.getLatitude() != null ? addr.getLatitude().doubleValue() : null;
            longitude = addr.getLongitude() != null ? addr.getLongitude().doubleValue() : null;
        }
        return new DriverDeliveryListItemDTO(
                order.getId(),
                order.getOrderNumber(),
                clientName,
                address,
                latitude,
                longitude,
                order.getStatus(),
                order.getDeliveryTour() != null ? order.getDeliveryTour().getId() : null
        );}
}
