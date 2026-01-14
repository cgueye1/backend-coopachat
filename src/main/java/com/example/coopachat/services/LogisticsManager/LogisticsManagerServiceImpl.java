package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.supplierOrders.CreateSupplierOrderDTO;
import com.example.coopachat.dtos.supplierOrders.SupplierOrderItemDTO;
import com.example.coopachat.dtos.supplierOrders.UpdateSupplierOrderDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.SupplierOrderStatus;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implémentation du service de gestion des actions du Responsable Logistique
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsManagerServiceImpl implements LogisticsManagerService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final ActivationCodeService activationCodeService;
    private final EmailService emailService;
    private final SupplierRepository supplierRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private  final ProductRepository productRepository;
    private final SupplierOrderItemRepository supplierOrderItemRepository;
    // ============================================================================
    // 🚚CRÉER UN LIVREUR
    // ============================================================================

     @Override
    @Transactional
    public void createDriver(RegisterDriverRequestDTO driverDTO) {

        // Vérifier que l'email n'existe pas déjà
        if (userRepository.existsByEmail(driverDTO.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Vérifier que le téléphone n'existe pas déjà
        if (userRepository.existsByPhone(driverDTO.getPhone())) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        // Récupérer le Responsable Logistique connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users logisticsManager = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

        // Vérifier que l'utilisateur connecté est bien un Responsable Logistique
        if (logisticsManager.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un Responsable Logistique peut créer un livreur");
        }

        // Créer l'utilisateur (livreur)
        Users user = new Users();
        user.setFirstName(driverDTO.getFirstName());
        user.setLastName(driverDTO.getLastName());
        user.setEmail(driverDTO.getEmail());
        user.setPhone(driverDTO.getPhone());
        user.setRole(UserRole.DELIVERY_DRIVER);
        user.setIsActive(false);

        Users savedUser = userRepository.save(user);

        // Créer le livreur
        Driver newDriver = new Driver();
        newDriver.setUser(savedUser);
        newDriver.setCreatedBy(logisticsManager);

        driverRepository.save(newDriver);

        // Créer et sauvegarder le code d'activation
        String codeActivation = activationCodeService.generateAndStoreCodeMobile(driverDTO.getEmail());

        // Envoyer l'email d'invitation avec le code d'activation
        emailService.sendDriverActivationCode(driverDTO.getEmail(), codeActivation, driverDTO.getFirstName());

        log.info("Livreur créé avec succès par le Responsable Logistique: {}", logisticsManager.getEmail());
    }

    // ============================================================================
    // 📦 GESTION DES COMMANDES FOURNISSEURS
    // ============================================================================
    @Override
    @Transactional
    public void createSupplierOrder (CreateSupplierOrderDTO createSupplierOrderDTO){

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

        // Vérifier que l'utilisateur connecté est bien un Responsable Logistique
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut créer une commande fournisseur");
        }

        // Vérifier que le fournisseur existe
        Supplier supplier = supplierRepository.findById(createSupplierOrderDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));

        // Vérifier que le fournisseur est actif
        if (!supplier.getIsActive()) {
            throw new RuntimeException("Le fournisseur sélectionné est inactif");
        }

        // Créer la commande fournisseur
        SupplierOrder supplierOrder = new SupplierOrder();
        supplierOrder.setSupplier(supplier);
        supplierOrder.setExpectedDate(createSupplierOrderDTO.getExpectedDate());
        supplierOrder.setNotes(createSupplierOrderDTO.getNotes());
        supplierOrder.setStatus(SupplierOrderStatus.EN_ATTENTE);//statut par défaut
        supplierOrder.setCreatedBy(user);

        // Générer le numéro de commande unique (ex: "CMD-2025-001")
        String orderNumber = generateUniqueOrderNumber();
        supplierOrder.setOrderNumber(orderNumber);

        // Créer les items (produits) de la commande
        for (SupplierOrderItemDTO itemDTO : createSupplierOrderDTO.getItems()) {
            // Vérifier que le produit existe
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable (ID: " + itemDTO.getProductId() + ")"));

            // Créer l'item de la commande
            SupplierOrderItem orderItem = new SupplierOrderItem();
            orderItem.setSupplierOrder(supplierOrder);
            orderItem.setProduct(product);
            orderItem.setQuantityOrdered(itemDTO.getQuantity());
            orderItem.setQuantityReceived(null); // Pas encore reçu

            // Ajouter l'item à la liste (le cascade s'occupera de la sauvegarde) et l'ajoutera directement dans la table SupplierOrderItems
            supplierOrder.getItems().add(orderItem);
        }

        // Sauvegarder la commande (le cascade sauvegarde automatiquement tous les items)
        supplierOrderRepository.save(supplierOrder);

        log.info("Commande fournisseur créée avec succès par {} : {} ({} produits)",
                user.getEmail(), orderNumber, createSupplierOrderDTO.getItems().size());
    }

    private String generateUniqueOrderNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String baseCode = "CMD-" + year + "-";
        String orderNumber;
        int counter = 1;

        do {
            orderNumber = baseCode + String.format("%03d", counter);
            counter++;
        } while (supplierOrderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
    }

    // ============================================================================
    // 🔄 MODIFICATION D'UNE COMMANDE FOURNISSEUR
    // ============================================================================
    @Override
    @Transactional
    public void updateSupplierOrder(Long id , UpdateSupplierOrderDTO updateSupplierOrderDTO) {

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String username = authentication.getName();
        Users user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

        // Vérifier que l'utilisateur connecté est bien un Responsable Logistique
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut modifier une commande fournisseur");
        }

        // Récupérer la commande par son ID
        SupplierOrder supplierOrder = supplierOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande fournisseur introuvable"));

        // Vérifier que le statut permet la modification (seulement "En attente")
        if (supplierOrder.getStatus() != SupplierOrderStatus.EN_ATTENTE) {
            throw new RuntimeException("Seules les commandes en attente peuvent être modifiées");
        }

        // Mettre à jour la date prévue si fournie
        if (updateSupplierOrderDTO.getExpectedDate() != null) {
            supplierOrder.setExpectedDate(updateSupplierOrderDTO.getExpectedDate());
        }

        // Mettre à jour les notes si fournies
        if (updateSupplierOrderDTO.getNotes() != null) {
            supplierOrder.setNotes(updateSupplierOrderDTO.getNotes());
        }

        // Mettre à jour les items si fournis (remplacer toute la liste)
        if (updateSupplierOrderDTO.getItems() != null) {

            // Supprimer tous les anciens items (grâce à orphanRemoval = true)
            supplierOrder.getItems().clear();

            // Créer les nouveaux items
            for (SupplierOrderItemDTO itemDTO : updateSupplierOrderDTO.getItems()) {

                // Vérifier que le produit existe
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("Produit introuvable (ID: " + itemDTO.getProductId() + ")"));

                // Créer le nouvel item
                SupplierOrderItem orderItem = new SupplierOrderItem();
                orderItem.setSupplierOrder(supplierOrder);
                orderItem.setProduct(product);
                orderItem.setQuantityOrdered(itemDTO.getQuantity());
                orderItem.setQuantityReceived(null); // Pas encore reçu

                // Ajouter l'item à la liste (le cascade s'occupera de la sauvegarde) et l'ajoutera directement dans la table SupplierOrderItems
                supplierOrder.getItems().add(orderItem);
            }


        }
        // Sauvegarder la commande (le cascade sauvegarde automatiquement les nouveaux items)
        supplierOrderRepository.save(supplierOrder);
        log.info("Commande fournisseur {} modifiée avec succès par {}",
                supplierOrder.getOrderNumber(), user.getEmail());
    }


}

