package com.example.coopachat.services.DeliveryDriver;

import com.example.coopachat.dtos.DeliveryDriver.DriverDeliveryListItemDTO;
import com.example.coopachat.dtos.DeliveryDriver.DriverPersonalInfoDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.OrderStatus;
import com.example.coopachat.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        // Texte à afficher : priorité à l'adresse formatée (ex. Google), sinon "ville, quartier", sinon l'un des deux
        address = (addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank())
                ? addr.getFormattedAddress()
                : (addr.getCity() != null && addr.getDistrict() != null ? addr.getCity() + ", " + addr.getDistrict()
                : addr.getCity() != null ? addr.getCity() : addr.getDistrict());
        // Coordonnées GPS pour la carte / navigation (null si non renseignées)
        latitude = addr.getLatitude() != null ? addr.getLatitude().doubleValue() : null;
        longitude = addr.getLongitude() != null ? addr.getLongitude().doubleValue() : null;
    }
    //Récupérer le créneau horaire
    String timeSlotStr = order.getDeliveryTour() != null && order.getDeliveryTour().getTimeSlot() != null
            ? order.getDeliveryTour().getTimeSlot().getDisplayName()
            : null;

    return new DriverDeliveryListItemDTO(
            order.getId(),
            order.getOrderNumber(),
            clientName,
            address,
            latitude,
            longitude,
            timeSlotStr,
            order.getStatus(),
            order.getDeliveryTour() != null ? order.getDeliveryTour().getId() : null
    );
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
}
