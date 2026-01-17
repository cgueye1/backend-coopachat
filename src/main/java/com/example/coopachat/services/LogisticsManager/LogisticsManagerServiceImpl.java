package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.products.ProductStockListItemDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.entities.*;
import com.example.coopachat.enums.EtatStock;
import com.example.coopachat.enums.SupplierOrderStatus;
import com.example.coopachat.enums.UserRole;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.auth.ActivationCodeService;
import com.example.coopachat.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final CategoryRepository categoryRepository;
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

    // ============================================================================
    // 👁️ CONSULTATION DES DÉTAILS D'UNE COMMANDE FOURNISSEUR
    // ============================================================================
    @Transactional
    @Override
    public SupplierOrderDetailsDTO getSupplierOrderById(Long id) {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter les détails d'une commande fournisseur");
        }

        // Récupérer la commande par son ID
        SupplierOrder supplierOrder = supplierOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande fournisseur introuvable"));

        // Mapper la commande vers le DTO
        return mapToSupplierOrderDetailsDTO(supplierOrder);
    }

    /**
     * Mappe une entité SupplierOrder vers un DTO SupplierOrderDetailsDTO
     *
     * @param supplierOrder L'entité SupplierOrder à mapper
     * @return SupplierOrderDetailsDTO contenant toutes les informations de la commande
     */
    private SupplierOrderDetailsDTO mapToSupplierOrderDetailsDTO(SupplierOrder supplierOrder) {
        SupplierOrderDetailsDTO dto = new SupplierOrderDetailsDTO();
        dto.setId(supplierOrder.getId());
        dto.setOrderNumber(supplierOrder.getOrderNumber());
        dto.setSupplierName(supplierOrder.getSupplier().getName());
        dto.setExpectedDate(supplierOrder.getExpectedDate());
        dto.setStatus(supplierOrder.getStatus().getLabel()); // Récupérer le label de l'enum
        dto.setNotes(supplierOrder.getNotes());

        // Mapper les items (produits) de la commande
        dto.setItems(supplierOrder.getItems().stream()
                .map(this::mapToSupplierOrderItemDetailsDTO)
                .toList());

        return dto;
    }

    /**
     * Mappe une entité SupplierOrderItem vers un DTO SupplierOrderItemDetailsDTO
     *
     * @param item L'entité SupplierOrderItem à mapper
     * @return SupplierOrderItemDetailsDTO contenant les informations du produit
     */
    private SupplierOrderItemDetailsDTO mapToSupplierOrderItemDetailsDTO(SupplierOrderItem item) {
        SupplierOrderItemDetailsDTO dto = new SupplierOrderItemDetailsDTO();
        dto.setProductName(item.getProduct().getName());
        dto.setProductCategory(item.getProduct().getCategory().getName());
        dto.setProductImage(item.getProduct().getImage());
        dto.setQuantityOrdered(item.getQuantityOrdered());
        return dto;
    }

    // ============================================================================
    // 📋 LISTE PAGINÉE DES COMMANDES FOURNISSEURS
    // ============================================================================

    @Override
    @Transactional
    public SupplierOrderListResponseDTO getAllSupplierOrders(int page, int size, String search, Long supplierId, SupplierOrderStatus status) {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter la liste des commandes fournisseurs");
        }

        // Normaliser le terme de recherche (supprimer les espaces)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Récupérer les commandes selon les différents cas (recherche + filtres)
        Page<SupplierOrder> supplierOrderPage;

        if (searchTerm != null && supplierId != null && status != null) {
            // Cas 1 : Recherche + Filtre fournisseur + Filtre statut
            supplierOrderPage = supplierOrderRepository.findByOrderNumberOrProductNameAndSupplierIdAndStatus(
                    searchTerm, supplierId, status, pageable);
        } else if (searchTerm != null && supplierId != null) {
            // Cas 2 : Recherche + Filtre fournisseur
            supplierOrderPage = supplierOrderRepository.findByOrderNumberOrProductNameAndSupplierId(
                    searchTerm, supplierId, pageable);

        } else if (searchTerm != null && status != null) {
            // Cas 3 : Recherche + Filtre statut
            supplierOrderPage = supplierOrderRepository.findByOrderNumberOrProductNameAndStatus(
                    searchTerm, status, pageable);
        } else if (searchTerm != null) {
                // Cas 4 : Recherche seulement
                supplierOrderPage = supplierOrderRepository.findByOrderNumberOrProductName(searchTerm, pageable);
            } else if (supplierId != null && status != null) {
                // Cas 5 : Filtre fournisseur + Filtre statut
                supplierOrderPage = supplierOrderRepository.findBySupplierIdAndStatus(supplierId, status, pageable);
            } else if (supplierId != null) {
                // Cas 6 : Filtre fournisseur seulement
                supplierOrderPage = supplierOrderRepository.findBySupplierId(supplierId, pageable);
            } else if (status != null) {
                // Cas 7 : Filtre statut seulement
            supplierOrderPage = supplierOrderRepository.findByStatus(status, pageable);
        } else {
            // Cas 8 : Aucune recherche ni filtre
            supplierOrderPage = supplierOrderRepository.findAll(pageable);
        }

        // Mapper les entités SupplierOrder vers SupplierOrderListItemDTO
        List<SupplierOrderListItemDTO> orderList = supplierOrderPage.getContent().stream()
                .map(this::mapToSupplierOrderListItemDTO)
                .toList();


        // Créer la réponse paginée
        SupplierOrderListResponseDTO response = new SupplierOrderListResponseDTO();
        response.setContent(orderList);
        response.setTotalElements(supplierOrderPage.getTotalElements());
        response.setTotalPages(supplierOrderPage.getTotalPages());
        response.setCurrentPage(supplierOrderPage.getNumber());
        response.setPageSize(supplierOrderPage.getSize());
        response.setHasNext(supplierOrderPage.hasNext());
        response.setHasPrevious(supplierOrderPage.hasPrevious());

        return response;
    }

    /**
     * Mappe une entité SupplierOrder vers un DTO SupplierOrderListItemDTO
     *
     * @param supplierOrder L'entité SupplierOrder à mapper
     * @return SupplierOrderListItemDTO contenant les informations de la commande pour la liste
     */
    private SupplierOrderListItemDTO mapToSupplierOrderListItemDTO(SupplierOrder supplierOrder) {
        SupplierOrderListItemDTO dto = new SupplierOrderListItemDTO();
        dto.setId(supplierOrder.getId());
        dto.setOrderNumber(supplierOrder.getOrderNumber());
        dto.setSupplierName(supplierOrder.getSupplier().getName());
        dto.setExpectedDate(supplierOrder.getExpectedDate());
        dto.setStatus(supplierOrder.getStatus().getLabel()); // Récupérer le label de l'enum
        dto.setProductsSummary ( buildProductsSummary (supplierOrder.getItems())); // Construire le résumé des produits
        return dto;
    }


    /**
    * Construit le résumé des produits d'une commande (ex: "Riz (100), Huile (50)")
    *
    * @param items La liste des items (produits) de la commande
    * @return String contenant le résumé formaté des produits avec leurs quantités
    */
    private String buildProductsSummary (List<SupplierOrderItem> items){
        if (items == null || items.isEmpty()){
            return "";
        }
        //on va parcourir la liste des items et on va construire une chaîne de caractères avec le nom du produit et sa quantité et on va joindre les chaînes de caractères avec une virgule
        return items.stream()
                .map(item -> item.getProduct().getName() + " (" + item.getQuantityOrdered() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
    }


    // ============================================================================
    // 🔄 MODIFICATION DU STATUT D'UNE COMMANDE FOURNISSEUR
    // ============================================================================
    @Override
    @Transactional
    public void updateSupplierOrderStatus(Long id, UpdateSupplierOrderStatusDTO updateSupplierOrderStatusDTO) {

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
            throw new RuntimeException("Seul un responsable logistique peut modifier le statut d'une commande fournisseur");
        }

        // Récupérer la commande par son ID
        SupplierOrder supplierOrder = supplierOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande fournisseur introuvable"));

        // Mettre à jour le statut
        SupplierOrderStatus oldStatus = supplierOrder.getStatus();
        supplierOrder.setStatus(updateSupplierOrderStatusDTO.getStatus());

        // Sauvegarder la commande
        supplierOrderRepository.save(supplierOrder);

        log.info("Commande fournisseur {}: statut changé de {} à {} par {}",
                supplierOrder.getOrderNumber(), oldStatus, updateSupplierOrderStatusDTO.getStatus(), user.getEmail());
    }

    // ============================================================================
    // 📊 STATISTIQUES DES COMMANDES FOURNISSEURS
    // ============================================================================
    @Override
    @Transactional
    public SupplierOrderStatsDTO getSupplierOrderStats(){

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
            throw new RuntimeException("Seul un responsable logistique peut consulter les statistiques des commandes fournisseurs");
        }

        // Calculer les statistiques
        long total = supplierOrderRepository.count();
        long pending = supplierOrderRepository.countByStatus(SupplierOrderStatus.EN_ATTENTE);
        long delivered = supplierOrderRepository.countByStatus(SupplierOrderStatus.LIVREE);
        long cancelled = supplierOrderRepository.countByStatus(SupplierOrderStatus.ANNULEE);

        return new SupplierOrderStatsDTO(total,pending,delivered,cancelled) ;
    }

    // ============================================================================
   // 📤 EXPORT DES COMMANDES FOURNISSEURS EN EXCEL
   // ============================================================================
    @Override
    public ByteArrayResource exportSupplierOrders(String search, Long supplierId, SupplierOrderStatus status) {

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
            throw new RuntimeException("Seul un responsable logistique peut exporter les commandes fournisseurs");
        }

        // Normaliser le terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupérer les commandes selon les filtres (mêmes règles que la liste)
        List<SupplierOrder> orders;


        if (searchTerm != null && supplierId != null && status != null) {
            orders = supplierOrderRepository
                    .findByOrderNumberOrProductNameAndSupplierIdAndStatus(searchTerm, supplierId, status, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null && supplierId != null) {
            orders = supplierOrderRepository
                    .findByOrderNumberOrProductNameAndSupplierId(searchTerm, supplierId, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null && status != null) {
            orders = supplierOrderRepository
                    .findByOrderNumberOrProductNameAndStatus(searchTerm, status, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null) {
            orders = supplierOrderRepository
                    .findByOrderNumberOrProductName(searchTerm, Pageable.unpaged())
                    .getContent();
        } else if (supplierId != null && status != null) {
            orders = supplierOrderRepository
                    .findBySupplierIdAndStatus(supplierId, status, Pageable.unpaged())
                    .getContent();
        } else if (supplierId != null) {
            orders = supplierOrderRepository
                    .findBySupplierId(supplierId, Pageable.unpaged())
                    .getContent();
        } else if (status != null) {
            orders = supplierOrderRepository
                    .findByStatus(status, Pageable.unpaged())
                    .getContent();
        } else {
            orders = supplierOrderRepository.findAll();
        }
        // Générer le fichier Excel (création du classeur + feuille)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Commandes Fournisseurs");

            // Style simple pour l'en-tête (gras)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Colonnes du tableau Excel
            String[] headers = {"Référence", "Fournisseur", "Date prévue", "Produits", "Statut"};
            Row headerRow = sheet.createRow(0);
            
            //parcourir le tableau des headers et créer une cellule pour chaque en-tête
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les lignes avec les commandes
            int rowNum = 1; // la première ligne est l'en-tête
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            //parcourir la liste des commandes et créer une ligne pour chaque commande
            for (SupplierOrder order : orders) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getSupplier().getName());
                row.createCell(2).setCellValue(order.getExpectedDate() != null
                        ? order.getExpectedDate().format(dateFormatter) : "");
                row.createCell(3).setCellValue(buildProductsSummary(order.getItems()));
                row.createCell(4).setCellValue(order.getStatus().getLabel());

            }
            // Ajuster la largeur des colonnes pour une meilleure lecture
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // Convertir le classeur en bytes pour l'envoyer au client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }

    }

    // ============================================================================
    // 📦 LISTE DES STOCKS (PAGINÉE)
    // ============================================================================
    @Override
    @Transactional
    public ProductStockListResponseDTO getStockList (int page , int size , String search, Long categoryId , Boolean status ){

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
            throw new RuntimeException("Seul un responsable logistique peut consulter le suivi des stocks");
        }

        // Normalisation du terme de recherche :
        // - suppression des espaces inutiles
        // - conversion en null si vide, afin de désactiver le filtre dans la requête
        String searchTerm = (search != null && !search.trim().isEmpty())
                ? search.trim()
                : null;

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        //Récupérer la ctégorie si pas null
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }
        // Récupérer la page des produits selon les différents cas
        Page<Product> productPage;
        if (searchTerm != null && category != null && status != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
                    searchTerm, searchTerm, category, status, pageable);
        } else if (searchTerm != null && category != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategory(
                    searchTerm, searchTerm, category, pageable);
        } else if (searchTerm != null && status != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndStatus(
                    searchTerm, searchTerm, status, pageable);
        } else if (searchTerm != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        } else if (category != null && status != null) {
            productPage = productRepository.findByCategoryAndStatus(category, status, pageable);
        } else if (category != null) {
            productPage = productRepository.findByCategory(category, pageable);
        } else if (status != null) {
            productPage = productRepository.findByStatus(status, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        // Mapper les résultats en ProductStockListItemDTO
        // On parcourt les produits et on calcule l'état du stock
        List<ProductStockListItemDTO> items = productPage.getContent().stream().map(product -> {

            // Valeurs par défaut si null
            Integer currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            Integer minThreshold = product.getMinThreshold() != null ? product.getMinThreshold() : 0;

            // Définir l'état du stock pour chaque produit
            EtatStock etatStock ; // valeur par défaut

            //Définir l'état du  stock selon les différents cas
            if (currentStock == 0) {
                etatStock = EtatStock.RUPTURE;
            } else if (currentStock <= minThreshold) {
                etatStock = EtatStock.SOUS_SEUIL;
            } else {
                etatStock = EtatStock.SUFFISANT;
            }
            // Utiliser directement le constructeur pour remplir le DTO
            return new ProductStockListItemDTO(
                    product.getId(),
                    product.getName(),
                    product.getProductCode(),
                    product.getCategory() != null ? product.getCategory().getName() : null,
                    product.getImage(),
                    currentStock,
                    minThreshold,
                    etatStock
            );
        }).toList();
        // Utiliser directement le constructeur pour remplir la réponse paginée
        return  new ProductStockListResponseDTO(items , productPage.getTotalElements(),productPage.getTotalPages(), productPage.getNumber(), productPage.getSize(),productPage.hasNext(),productPage.hasPrevious());


    }

    // ============================================================================
    // ➕ ENTRÉE DE STOCK
    // ============================================================================
    @Override
    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("La quantité doit être positive");
        }

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
            throw new RuntimeException("Seul un responsable logistique peut modifier le stock");
        }

        // Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        product.setCurrentStock(currentStock + quantity);

        productRepository.save(product);
    }

    // ============================================================================
    // ➖ SORTIE DE STOCK
    // ============================================================================
    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("La quantité doit être positive");
        }

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
            throw new RuntimeException("Seul un responsable logistique peut modifier le stock");
        }

        // Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        //vérifier si le stock est suffisant pour le diminuer
        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        if (quantity > currentStock) {
            throw new RuntimeException("Stock insuffisant");
        }
        // si oui , on diminue le stock actuel
        product.setCurrentStock(currentStock - quantity);
        productRepository.save(product);
    }

    // ============================================================================
    // ✏️ MODIFICATION DU SEUIL MINIMUM
    // ============================================================================
    @Override
    @Transactional
    public void updateMinThreshold(Long productId, Integer minThreshold) {
        if (minThreshold == null || minThreshold < 0) {
            throw new RuntimeException("Le seuil minimum doit être positif ou égal à 0");
        }

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
            throw new RuntimeException("Seul un responsable logistique peut modifier le seuil minimum");
        }

        // Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        product.setMinThreshold(minThreshold);
        productRepository.save(product);
    }

    // ============================================================================
    // ✏️ MODIFICATION DU SEUIL MINIMUM PAR POURCENTAGE
    // ============================================================================
    @Override
    @Transactional
    public void updateMinThresholdByPercent(Long productId, Integer percent) {

        if (percent == null || percent <= 0) {
            throw new RuntimeException("Le pourcentage doit être positif");
        }

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
            throw new RuntimeException("Seul un responsable logistique peut modifier le seuil minimum");
        }

        // Récupérer le produit
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        
        //Récupérer le seuil minimum actuel
        Integer currentThreshold = product.getMinThreshold() != null ? product.getMinThreshold() : 0;

        //Calculer le delta (la variation ) en fonction du pourcentage
        int delta = (int) Math.round(currentThreshold * (percent / 100.0)); // ex: Combien vaut 10 % de 20 (le seuil) ? et prendre le nombre l'ajouter à notre valeur actuelle
    
        int newThreshold = currentThreshold + delta;

        product.setMinThreshold(newThreshold);
        productRepository.save(product);
    }

    // ============================================================================
    // 📊 STATISTIQUES DU SUIVI DES STOCKS
    // ============================================================================
    @Override
    @Transactional
    public StockStatsDTO getStockStats() {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter les statistiques des stocks");
        }

        long total = productRepository.count();
        long lowStock = productRepository.countLowStock();
        long outOfStock = productRepository.countByCurrentStock(0);

        return new StockStatsDTO(total, lowStock, outOfStock);
    }

    // ============================================================================
    // ⚠️ LISTE DES ALERTES DE RÉAPPROVISIONNEMENT (PAGINÉE)
    // ============================================================================
    @Override
    @Transactional
    public ProductStockListResponseDTO getStockAlerts(int page, int size, String search, Long categoryId) {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter les alertes de stock");
        }


        // Normalisation du terme de recherche :
        // - suppression des espaces inutiles
        // - conversion en null si vide, afin de désactiver le filtre dans la requête
        String searchTerm = (search != null && !search.trim().isEmpty())
                ? search.trim()
                : null;

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        //Récupérer la Catégorie si fournie
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }
        // Récupérer la page des produits selon le filtre
        Page<Product> productPage = productRepository.findStockAlerts(searchTerm, category, pageable);

        // Mapper les résultats en ProductStockListItemDTO
        List<ProductStockListItemDTO> items = productPage.getContent().stream()
                .map(this::mapToProductStockAlertDTO)
                .toList();

        //retourner la réponse
        return new ProductStockListResponseDTO(
                items,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.hasNext(),
                productPage.hasPrevious()
        );
    }

    /**
     * Mappe un produit vers ProductStockListItemDTO pour la liste d'alertes
     */
    private ProductStockListItemDTO mapToProductStockAlertDTO(Product product) {
        Integer currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        Integer minThreshold = product.getMinThreshold() != null ? product.getMinThreshold() : 0;

        return new ProductStockListItemDTO(
                product.getId(),
                product.getName(),
                product.getProductCode(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImage(),
                currentStock,
                minThreshold,
                EtatStock.SOUS_SEUIL
        );
    }

    // ============================================================================
    // 📤 EXPORT DES ALERTES DE STOCK
    // ============================================================================
    @Override
    @Transactional
    public ByteArrayResource exportStockAlerts(String search, Long categoryId) {

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
            throw new RuntimeException("Seul un responsable logistique peut exporter les alertes de stock");
        }

        // Normalisation du terme de recherche :
        // - suppression des espaces inutiles
        // - conversion en null si vide, afin de désactiver le filtre dans la requête
        String searchTerm = (search != null && !search.trim().isEmpty())
                ? search.trim()
                : null;


        //Récupérer la Catégorie si fournie
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }

        //Récupérer la liste des produits à exporter
        List<Product> products = productRepository
                .findStockAlerts(searchTerm, category, Pageable.unpaged())
                .getContent();

        // Générer le fichier Excel (création du classeur + feuille)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Alertes Stock");

            // Style simple pour l'en-tête (gras)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Colonnes du tableau Excel (sans statut)
            String[] headers = {"Référence", "Produit", "Catégorie", "Stock", "Seuil"};
            Row headerRow = sheet.createRow(0);

            //parcourir le tableau des headers et créer une cellule pour chaque en-tête
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les lignes avec les produits
            int rowNum = 1; // la première ligne est l'en-tête


            //parcourir la liste des produits et créer une ligne pour chaque produit dont les cellules contiendront les infos du produit concerné
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getProductCode() != null ? product.getProductCode() : "");
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory() != null ? product.getCategory().getName() : "");
                row.createCell(3).setCellValue(product.getCurrentStock() != null ? product.getCurrentStock() : 0);
                row.createCell(4).setCellValue(product.getMinThreshold() != null ? product.getMinThreshold() : 0);
            }

            // Ajuster la largeur des colonnes pour une meilleure lecture
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir le classeur en bytes pour l'envoyer au client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }
}






