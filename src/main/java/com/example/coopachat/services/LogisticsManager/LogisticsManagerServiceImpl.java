package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.CancelDeliveryTourDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.claim.ClaimDetailDTO;
import com.example.coopachat.dtos.claim.ClaimListItemDTO;
import com.example.coopachat.dtos.claim.ClaimListResponseDTO;
import com.example.coopachat.dtos.claim.ClaimStatsDTO;
import com.example.coopachat.dtos.claim.RejectClaimDTO;
import com.example.coopachat.dtos.claim.ValidateClaimDTO;
import com.example.coopachat.dtos.dashboard.admin.LivraisonParJourDTO;
import com.example.coopachat.dtos.dashboard.admin.StockEtatGlobalDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.CommandesParJourDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.RLDashboardKpisDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatutTourneesDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.StatusCountDTO;
import com.example.coopachat.dtos.dashboard.logisticsManager.TauxRetoursParJourDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.dtos.products.ProductPreviewDTO;
import com.example.coopachat.dtos.products.ProductStockListItemDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.products.TopProductUsageDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.entities.util.GeoUtil;
import com.example.coopachat.enums.*;
import com.example.coopachat.repositories.*;
import com.example.coopachat.services.DeliveryDriver.DriverNotificationService;
import com.example.coopachat.services.LogisticsManager.LogisticsManagerNotificationService;
import com.example.coopachat.services.user.UserReferenceGenerator;
import com.example.coopachat.services.Employee.EmployeeNotificationService;
import com.example.coopachat.services.auth.EmailService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final ProductRepository productRepository;
    private final SupplierOrderItemRepository supplierOrderItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryDriverRepository deliveryDriverRepository;
    private final DeliveryTourRepository deliveryTourRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ClaimRepository claimRepository;
    private final EmailService emailService;
    private final EmployeeNotificationService employeeNotificationService;
    private final DriverNotificationService driverNotificationService;
    private final LogisticsManagerNotificationService logisticsManagerNotificationService;
    private final UserReferenceGenerator userReferenceGenerator;
    private final SupplierRepository supplierRepository;



    // ----------------------------------------------------------------------------
    // 🧾 GESTION DES FOURNISSEURS
    // ----------------------------------------------------------------------------

    @Override
    public List<SupplierListItemDTO> getAllSuppliers(Long categoryId, SupplierType type) {
        // On récupère uniquement les fournisseurs actifs
        return supplierRepository.findWithFilters(null, categoryId, type, true, Pageable.unpaged())
                .stream()
                .map(this::mapToSupplierListItemDTO)
                .collect(Collectors.toList());
    }

    private SupplierListItemDTO mapToSupplierListItemDTO(Supplier s) {
        SupplierListItemDTO dto = new SupplierListItemDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setCategoryNames(s.getCategories() != null && !s.getCategories().isEmpty() 
            ? s.getCategories().stream().map(Category::getName).collect(java.util.stream.Collectors.joining(", ")) 
            : null);
        dto.setType(s.getType());
        dto.setContactName(s.getContactName());
        dto.setPhone(s.getPhone());
        dto.setEmail(s.getEmail());
        dto.setActive(s.getIsActive());
        return dto;
    }

    // ============================================================================
    // 📦 GESTION DES COMMANDES FOURNISSEURS
    // ============================================================================
    @Override
    @Transactional
    public void createSupplierOrder(CreateSupplierOrderDTO createSupplierOrderDTO) {

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
        if (supplier.getIsActive() != null && !supplier.getIsActive()) {
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
            if (itemDTO.getQuantite() == null || itemDTO.getQuantite() <= 0) {
                throw new RuntimeException("La quantité commandée doit être positive");
            }
            // Vérifier que le produit existe
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable (ID: " + itemDTO.getProductId() + ")"));

            // Créer l'item de la commande
            SupplierOrderItem orderItem = new SupplierOrderItem();
            orderItem.setSupplierOrder(supplierOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantite());
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

    @Override
    @Transactional
    public void updateSupplierOrder(Long id, UpdateSupplierOrderDTO updateSupplierOrderDTO) {

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
                if (itemDTO.getQuantite() == null || itemDTO.getQuantite() <= 0) {
                    throw new RuntimeException("La quantité commandée doit être positive");
                }
                // Vérifier que le produit existe
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("Produit introuvable (ID: " + itemDTO.getProductId() + ")"));

                // Créer le nouvel item
                SupplierOrderItem orderItem = new SupplierOrderItem();
                orderItem.setSupplierOrder(supplierOrder);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemDTO.getQuantite());
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

        // Créer l'objet Pageable : tri par date de commande décroissante (dernière créée en haut)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));

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
        SupplierOrderStatus newStatus = updateSupplierOrderStatusDTO.getStatus();

        // Règle métier : une commande fournisseur LIVRÉE est finale → interdiction de revenir en arrière.
        if (oldStatus == SupplierOrderStatus.LIVREE && newStatus != SupplierOrderStatus.LIVREE) {
            throw new RuntimeException("Impossible de modifier le statut : cette commande fournisseur est déjà livrée.");
        }

        supplierOrder.setStatus(newStatus);

        // Quand le statut passe à LIVRÉE : enregistrer la date de réception et mettre à jour le stock des produits
        if (newStatus == SupplierOrderStatus.LIVREE && oldStatus != SupplierOrderStatus.LIVREE) {
            supplierOrder.setReceivedDate(LocalDateTime.now());
            for (SupplierOrderItem item : supplierOrder.getItems()) {
                Product product = item.getProduct();
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                if (product != null && quantity > 0) {
                    int newStock = product.getCurrentStock() != null ? product.getCurrentStock() + quantity : quantity;
                    product.setCurrentStock(newStock);
                    productRepository.save(product);
                    log.info("Stock mis à jour: produit {} (id={}) +{} → stock={}",
                            product.getName(), product.getId(), quantity, newStock);
                }
            }
        }

        // Sauvegarder la commande
        supplierOrderRepository.save(supplierOrder);

        log.info("Commande fournisseur {}: statut changé de {} à {} par {}",
                supplierOrder.getOrderNumber(), oldStatus, newStatus, user.getEmail());
    }

    @Override
    @Transactional
    public SupplierOrderStatsDTO getSupplierOrderStats() {

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

        return new SupplierOrderStatsDTO(total, pending, delivered, cancelled);
    }

    @Override
    @Transactional(readOnly = true)
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
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            //parcourir la liste des commandes et créer une ligne pour chaque commande
            for (SupplierOrder order : orders) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(order.getOrderNumber() != null ? order.getOrderNumber() : "");
                row.createCell(1).setCellValue(
                        order.getSupplier() != null ? order.getSupplier().getName() : "");
                row.createCell(2).setCellValue(order.getExpectedDate() != null
                        ? order.getExpectedDate().format(dateFormatter) : "");
                row.createCell(3).setCellValue(buildProductsSummary(order.getItems()));
                row.createCell(4).setCellValue(
                        order.getStatus() != null ? order.getStatus().getLabel() : "");

            }
            autoSizeColumnsSafe(sheet, headers.length);
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
    public ProductStockListResponseDTO getStockList(int page, int size, String search, Long categoryId, Boolean status) {

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
            EtatStock etatStock = computeStockStatus(currentStock, minThreshold);
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
        return new ProductStockListResponseDTO(items, productPage.getTotalElements(), productPage.getTotalPages(), productPage.getNumber(), productPage.getSize(), productPage.hasNext(), productPage.hasPrevious());


    }

    private EtatStock computeStockStatus(Integer currentStock, Integer minThreshold) {
        int stockValue = currentStock != null ? currentStock : 0;
        int thresholdValue = minThreshold != null ? minThreshold : 0;

        if (stockValue == 0) {
            return EtatStock.RUPTURE;
        }
        if (stockValue < thresholdValue) {
            return EtatStock.SOUS_SEUIL;
        }
        return EtatStock.SUFFISANT;
    }

    /**
     * Ajuste la largeur des colonnes Excel. Sur serveur headless (Linux sans AWT),
     * {@link Sheet#autoSizeColumn(int)} échoue souvent — on applique alors une largeur par défaut.
     */
    private void autoSizeColumnsSafe(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Throwable e) {
                log.debug("autoSizeColumn({}) ignoré (headless / polices): {}", i, e.getMessage());
                sheet.setColumnWidth(i, 20 * 256);
            }
        }
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
        long sufficient = total - lowStock - outOfStock;

        return new StockStatsDTO(total, lowStock, outOfStock, sufficient);
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

    // ============================================================================
    // 📤 EXPORT DES ALERTES DE STOCK
    // ============================================================================
    @Override
    @Transactional(readOnly = true)
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
            int rowNum = 1;


            //parcourir la liste des produits et créer une ligne pour chaque produit dont les cellules contiendront les infos du produit concerné
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getProductCode() != null ? product.getProductCode() : "");
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory() != null ? product.getCategory().getName() : "");
                row.createCell(3).setCellValue(product.getCurrentStock() != null ? product.getCurrentStock() : 0);
                row.createCell(4).setCellValue(product.getMinThreshold() != null ? product.getMinThreshold() : 0);
            }

            autoSizeColumnsSafe(sheet, headers.length);

            // Convertir le classeur en bytes pour l'envoyer au client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    // ============================================================================
    // 📤 EXPORT DU SUIVI DES STOCKS
    // ============================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportStockList(String search, Long categoryId, Boolean status) {

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
            throw new RuntimeException("Seul un responsable logistique peut exporter le suivi des stocks");
        }

        // Normalisation du terme de recherche :
        // - suppression des espaces inutiles
        // - conversion en null si vide, afin de désactiver le filtre dans la requête
        String searchTerm = (search != null && !search.trim().isEmpty())
                ? search.trim()
                : null;

        // Récupérer la catégorie si fournie
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        }

        // Récupérer la liste des produits selon les filtres (mêmes règles que la liste paginée)
        List<Product> products;
        if (searchTerm != null && category != null && status != null) {
            products = productRepository
                    .findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategoryAndStatus(
                            searchTerm, searchTerm, category, status, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null && category != null) {
            products = productRepository
                    .findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndCategory(
                            searchTerm, searchTerm, category, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null && status != null) {
            products = productRepository
                    .findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseAndStatus(
                            searchTerm, searchTerm, status, Pageable.unpaged())
                    .getContent();
        } else if (searchTerm != null) {
            products = productRepository
                    .findByNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(
                            searchTerm, searchTerm, Pageable.unpaged())
                    .getContent();
        } else if (category != null && status != null) {
            products = productRepository.findByCategoryAndStatus(category, status, Pageable.unpaged())
                    .getContent();
        } else if (category != null) {
            products = productRepository.findByCategory(category, Pageable.unpaged()).getContent();
        } else if (status != null) {
            products = productRepository.findByStatus(status, Pageable.unpaged()).getContent();
        } else {
            products = productRepository.findAll();
        }

        // Générer le fichier Excel (création du classeur + feuille)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Suivi Stocks");

            // Style simple pour l'en-tête (gras)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Colonnes du tableau Excel
            String[] headers = {"Référence", "Produit", "Catégorie", "Stock", "Seuil", "Etat Stock"};
            Row headerRow = sheet.createRow(0);

            // Parcourir le tableau des headers et créer une cellule pour chaque en-tête
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les lignes avec les produits
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);//rowNum++ utilise d’abord la valeur actuelle,   puis l’incrémente pour le tour suivant.


                Integer currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                Integer minThreshold = product.getMinThreshold() != null ? product.getMinThreshold() : 0;
                EtatStock etatStock = computeStockStatus(currentStock, minThreshold);

                row.createCell(0).setCellValue(product.getProductCode() != null ? product.getProductCode() : "");
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory() != null ? product.getCategory().getName() : "");
                row.createCell(3).setCellValue(currentStock);
                row.createCell(4).setCellValue(minThreshold);
                row.createCell(5).setCellValue(etatStock.getLabel());
            }

            autoSizeColumnsSafe(sheet, headers.length);

            // Convertir le classeur en bytes pour l'envoyer au client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    // ============================================================================
    // 📦 GESTION DES COMMANDES SALARIÉS
    // ============================================================================
    @Override
    @Transactional(readOnly = true)
    public OrderEmployeeListResponseDTO getAllEmployeeOrders(int page, int size, String search, OrderStatus status) {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter la liste des commandes salariés");
        }

        // Normaliser les paramètres
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Créer l'objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Récupérer la liste des commandes selon les filtres
        Page<Order> orderPage = orderRepository.findEmployeeOrders(searchTerm, status, pageable);

        // On parcourt la liste paginée des commandes récupérées depuis la base de données
        // Objectif : transformer chaque commande en un DTO prêt pour l’affichage côté RL
        List<OrderEmployeeListItemDTO> orderList = orderPage.getContent().stream()
                .map(order -> {

                    // Liste des noms de produits (items ou product peuvent être null)
                    List<OrderItem> items = order.getItems() != null ? order.getItems() : new ArrayList<OrderItem>();
                    List<String> products = items.stream()
                            .filter(item -> item.getProduct() != null)
                            .map(item -> item.getProduct().getName())
                            .collect(Collectors.toList());

                    List<String> display = products.size() > 2
                            ? Arrays.asList(products.get(0), products.get(1), "+" + (products.size() - 2))
                            : products;

                    String deliveryFrequency = (order.getDeliveryOption() != null && order.getDeliveryOption().getName() != null)
                            ? order.getDeliveryOption().getName()
                            : "—";

                    LocalDate orderDate = order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null;
                    LocalDate statusDate = getOrderStatusDate(order);

                    String employeeName = "—";
                    if (order.getEmployee() != null && order.getEmployee().getUser() != null) {
                        var u = order.getEmployee().getUser();
                        employeeName = (u.getFirstName() != null ? u.getFirstName() : "") + " "
                                + (u.getLastName() != null ? u.getLastName() : "").trim();
                        if (employeeName.trim().isEmpty()) employeeName = "—";
                    }

                    return new OrderEmployeeListItemDTO(
                            order.getId(),
                            order.getOrderNumber(),
                            employeeName,
                            orderDate,
                            statusDate,
                            display,
                            deliveryFrequency,
                            order.getStatus() != null ? order.getStatus().getLabel() : "—",
                            order.getFailureReason()
                    );
                })
                // Ici, on termine le parcours et on récupère la liste finale des DTO
                .toList();

        // Enfin, on retourne la réponse complète avec :
        // - la liste des commandes formatées
        // - les informations de pagination nécessaires au front
        return new OrderEmployeeListResponseDTO(
                orderList,
                orderPage.getTotalElements(), // Nombre total de commandes
                orderPage.getTotalPages(),    // Nombre total de pages
                orderPage.getNumber(),        // Page actuelle
                orderPage.getSize(),          // Taille de la page
                orderPage.hasNext(),          // Y a-t-il une page suivante ?
                orderPage.hasPrevious()       // Y a-t-il une page précédente ?
        );

    }

    /** Date à laquelle la commande est passée à son statut actuel (pour affichage "Passé à ce statut le ..."). */
    private LocalDate getOrderStatusDate(Order order) {
        if (order == null) return null;
        LocalDateTime dateTime = null;
        switch (order.getStatus()) {
            case EN_ATTENTE -> dateTime = order.getCreatedAt();
            case VALIDEE -> dateTime = order.getValidatedAt();
            case EN_PREPARATION -> dateTime = order.getPickupStartedAt();
            case EN_COURS -> dateTime = order.getDeliveryStartedAt();
            case ARRIVE -> dateTime = order.getDeliveryArrivedAt();
            case LIVREE -> dateTime = order.getDeliveryCompletedAt();
            case ECHEC_LIVRAISON -> dateTime = order.getFailureReportedAt();
            case ANNULEE -> dateTime = order.getUpdatedAt();
            default -> dateTime = order.getCreatedAt();
        }
        if (dateTime == null) dateTime = order.getCreatedAt();
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    @Transactional(readOnly = true)
    @Override
    public EmployeeOrderStatsDTO getEmployeeOrderStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String username = authentication.getName();
        Users user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les statistiques des commandes salariés");
        }

        LocalDate today = LocalDate.now();
        long enAttente = orderRepository.countByStatusAndDeliveryTourIsNull(OrderStatus.EN_ATTENTE);
        long enRetard = orderRepository.countByStatusAndDeliveryDateBeforeAndDeliveryTourIsNull(OrderStatus.EN_ATTENTE, today);
        long enCours = deliveryTourRepository.countByStatus(DeliveryTourStatus.EN_COURS);
        long validees = orderRepository.countByStatus(OrderStatus.VALIDEE);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59, 999_000_000);
        long livreesCeMois = orderRepository.countByStatusAndDeliveryCompletedAtBetween(OrderStatus.LIVREE, monthStart, monthEnd);
        long totalCommandes = orderRepository.countByStatusNot(OrderStatus.ANNULEE);

        return new EmployeeOrderStatsDTO(totalCommandes, enAttente, enRetard, enCours, validees, livreesCeMois);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderItemDetailsDTO getOrderItemDetailById(Long orderId) {

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
            throw new RuntimeException("Seul un responsable logistique peut consulter les détails d'une commande");
        }

        // Charger la commande avec items + produits (évite lazy et liste vide)
        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // Construire le nom du salarié
        String employeeName = order.getEmployee() != null && order.getEmployee().getUser() != null
                ? (order.getEmployee().getUser().getFirstName() != null ? order.getEmployee().getUser().getFirstName() : "")
                  + " "
                  + (order.getEmployee().getUser().getLastName() != null ? order.getEmployee().getUser().getLastName() : "")
                : "";

        // Construire le nom et le téléphone du livreur (tournée + driver → user)
        String driverName = null;
        String driverPhone = null;
        if (order.getDeliveryTour() != null
                && order.getDeliveryTour().getDriver() != null
                && order.getDeliveryTour().getDriver().getUser() != null) {
            var driverUser = order.getDeliveryTour().getDriver().getUser();
            String first = driverUser.getFirstName();
            String last = driverUser.getLastName();
            driverName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            if (driverName.isEmpty()) {
                driverName = null;
            }
            String phone = driverUser.getPhone();
            if (phone != null && !phone.isBlank()) {
                driverPhone = phone.trim();
            }
        }

        String failureReason = order.getFailureReason();
        if (failureReason != null && failureReason.isBlank()) {
            failureReason = null;
        }

        // Récupérer la dernière transition d'historique pour identifier l'acteur
        // qui a posé le statut courant de la commande.
        Optional<OrderStatusHistory> latestStatusHistory = orderStatusHistoryRepository
                .findTopByOrderIdOrderByChangedAtDesc(order.getId());

        String currentStatusChangedByName = null;
        LocalDateTime currentStatusChangedAt = null;
        String currentStatusChangedByRole = null;
        String previousStatusLabel = null;
        if (latestStatusHistory.isPresent()) {
            OrderStatusHistory h = latestStatusHistory.get();
            String first = h.getActorFirstName() != null ? h.getActorFirstName().trim() : "";
            String last = h.getActorLastName() != null ? h.getActorLastName().trim() : "";
            String actorName = (first + " " + last).trim();
            currentStatusChangedByName = actorName.isBlank() ? null : actorName;
            currentStatusChangedAt = h.getChangedAt();
            currentStatusChangedByRole = h.getChangedByRole() != null
                    ? h.getChangedByRole().getLabel()
                    : null;
            // Statut précédent : même entrée d'historique si la dernière transition est * → ANNULEE
            // (tracée notamment dans EmployeeServiceImpl.cancelOrder avec setFromStatus).
            if (order.getStatus() == OrderStatus.ANNULEE
                    && h.getToStatus() == OrderStatus.ANNULEE
                    && h.getFromStatus() != null) {
                String pl = h.getFromStatus().getLabel();
                previousStatusLabel = (pl != null && !pl.isBlank()) ? pl.trim() : null;
            }
        }

        // On prépare un DTO contenant :
        // - les infos générales de la commande
        // - l'acteur/date ayant posé le statut courant
        // - éventuellement le nom / téléphone du livreur
        // - la liste des produits associés à la commande
        return new OrderItemDetailsDTO(
                order.getOrderNumber(),
                order.getCreatedAt().toLocalDate(),
                employeeName.trim(),
                order.getStatus().getLabel(),
                currentStatusChangedByName,
                currentStatusChangedAt,
                currentStatusChangedByRole,
                driverName,
                driverPhone,
                failureReason,
                previousStatusLabel,
                order.getItems().stream()
                        .filter(item -> item.getProduct() != null)
                        .map(item -> new ProductPreviewDTO(
                                item.getProduct().getImage(),
                                item.getProduct().getName(),
                                item.getProduct().getCategory().getName(),
                                item.getProduct().getCurrentStock(),
                                item.getQuantity() != null ? item.getQuantity() : 0
                        )).toList()
        );

    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportEmployeeOrders(String search, OrderStatus status) {

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
            throw new RuntimeException("Seul un responsable logistique peut exporter les commandes salariés");
        }
        // Normalisation du terme de recherche
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Récupération TOUTES les commandes (sans pagination)
        List<Order> orders = orderRepository.findEmployeeOrders(searchTerm, status, Pageable.unpaged()).getContent();

        // Génération Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Commandes Salariés");

            // Style en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Colonnes (adaptées à OrderEmployeeListItemDTO)
            String[] headers = {
                    "N° Commande",           // order.getOrderNumber()
                    "Salarié",              // Nom complet
                    "Date Commande",        // order.getCreatedAt()
                    "Produits",             // Liste formatée
                    "Option Livraison",     // order.getDeliveryOption()
                    "Statut"                // order.getStatus()
            };
            // Création ligne en-tête
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplissage des données
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);// rowNum++ = utilise PUIS incrémente

                // N° Commande (colonne 0)
                row.createCell(0).setCellValue(order.getOrderNumber());

                // Salarié (colonne 1)
                String employeeName = order.getEmployee().getUser().getFirstName() + " "
                        + order.getEmployee().getUser().getLastName();
                row.createCell(1).setCellValue(employeeName);

                // Date Commande (colonne 2)
                row.createCell(2).setCellValue(order.getCreatedAt().toLocalDate().toString());

                // Produits : on va récupérer tous les noms des produits
                List<String> products = order.getItems().stream()
                        .map(item -> item.getProduct().getName())
                        .collect(Collectors.toList());
                String productsDisplay = String.join(", ", products);
                row.createCell(3).setCellValue(productsDisplay);

                // Option Livraison (colonne 4)
                String deliveryOption = order.getDeliveryOption() != null
                        ? order.getDeliveryOption().getName()
                        : "Non spécifiée";
                row.createCell(4).setCellValue(deliveryOption);

                // Statut (colonne 5)
                row.createCell(5).setCellValue(order.getStatus().getLabel());

            }
            autoSizeColumnsSafe(sheet, headers.length);

            // ByteArrayOutputStream = "Un conteneur temporaire en mémoire"
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // workbook.write(outputStream) = "Écris le Excel dans ce conteneur"
            workbook.write(outputStream);

            // toByteArray() = "Transforme en tableau d'octets"
            // ByteArrayResource = "Prépare pour l'envoi au navigateur"
            return new ByteArrayResource(outputStream.toByteArray()); // Transforme en données brutes téléchargeables

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void replanOrder(Long orderId) {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut replanifier une commande");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (order.getStatus() != OrderStatus.ECHEC_LIVRAISON) {
            throw new RuntimeException("Seules les commandes en échec de livraison peuvent être replanifiées");
        }
        order.setStatus(OrderStatus.EN_ATTENTE);
        order.setDeliveryTour(null);
        order.setFailureReason(null);
        orderRepository.save(order);
        employeeNotificationService.notifyOrderReplanned(order);
        log.info("Commande {} replanifiée (EN_ATTENTE) par le RL", order.getOrderNumber());
    }

    @Override
    @Transactional
    public void cancelOrderAfterFailure(Long orderId) {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut annuler une commande après échec");
        }
        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));
        if (order.getStatus() != OrderStatus.ECHEC_LIVRAISON) {
            throw new RuntimeException("Seules les commandes en échec de livraison peuvent être annulées définitivement");
        }
        // Réintégration des quantités en stock pour chaque ligne de commande
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Product product = item.getProduct();
                    int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                    product.setCurrentStock(current + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }
        order.setStatus(OrderStatus.ANNULEE);
        order.setDeliveryTour(null);
        order.setFailureReason(null);
        orderRepository.save(order);
        employeeNotificationService.notifyOrderCancelledAfterFailure(order);
        log.info("Commande {} annulée définitivement après échec (stock réintégré) par le RL", order.getOrderNumber());
    }

    // ============================================================================
    // 🚚 GESTION DES TOURNÉES DE LIVRAISON
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public DeliveryPlanningCalendarResponseDTO getDeliveryPlanningCalendar(int year, int month) {
        /*
         * Objectif (vue globale RL)
         * - Construire un calendrier pour un mois donné.
         * - Pour chaque jour du mois, on retourne des compteurs basés sur la DATE DE LIVRAISON des commandes
         *   (Order.deliveryDate). Autrement dit: "le 17/03 = date de livraison de X commandes".
         *
         * Champs du DTO:
         * - pendingOrders : commandes EN_ATTENTE, non planifiées (deliveryTour = NULL), groupées par deliveryDate
         * - plannedOrders : commandes déjà planifiées = dans une tournée (deliveryTour != NULL)
         *                 dont le statut est "actif" (ASSIGNEE/EN_COURS/TERMINEE), groupées par deliveryDate
         * - overdueOrders : parmi pendingOrders, celles dont la date de livraison est passée (deliveryDate < aujourd'hui)
         *
         * Important:
         * - plannedOrders est  regroupé par Order.deliveryDate
         *   pour que le calendrier reflète uniquement les dates de livraison des commandes.
         */
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter le calendrier");
        }
        if (month < 1 || month > 12) {
            throw new RuntimeException("Mois invalide (1-12)");
        }

        // Période du mois (inclusif)
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // 1) EN_ATTENTE non planifiées (Order.deliveryDate dans le mois)
        List<Object[]> pendingRows = orderRepository.countPendingOrdersByDeliveryDateBetween(
                OrderStatus.EN_ATTENTE, monthStart, monthEnd);
        Map<LocalDate, Long> pendingByDay = new HashMap<>();
        for (Object[] row : pendingRows) {
            LocalDate day = (LocalDate) row[0];
            Long count = (Long) row[1];
            pendingByDay.put(day, count != null ? count : 0L);
        }

        // 2) Déjà planifiées = commandes présentes dans une tournée "active" (basé sur le statut de la tournée)
        List<DeliveryTourStatus> tourStatuses = java.util.List.of(
               DeliveryTourStatus.ASSIGNEE,
               DeliveryTourStatus.EN_COURS,
                DeliveryTourStatus.TERMINEE
        );
        List<Object[]> plannedRows = orderRepository.countPlannedOrdersByDeliveryDateBetween(
                monthStart, monthEnd, tourStatuses);
       Map<LocalDate, Long> plannedByDay = new HashMap<>();
        for (Object[] row : plannedRows) {
            LocalDate day = (LocalDate) row[0];
            Long count = (Long) row[1];
            plannedByDay.put(day, count != null ? count : 0L);
        }

        // 3) Assemblage du résultat: 1 DTO par jour du mois (y compris jours à 0)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<DeliveryPlanningCalendarDayDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            long rawPending = pendingByDay.getOrDefault(d, 0L);
            long planned = plannedByDay.getOrDefault(d, 0L);
            // On ne veut plus afficher les jours "en retard" dans le calendrier (jours < aujourd'hui).
            // On conserve quand même l'info dans overdueOrders pour calculer un total global côté front.
            long overdue = (rawPending > 0 && d.isBefore(today)) ? rawPending : 0L;
            long visiblePending = d.isBefore(today) ? 0L : rawPending;
            result.add(new DeliveryPlanningCalendarDayDTO(d.format(fmt), visiblePending, planned, overdue));
        }
        long totalOverdueGlobal = orderRepository.countOverduePendingUnassigned(OrderStatus.EN_ATTENTE, today);
        return new DeliveryPlanningCalendarResponseDTO(result, totalOverdueGlobal);
    }

    /** Filtre : date + EN_ATTENTE + employé actif + pas en tournée. Retourne les Order pour liste ou regroupement. */
    private List<Order> filterEligibleOrders(LocalDate deliveryDate) {
        return orderRepository.findEligibleOrdersForDate(deliveryDate, OrderStatus.EN_ATTENTE);
    }

    @Override
    public List<EligibleOrderDTO> getEligibleOrders(LocalDate deliveryDate) {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les commandes éligibles");
        }
        return filterEligibleOrders(deliveryDate).stream()
                .map(order -> mapToEligibleOrderDTO(order, deliveryDate))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleOrderLotDTO> getGroupedEligibleOrders(LocalDate deliveryDate, int lotSize) {

        // On récupère l'utilisateur connecté
        Users user = getCurrentUser();

        // On vérifie que seul le responsable logistique peut accéder à cette méthode
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les commandes éligibles groupées");
        }

        // ------------------------------------------------------------
        // ETAPE 1 : Récupérer les commandes éligibles depuis la base
        // ------------------------------------------------------------
        // Cette méthode appelle le repository et retourne les commandes qui respectent :
        // - deliveryDate <= date demandée
        // - status = EN_ATTENTE
        // - employé actif
        // - pas encore affectées à une tournée

        List<Order> eligible = filterEligibleOrders(deliveryDate);

        // ------------------------------------------------------------
        // ETAPE 2 : Regrouper les commandes par proximité GPS
        // ------------------------------------------------------------

        List<List<Order>> lots = groupOrdersByProximity(eligible, lotSize);

        // Exemple si lotSize = 3 :
        //
        // lots =
        // [
        //   [O1, O2, O3],   // Lot 1 (Plateau +Pikine)
        //   [O4, O5]        // Lot 2 (Yeumbeul)
        // ]

        // Liste finale des DTO qui sera retournée au frontend
        List<EligibleOrderLotDTO> result = new ArrayList<>();

        // ------------------------------------------------------------
        // ETAPE 3 : Transformer chaque lot en DTO
        // ------------------------------------------------------------
        for (int i = 0; i < lots.size(); i++) {

            // Récupérer les commandes du lot courant
            List<Order> lotOrders = lots.get(i);

            // Exemple :
            // i = 0
            // lotOrders = [O1, O2, O3]

            // ------------------------------------------------------------
            // ETAPE 4 : Créer un nom de zone pour le lot
            // ------------------------------------------------------------

            // Nom par défaut :
            String zoneLabel = "Lot " + (i + 1);

            // Exemple :
            // zoneLabel = "Lot 1"

            // ------------------------------------------------------------
            // ETAPE 5 : Convertir les commandes en DTO
            // ------------------------------------------------------------

            // Chaque Order devient EligibleOrderDTO

            // Exemple conversion :
            //
            // O1 →
            // EligibleOrderDTO {
            //    orderId = 1
            //    orderNumber = "CMD001"
            //    customerName = "Sokhna Faye"
            //    formattedAddress = "sacréCoeur,Dakar"
            // }

            List<EligibleOrderDTO> orderDTOs =
                    lotOrders.stream()
                            .map(order -> mapToEligibleOrderDTO(order, deliveryDate))
                            .toList();


            // ------------------------------------------------------------
            // ETAPE 6 : Créer le DTO du lot
            // ------------------------------------------------------------

            EligibleOrderLotDTO lotDTO =
                    new EligibleOrderLotDTO(
                            i + 1,             // numéro du lot (1, 2, 3...)
                            lotOrders.size(),  // nombre de commandes
                            orderDTOs,        // liste des commandes DTO
                            zoneLabel         // nom de zone pour affichage (ex. "Lot 1")
                    );

            // Ajouter à la liste finale
            result.add(lotDTO);
        }

        // ------------------------------------------------------------
        // ETAPE 7 : Retourner le résultat final au controller
        // ------------------------------------------------------------

        // Exemple résultat final :
        //
        // result =
        // [
        //   {
        //     lotNumber: 1,
        //     orderCount: 3,
        //     orders: [...]
        //   },
        //   {
        //     lotNumber: 2,
        //     orderCount: 2,
        //     orders: [...]
        //   }
        // ]

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countEligibleOrdersForPlanning(LocalDate deliveryDate) {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter ce décompte");
        }
        return orderRepository.countEligibleOrdersForPlanningDate(deliveryDate, OrderStatus.EN_ATTENTE);
    }

    @Override
    public List<AvailableDriverDTO> getAvailableDrivers(LocalDate deliveryDate, Long excludeTourId) {

        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les chauffeurs disponibles");
        }

        List<Driver> activeDrivers = deliveryDriverRepository.findAll().stream()
                .filter(driver -> driver.getUser() != null && driver.getUser().getIsActive())
                .toList();

        if (deliveryDate == null) {
            return activeDrivers.stream()
                    .map(driver -> new AvailableDriverDTO(driver.getId(), driverFullName(driver)))
                    .toList();
        }

        List<DeliveryTourStatus> activeStatuses = java.util.List.of(
                DeliveryTourStatus.ASSIGNEE,
                DeliveryTourStatus.EN_COURS);
        java.util.Set<Long> busyDriverIds = new java.util.HashSet<>(
                deliveryTourRepository.findDriverIdsWithActiveTourOnDateExcluding(
                        deliveryDate, activeStatuses, excludeTourId));

        return activeDrivers.stream()
                .filter(driver -> !busyDriverIds.contains(driver.getId()))
                .map(driver -> new AvailableDriverDTO(driver.getId(), driverFullName(driver)))
                .toList();
    }

    private static String driverFullName(Driver driver) {
        return driver.getUser().getFirstName() + " " + driver.getUser().getLastName();
    }

    @Override
    @Transactional
    public void createDeliveryTour(CreateDeliveryTourDTO dto) {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut créer une tournée");
        }
        // 2. RÉCUPÉRATION DU CHAUFFEUR
        Driver driver = deliveryDriverRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new RuntimeException("Chauffeur introuvable"));

        // 3. RÉCUPÉRATION DES COMMANDES
        List <Order> orders = orderRepository.findAllById(dto.getOrderIds());
         if(orders.size() != dto.getOrderIds().size()){
             throw new RuntimeException("Certaines commandes n'existent pas");
         }
        // 4. CRÉATION DE LA TOURNÉE
        DeliveryTour tour = new DeliveryTour();

        // Numéro unique aléatoire (même principe que les commandes CMD-XXXXXXXX)
        String tourNumber = generateUniqueTourNumber();
        tour.setTourNumber(tourNumber);

        // Informations de base
        tour.setDeliveryDate(dto.getDeliveryDate());
        tour.setDriver(driver);
        tour.setVehicleTypePlate(buildVehicleTypePlate(dto.getVehicleType(), dto.getVehiclePlate()));
        tour.setCreatedBy(currentUser);
        tour.setUpdatedBy(currentUser);
        tour.setNotes(dto.getNotes());
        // Tournée directement assignée au livreur 
        tour.setStatus(DeliveryTourStatus.ASSIGNEE);

        // 5. SAUVEGARDE
        DeliveryTour savedTour = deliveryTourRepository.save(tour);

        // 6. ASSIGNATION DES COMMANDES + passage en VALIDEE (RL valide en les mettant dans la tournée)
        for (Order order : orders) {
            OrderStatus fromStatus = order.getStatus();
            order.setDeliveryTour(savedTour);
            order.setStatus(OrderStatus.VALIDEE);
            order.setValidatedAt(LocalDateTime.now());
            Order savedOrder = orderRepository.save(order);

            // Historique de statut : état précédent -> VALIDEE par le RL courant.
            // Ce log alimentera la timeline détaillée de la commande côté back-office.
            OrderStatusHistory statusHistory = new OrderStatusHistory();
            statusHistory.setOrder(savedOrder);
            statusHistory.setFromStatus(fromStatus);
            statusHistory.setToStatus(OrderStatus.VALIDEE);
            statusHistory.setChangedByUser(currentUser);
            statusHistory.setChangedByRole(currentUser.getRole());
            statusHistory.setActorFirstName(currentUser.getFirstName());
            statusHistory.setActorLastName(currentUser.getLastName());
            statusHistory.setReason("Commande validée et affectée à la tournée " + tourNumber);
            statusHistory.setSourceAction("TOUR_CREATED");
            orderStatusHistoryRepository.save(statusHistory);

            // Notification au salarié que sa commande a été validée
            employeeNotificationService.notifyOrderScheduled(savedOrder);
        }

        // Notification au livreur qu'une tournée lui a été assignée
        driverNotificationService.notifyTourAssigned(savedTour, orders.size());

        log.info("Tournée {} créée par {} avec {} commandes (chauffeur: {})",
                tourNumber, currentUser.getEmail(), orders.size(),
                driver.getUser().getFirstName());

    }

    /**
     * Numéro de tournée unique aléatoire (même principe que les commandes salarié : {@code CMD-} + 8 caractères ).
     * Format {@code PL-XXXXXXXX} — préfixe planification conservé, suffixe imprévisible (plus de séquence annuelle).
     */
    private String generateUniqueTourNumber() {
        final int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String candidate = "PL-" + suffix;
            if (!deliveryTourRepository.existsByTourNumber(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Impossible de générer un numéro de tournée unique");
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryTourDetailsDTO getDeliveryTourDetails(Long tourId) {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut récupérer les détails d'une tournée");
        }

        // Récupérer la tournée avec ses relations
        DeliveryTour deliveryTour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tournée introuvable"));

        DeliveryTourDetailsDTO dto = new DeliveryTourDetailsDTO();
        dto.setId(deliveryTour.getId());
        dto.setTourNumber(deliveryTour.getTourNumber());
        dto.setDeliveryDate(deliveryTour.getDeliveryDate());
        dto.setStatus(deliveryTour.getStatus());

        // Chauffeur
        if (deliveryTour.getDriver() != null) {
            dto.setDriverId(deliveryTour.getDriver().getId());
            dto.setDriverName(deliveryTour.getDriver().getUser().getFirstName()+ " "+ deliveryTour.getDriver().getUser().getLastName());
            dto.setDriverPhone(deliveryTour.getDriver().getUser().getPhone());
        }

        // Véhicule : type + matricule (séparés si stockés "Type — Plaque" dans vehicleTypePlate)
        fillVehicleTypeAndPlate(deliveryTour.getVehicleTypePlate(), dto);

        if (deliveryTour.getNotes() != null && !deliveryTour.getNotes().isBlank()) {
            dto.setNotes(deliveryTour.getNotes());
        }

        if (deliveryTour.getCancellationReason() != null && !deliveryTour.getCancellationReason().isBlank()) {
            dto.setCancellationReason(deliveryTour.getCancellationReason());
        }

        // Commandes : ordre de passage (validatedAt puis id), détails + récapitulatif
        if (deliveryTour.getOrders() != null && !deliveryTour.getOrders().isEmpty()) {
            List<Order> sortedOrders = new ArrayList<>(deliveryTour.getOrders());
            sortedOrders.sort(Comparator
                    .comparing((Order o) -> o.getValidatedAt() != null ? o.getValidatedAt() : LocalDateTime.MIN)
                    .thenComparing(Order::getId));

            dto.setOrderCount(sortedOrders.size());
            List<OrderInTourDTO> orderDtos = new ArrayList<>();
            int delivered = 0;
            int failed = 0;
            BigDecimal totalTourAmount = BigDecimal.ZERO;

            for (Order order : sortedOrders) {
                String empName = "";
                String empFirst = "";
                String empLast = "";
                String companyName = "";
                String addressLabel = "—";
                if (order.getEmployee() != null) {
                    if (order.getEmployee().getCompany() != null && order.getEmployee().getCompany().getName() != null) {
                        companyName = order.getEmployee().getCompany().getName();
                    }
                    if (order.getEmployee().getUser() != null) {
                        empFirst = order.getEmployee().getUser().getFirstName() != null
                                ? order.getEmployee().getUser().getFirstName().trim() : "";
                        empLast = order.getEmployee().getUser().getLastName() != null
                                ? order.getEmployee().getUser().getLastName().trim() : "";
                        empName = (empFirst + " " + empLast).trim();
                        if (order.getEmployee().getAddresses() != null && !order.getEmployee().getAddresses().isEmpty()) {
                            addressLabel = order.getEmployee().getAddresses().stream()
                                    .filter(a -> a.getFormattedAddress() != null && !a.getFormattedAddress().isBlank())
                                    .sorted(Comparator.comparing((Address a) -> !a.isPrimary()))
                                    .findFirst()
                                    .map(Address::getFormattedAddress)
                                    .orElse("—");
                        }
                    }
                }

                OrderStatus st = order.getStatus();
                if (st == OrderStatus.LIVREE) {
                    delivered++;
                }
                if (st == OrderStatus.ECHEC_LIVRAISON) {
                    failed++;
                }

                BigDecimal lineTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
                totalTourAmount = totalTourAmount.add(lineTotal);

                String paymentStatusLabel = PaymentStatus.UNPAID.getLabel();
                Payment pay = order.getPayment();
                if (pay != null && pay.getStatus() != null) {
                    paymentStatusLabel = pay.getStatus().getLabel();
                }

                OrderInTourDTO row = new OrderInTourDTO();
                row.setOrderId(order.getId());
                row.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : "");
                row.setEmployeeName(empName.trim());
                row.setEmployeeFirstName(empFirst);
                row.setEmployeeLastName(empLast);
                row.setCompanyName(companyName);
                row.setDeliveryAddress(addressLabel);
                row.setTotalAmount(lineTotal);
                row.setOrderStatus(st != null ? st.name() : "");
                row.setOrderStatusLabel(st != null ? st.getLabel() : "");
                row.setPaymentStatusLabel(paymentStatusLabel);
                orderDtos.add(row);
            }

            dto.setOrders(orderDtos);
            dto.setDeliveredOrderCount(delivered);
            dto.setFailedOrderCount(failed);
            dto.setTotalTourAmount(totalTourAmount);
        } else {
            dto.setOrderCount(0);
            dto.setDeliveredOrderCount(0);
            dto.setFailedOrderCount(0);
            dto.setTotalTourAmount(BigDecimal.ZERO);
        }

        return dto;

    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryTourListResponseDTO getAllDeliveryTours(int page, int size, String tourNumber, DeliveryTourStatus status) {

        // VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut récupérer les tournées de livraison ");
        }

        // Normaliser le paramètre
        String tourNumberFilter = (tourNumber != null && !tourNumber.trim().isEmpty())
                ? tourNumber.trim()
                : null;

        // Créer la pagination
        Pageable pageable = PageRequest.of(page, size);

        // Récupérer les données si fournies
        Page<DeliveryTour> deliveryTourPage = deliveryTourRepository.findDeliveryTourWithFilters(tourNumberFilter, status, pageable);

        // Transformer en DTO
        List<DeliveryTourListDTO> tourList = deliveryTourPage.getContent().stream()
                .map(this::mapToDeliveryTourListDTO)
                .toList();

        // Retourner la réponse
        return new DeliveryTourListResponseDTO(
                tourList,
                deliveryTourPage.getTotalElements(),
                deliveryTourPage.getTotalPages(),
                deliveryTourPage.getNumber(),
                deliveryTourPage.getSize(),
                deliveryTourPage.hasNext(),
                deliveryTourPage.hasPrevious()
        );
    }

    @Override
    @Transactional
    public void updateDeliveryTour(Long tourId, UpdateDeliveryTourDTO dto) {

        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Accès refusé");
        }

        DeliveryTour deliveryTour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new EntityNotFoundException("Tournée non trouvée"));

        if (!deliveryTour.canBeModified()) {
            throw new IllegalStateException("Modification impossible : la tournée n'est plus au statut Assignée.");
        }

        LocalDate oldDate = deliveryTour.getDeliveryDate();
        Driver oldDriver = deliveryTour.getDriver();

        LocalDate targetDate = dto.getDeliveryDate() != null ? dto.getDeliveryDate() : deliveryTour.getDeliveryDate();
        Driver targetDriver = deliveryTour.getDriver();
        if (dto.getDriverId() != null) {
            targetDriver = deliveryDriverRepository.findById(dto.getDriverId())
                    .orElseThrow(() -> new EntityNotFoundException("Livreur introuvable"));
        }

        List<DeliveryTourStatus> activeStatuses = java.util.List.of(
                DeliveryTourStatus.ASSIGNEE,
                DeliveryTourStatus.EN_COURS);
        if (deliveryTourRepository.countActiveToursForDriverOnDateExcluding(
                targetDriver.getId(), targetDate, activeStatuses, tourId) > 0) {
            throw new IllegalStateException(
                    "Ce livreur a déjà une tournée assignée ou en cours à la date choisie. Choisissez une autre date ou un autre livreur.");
        }

        if (dto.getDeliveryDate() != null) {
            deliveryTour.setDeliveryDate(dto.getDeliveryDate());
        }
        if (dto.getDriverId() != null) {
            deliveryTour.setDriver(targetDriver);
        }
        if (dto.getVehicleInfo() != null) {
            deliveryTour.setVehicleTypePlate(dto.getVehicleInfo());
        }
        if (dto.getNotes() != null) {
            deliveryTour.setNotes(dto.getNotes());
        }
        deliveryTour.setUpdatedBy(currentUser);

        if (dto.getOrderIds() != null) {
            java.util.Set<Long> idsToKeep = new java.util.HashSet<>(dto.getOrderIds());
            if (idsToKeep.isEmpty()) {
                throw new IllegalArgumentException("Pas de tournée sans commande");
            }
            if (deliveryTour.getOrders() != null) {
                for (Order order : new java.util.ArrayList<>(deliveryTour.getOrders())) {
                    if (!idsToKeep.contains(order.getId())) {
                        order.setDeliveryTour(null);
                        orderRepository.save(order);
                    }
                }
            }
            deliveryTour = deliveryTourRepository.findById(tourId).orElseThrow();
            if (deliveryTour.getOrders() == null || deliveryTour.getOrders().isEmpty()) {
                throw new IllegalArgumentException("Pas de tournée sans commande");
            }
        }

        deliveryTourRepository.save(deliveryTour);
        deliveryTourRepository.flush();

        LocalDate finalDate = deliveryTour.getDeliveryDate();
        if (dto.getDeliveryDate() != null && oldDate != null && !oldDate.equals(finalDate)) {
            for (Order o : orderRepository.findByDeliveryTour_Id(tourId)) {
                o.setDeliveryDate(finalDate);
                orderRepository.save(o);
            }
        }

        DeliveryTour reloaded = deliveryTourRepository.findById(tourId).orElseThrow();
        List<Order> ordersInTour = orderRepository.findByDeliveryTour_Id(tourId);
        int orderCount = ordersInTour.size();

        String summary = summarizeTourChanges(oldDate, reloaded.getDeliveryDate(), oldDriver, reloaded.getDriver());
        try {
            if (oldDriver != null && reloaded.getDriver() != null
                    && !oldDriver.getId().equals(reloaded.getDriver().getId())) {
                driverNotificationService.notifyTourReassignedAway(oldDriver, reloaded.getTourNumber(), reloaded.getDeliveryDate());
            }
            driverNotificationService.notifyTourUpdated(reloaded, orderCount);
            for (Order o : ordersInTour) {
                employeeNotificationService.notifyTourUpdatedForEmployee(o, reloaded);
            }
            logisticsManagerNotificationService.notifyTourModificationConfirmation(currentUser, reloaded, summary);
        } catch (Exception e) {
            log.warn("Notifications post-modification tournée {} : {}", tourId, e.getMessage());
        }

        log.info("Tournée {} mise à jour par {}", tourId, currentUser.getEmail());
    }

    private String summarizeTourChanges(LocalDate oldDate, LocalDate newDate, Driver oldDriver, Driver newDriver) {
        StringBuilder sb = new StringBuilder();
        if (oldDate != null && newDate != null && !oldDate.equals(newDate)) {
            sb.append("Date : ").append(oldDate).append(" → ").append(newDate).append(". ");
        }
        if (oldDriver != null && newDriver != null && !oldDriver.getId().equals(newDriver.getId())) {
            String o = driverDisplayName(oldDriver);
            String n = driverDisplayName(newDriver);
            sb.append("Livreur : ").append(o).append(" → ").append(n).append(". ");
        }
        if (sb.isEmpty()) {
            return "Véhicule, notes ou liste des commandes ont pu être ajustés.";
        }
        return sb.toString().trim();
    }

    private static String driverDisplayName(Driver d) {
        if (d == null || d.getUser() == null) {
            return "—";
        }
        String fn = Optional.ofNullable(d.getUser().getFirstName()).orElse("");
        String ln = Optional.ofNullable(d.getUser().getLastName()).orElse("");
        return (fn + " " + ln).trim();
    }

    // ========================================
    // RETIRER UNE COMMANDE D'UNE TOURNÉE
    // ========================================
    @Override
    @Transactional
    public void removeOrderFromTour(Long tourId, Long orderId) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut retirer une commande d'une tournée");
        }
        DeliveryTour tour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new EntityNotFoundException("Tournée non trouvée"));
        if (tour.getStatus() != DeliveryTourStatus.ASSIGNEE) {
            throw new IllegalStateException("Seules les tournées au statut Assignée peuvent être modifiées (retrait de commande)");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande non trouvée"));
        if (order.getDeliveryTour() == null || !order.getDeliveryTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Cette commande n'appartient pas à la tournée");
        }
        order.setDeliveryTour(null);
        orderRepository.save(order);
        log.info("Commande {} retirée de la tournée {} par {}", orderId, tourId, currentUser.getEmail());
    }

    // ========================================
    // ANNULATION TOURNÉE PAR LE RL
    // ========================================
    // Quand : tournée ASSIGNEE (livreur pas parti) ou EN_COURS sans livraison démarrée.
    // Effet : tournée ANNULEE, toutes les commandes remises EN_ATTENTE (deliveryTour = null),
    // notification salarié + livreur.

    @Override
    @Transactional
    public void cancelDeliveryTour(Long tourId, CancelDeliveryTourDTO dto) {
        // 1. Vérifier que l'utilisateur connecté est bien un RL
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut annuler une tournée");
        }

        // 2. Charger la tournée
        DeliveryTour tour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new EntityNotFoundException("Tournée non trouvée"));

        // 3. Vérifier qu'aucune livraison n'a été démarrée (on ne peut pas annuler si une commande est EN_COURS, ARRIVE ou LIVREE)
        if (tour.getOrders() != null) {
            for (Order order : tour.getOrders()) {
                if (order.getStatus() == OrderStatus.EN_COURS
                        || order.getStatus() == OrderStatus.ARRIVE
                        || order.getStatus() == OrderStatus.LIVREE) {
                    throw new RuntimeException(
                            "Impossible d'annuler : des livraisons sont déjà en cours"
                    );
                }
            }
        }

        // 4. Vérifier que la tournée est annulable (ASSIGNEE ou EN_COURS tant qu'aucune livraison démarrée)
        if (tour.getStatus() != DeliveryTourStatus.ASSIGNEE && tour.getStatus() != DeliveryTourStatus.EN_COURS) {
            throw new IllegalStateException(
                    "Seules les tournées ASSIGNEE ou EN_COURS (sans livraison démarrée) peuvent être annulées"
            );
        }

        DeliveryTourStatus oldStatus = tour.getStatus();

        // 5. Marquer la tournée comme annulée
        tour.setStatus(DeliveryTourStatus.ANNULEE);
        tour.setCancellationReason(dto.getReason());
        tour.setCancelledAt(LocalDateTime.now());
        tour.setCancelledBy(currentUser);

        // 6. Notifier le salarié puis remettre chaque commande en EN_ATTENTE, détacher de la tournée et effacer l'historique de livraison
        if (tour.getOrders() != null) {
            for (Order order : tour.getOrders()) {
                employeeNotificationService.notifyTourCancelled(order, dto.getReason());
                order.setStatus(OrderStatus.EN_ATTENTE);
                order.setDeliveryTour(null);
                order.setValidatedAt(null);
                order.setPickupStartedAt(null);
                order.setDeliveryStartedAt(null);
                order.setDeliveryArrivedAt(null);
                order.setDeliveryCompletedAt(null);
                orderRepository.save(order);
            }
        }

        // 7. Notifier le livreur que la tournée est annulée
        if (tour.getDriver() != null && tour.getDriver().getUser() != null
                && tour.getDriver().getUser().getEmail() != null && !tour.getDriver().getUser().getEmail().isBlank()) {
            driverNotificationService.notifyTourCancelled(tour);
        }

        deliveryTourRepository.save(tour);
        log.info("Tournée {} annulée par {}", tour.getTourNumber(), currentUser.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportDeliveryTours(String tourNumber, DeliveryTourStatus status) {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut exporter les tournées");
        }

        // 2. NORMALISATION DU TERME DE RECHERCHE
        String tourNumberFilter = (tourNumber != null && !tourNumber.trim().isEmpty())
                ? tourNumber.trim()
                : null;

        // 3. RÉCUPÉRATION TOUTES LES TOURNÉES (sans pagination)
        List<DeliveryTour> tours = deliveryTourRepository
                .findDeliveryTourWithFilters(tourNumberFilter, status, Pageable.unpaged())
                .getContent();

        // 4. GÉNÉRATION EXCEL
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tournées de livraison");

            // Style en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "N° Tournée",
                    "Date livraison",
                    "Chauffeur",
                    "Véhicule",
                    "Nb commandes",
                    "Statut"
            };

            // Création ligne en-tête
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplissage des données
            int rowNum = 1;
            for (DeliveryTour tour : tours) {
                Row row = sheet.createRow(rowNum++); // rowNum++ = utilise PUIS incrémente

                // N° Tournée (colonne 0)
                row.createCell(0).setCellValue(tour.getTourNumber());

                // Date livraison (colonne 1)
                row.createCell(1).setCellValue(
                        tour.getDeliveryDate() != null ?
                                tour.getDeliveryDate().toString() : "Non définie"
                );

                // Chauffeur (colonne 2)
                String driverName = "Non assigné";
                if (tour.getDriver() != null && tour.getDriver().getUser() != null) {
                    Users driverUser = tour.getDriver().getUser();
                    driverName = driverUser.getFirstName() + " " + driverUser.getLastName();
                }
                row.createCell(2).setCellValue(driverName);

                // Véhicule (colonne 3)
                String vehicleInfo = "Non spécifié";
                if (tour.getVehicleTypePlate() != null) {
                    vehicleInfo = tour.getVehicleTypePlate();
                }
                row.createCell(3).setCellValue(vehicleInfo);

                // Nb commandes (colonne 4)
                int orderCount = tour.getOrders() != null ? tour.getOrders().size() : 0;
                row.createCell(4).setCellValue(orderCount);

                // Statut (colonne 5)
                row.createCell(5).setCellValue(tour.getStatus().getDisplayName());
            }

            autoSizeColumnsSafe(sheet, headers.length);

            // ByteArrayOutputStream = "Un conteneur temporaire en mémoire"
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // workbook.write(outputStream) = "Écris le Excel dans ce conteneur"
            workbook.write(outputStream);

            // toByteArray() = "Transforme en tableau d'octets"
            // ByteArrayResource = "Prépare pour l'envoi au navigateur"
            return new ByteArrayResource(outputStream.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    @Override
    public DeliveryTourStatsDTO getDeliveryTourStats() {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les statistiques des tournées");
        }

        // 2. COMPTAGE PAR STATUT (4 statuts : ASSIGNEE, EN_COURS, TERMINEE, ANNULEE)
        long assigned = deliveryTourRepository.countByStatus(DeliveryTourStatus.ASSIGNEE);
        long inProgress = deliveryTourRepository.countByStatus(DeliveryTourStatus.EN_COURS);
        long completed = deliveryTourRepository.countByStatus(DeliveryTourStatus.TERMINEE);
        long cancelled = deliveryTourRepository.countByStatus(DeliveryTourStatus.ANNULEE);
        long total = assigned + inProgress + completed + cancelled;

        // 3. RETOUR DU DTO
        return new DeliveryTourStatsDTO(total, assigned, inProgress, completed, cancelled);
    }

    // ============================================================================
    // GESTION DES RÉCLAMATIONS
    // ============================================================================

    @Override
    public ClaimStatsDTO getClaimStats() {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les statistiques des retours");
        }
        long total = claimRepository.count();
        long validatedCount = claimRepository.countByStatus(ClaimStatus.VALIDE);
        long rejectedCount = claimRepository.countByStatus(ClaimStatus.REJETE);
        long reintegratedCount = claimRepository.countByDecisionType(ClaimDecisionType.REINTEGRATION);
        BigDecimal sumRefund = claimRepository.sumRefundAmount();
        BigDecimal totalRefundAmount = sumRefund != null ? sumRefund : BigDecimal.ZERO;
        return new ClaimStatsDTO(total, validatedCount, rejectedCount, reintegratedCount, totalRefundAmount);
    }

    @Override
    public ClaimListResponseDTO getClaims(int page, int size, String search, ClaimStatus status) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter la liste des réclamations");
        }
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<Claim> claimPage = claimRepository.findAllWithFilters(searchTerm, status, pageable);

        List<ClaimListItemDTO> content = claimPage.getContent().stream()
                .map(this::mapToClaimListItemDTO)
                .collect(Collectors.toList());

        return new ClaimListResponseDTO(
                content,
                claimPage.getTotalElements(),
                claimPage.getTotalPages(),
                claimPage.getNumber(),
                claimPage.getSize(),
                claimPage.hasNext(),
                claimPage.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayResource exportClaims(String search, ClaimStatus status) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut exporter les réclamations");
        }
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        List<Claim> claims = claimRepository.findAllWithFilters(searchTerm, status, Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Réclamations");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "N° Réclamation",
                    "N° Commande",
                    "Salarié",
                    "Produit",
                    "Quantité",
                    "Type problème",
                    "Statut",
                    "Date création",
                    "Décision",
                    "Montant remboursé"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (Claim claim : claims) {
                ClaimListItemDTO dto = mapToClaimListItemDTO(claim);
                Row row = sheet.createRow(rowNum++);

                if (dto.getClaimId() != null) {
                    row.createCell(0).setCellValue(dto.getClaimId());
                } else {
                    row.createCell(0).setCellValue("");
                }
                row.createCell(1).setCellValue(dto.getOrderNumber() != null ? dto.getOrderNumber() : "");
                row.createCell(2).setCellValue(dto.getEmployeeName() != null ? dto.getEmployeeName() : "");
                row.createCell(3).setCellValue(dto.getProductName() != null ? dto.getProductName() : "");
                if (dto.getQuantity() != null) {
                    row.createCell(4).setCellValue(dto.getQuantity());
                } else {
                    row.createCell(4).setCellValue("");
                }
                row.createCell(5).setCellValue(dto.getProblemTypeLabel() != null ? dto.getProblemTypeLabel() : "");
                row.createCell(6).setCellValue(dto.getStatus() != null ? dto.getStatus() : "");
                row.createCell(7).setCellValue(
                        dto.getCreatedAt() != null ? dto.getCreatedAt().format(dtFormatter) : "");
                row.createCell(8).setCellValue(dto.getDecisionLabel() != null ? dto.getDecisionLabel() : "");
                if (dto.getRefundAmount() != null) {
                    row.createCell(9).setCellValue(dto.getRefundAmount().doubleValue());
                } else {
                    row.createCell(9).setCellValue("");
                }
            }

            autoSizeColumnsSafe(sheet, headers.length);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    @Override
    public ClaimDetailDTO getClaimById(Long id) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les détails d'une réclamation");
        }
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        return mapToClaimDetailDTO(claim);
    }

    @Override
    @Transactional
    public void validateClaim(Long id, ValidateClaimDTO dto) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut valider une réclamation");
        }
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        if (claim.getStatus() != ClaimStatus.EN_ATTENTE) {
            throw new RuntimeException("Seule une réclamation en attente peut être validée");
        }
        claim.setDecisionType(dto.getDecisionType());

        if (claim.getOrderItem() == null) {
            throw new RuntimeException("Réclamation sans ligne commande associée : traitement impossible");
        }

        BigDecimal lineRefund = claim.getOrderItem().getSubtotal();
        if (lineRefund == null || lineRefund.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Montant de la ligne commande introuvable ou invalide pour le remboursement");
        }

        if (dto.getDecisionType() == ClaimDecisionType.REINTEGRATION) {
            // Réintégration au stock + remboursement du montant de la ligne (quantité concernée)
            if (claim.getOrderItem().getProduct() == null) {
                throw new RuntimeException("Réclamation sans produit associé : impossible de réintégrer au stock");
            }
            Product product = claim.getOrderItem().getProduct();
            int quantityOrdered = claim.getOrderItem().getQuantity() != null ? claim.getOrderItem().getQuantity() : 0;
            if (quantityOrdered <= 0) {
                throw new RuntimeException("Quantité commandée invalide pour la réintégration");
            }
            int quantity = dto.getQuantityToReintegrate() != null
                    ? dto.getQuantityToReintegrate()
                    : quantityOrdered;
            if (quantity <= 0 || quantity > quantityOrdered) {
                throw new RuntimeException("La quantité à réintégrer doit être entre 1 et " + quantityOrdered + " (quantité commandée)");
            }
            int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            product.setCurrentStock(currentStock + quantity);
            productRepository.save(product);
            claim.setRefundAmount(lineRefund);
        } else {
            // Remboursement seul : montant = sous-total ligne (saisie front ignorée)
            claim.setRefundAmount(lineRefund);
        }
        claim.setStatus(ClaimStatus.VALIDE);
        claim.setProcessedAt(LocalDateTime.now());
        claim.setProcessedBy(currentUser);
        claimRepository.save(claim);

        try {
            employeeNotificationService.notifyClaimValidated(claim, dto.getDecisionType(), claim.getRefundAmount());
        } catch (Exception e) {
            log.warn("Notification salarié après validation réclamation {} : {}", id, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void rejectClaim(Long id, RejectClaimDTO dto) {
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut rejeter une réclamation");
        }
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        if (claim.getStatus() != ClaimStatus.EN_ATTENTE) {
            throw new RuntimeException("Seule une réclamation en attente peut être rejetée");
        }
        claim.setStatus(ClaimStatus.REJETE);
        claim.setRejectionReason(dto.getRejectionReason());
        claim.setProcessedAt(LocalDateTime.now());
        claim.setProcessedBy(currentUser);
        claimRepository.save(claim);
    }

   //----------- Méthode Tableau de Bord ------------

    /**
     * KPIs du tableau de bord RL : commandes en attente, en retard, tournées actives, livrées ce mois.
     * Les deux premiers compteurs utilisent le même périmètre que la planification des tournées
     * ({@link OrderRepository#findEligibleOrdersForDate}) : EN_ATTENTE, sans tournée, salarié actif.
     * Les « en retard » sont le sous-ensemble avec {@code deliveryDate} strictement avant aujourd'hui
     * (identique à {@link OrderRepository#countOverduePendingUnassigned} et au badge du calendrier).
     */
    @Override
    public RLDashboardKpisDTO getDashboardKpis() {
        LocalDate today = LocalDate.now();
        // EN_ATTENTE, pas encore en tournée, employé actif (planifiable par le RL)
        long commandesEnAttente = orderRepository.countPendingUnassignedActiveEmployee(OrderStatus.EN_ATTENTE);
        // Sous-ensemble : date de livraison dépassée (aligné calendrier « X en retard »)
        long commandesEnRetard = orderRepository.countOverduePendingUnassigned(OrderStatus.EN_ATTENTE, today);
        // Tournées dont le statut est EN_COURS
        long tourneesActives = deliveryTourRepository.countByStatus(DeliveryTourStatus.EN_COURS);
        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finAujourdhui = today.atTime(23, 59, 59, 999_999_999);
        // Commandes LIVREE avec deliveryCompletedAt dans le mois en cours
        long livreesCeMois = orderRepository.countByStatusAndDeliveryCompletedAtBetween(
                OrderStatus.LIVREE, debutMois, finAujourdhui);
        return new RLDashboardKpisDTO(commandesEnAttente, commandesEnRetard, tourneesActives, livreesCeMois);
    }

    /**
     * Effectif des tournées par statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) pour le graphique donut.
     */
    @Override
    public StatutTourneesDTO getStatutTournees() {
        // Map qui va contenir le nombre de tournées pour chaque statut (clé = nom du statut, valeur = effectif)
        Map<String, Long> parStatut = new LinkedHashMap<>();

        // Initialise tous les statuts à 0 pour que le front affiche les 4 libellés même sans donnée
        for (DeliveryTourStatus s : DeliveryTourStatus.values()) {
            parStatut.put(s.name(), 0L);
        }

        // Remplit la map avec les vrais effectifs retournés par le repository (comptage groupé par status)
        for (StatusCountDTO row : deliveryTourRepository.countGroupByStatus()) {
            parStatut.put(row.status().name(), row.count());
        }

        // Retourne le DTO pour alimenter le graphique donut
        return new StatutTourneesDTO(parStatut);
    }

    /** Graphique "Commandes par jour" : 7 derniers jours (date, nbCommandes). */
    @Override
    public List<CommandesParJourDTO> getCommandesParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<CommandesParJourDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.atTime(23, 59, 59, 999_999_999);
            long nbCommandes = orderRepository.countByCreatedAtBetween(dayStart, dayEnd);
            result.add(new CommandesParJourDTO(day.format(formatter), nbCommandes));
        }
        return result;
    }

    /**
     * Graphique « Taux de retours (%) » : 7 derniers jours.
     * Pour chaque jour : taux = (commandes livrées ce jour ayant au moins une réclamation / commandes livrées ce jour) × 100.
     * La date de création de la réclamation peut être postérieure à la livraison. Si aucune livraison ce jour, le taux est 0.
     */
    @Override
    public List<TauxRetoursParJourDTO> getTauxRetoursParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<TauxRetoursParJourDTO> result = new ArrayList<>();
        // Pour chaque jour de la semaine, on calcule le taux de retours
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);//date du jour - i jours
            LocalDateTime dayStart = day.atStartOfDay();//date du jour - i jours à 00:00:00
            LocalDateTime dayEnd = day.atTime(23, 59, 59, 999_999_999);//date du jour - i jours à 23:59:59:999999999
            long nbLivrees = orderRepository.countByStatusAndDeliveryCompletedAtBetween(//commandes livrées dans la fenêtre [start, end]
                    OrderStatus.LIVREE, dayStart, dayEnd);
            long nbAvecReclamation = orderRepository.countDeliveredBetweenWithAtLeastOneClaim(//commandes livrées dans la fenêtre [start, end] ayant au moins une réclamation
                    OrderStatus.LIVREE, dayStart, dayEnd);
            //taux de retours = (commandes livrées ce jour ayant au moins une réclamation / commandes livrées ce jour) × 100 ou nbAvecReclamation * 100.0 / nbLivrees.
            double tauxPercent = nbLivrees > 0 ? (nbAvecReclamation * 100.0 / nbLivrees) : 0.0;

            //on ajoute le taux de retours au résultat (Math.round(tauxPercent * 100) / 100.0) pour arrondir le taux de retours à 2 décimales
            result.add(new TauxRetoursParJourDTO(day.format(formatter), Math.round(tauxPercent * 100) / 100.0));
        }
        return result;
    }

    @Override
    public List<TopProductUsageDTO> getTop5ProductUsage() {
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un Responsable Logistique peut consulter le top 5 produits.");
        }
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(30);
        Pageable top5 = PageRequest.of(0, 5);
        List<Object[]> rawTop5 = orderItemRepository.findTop5ProductsByQuantitySince(dateDebut, top5);
        long totalSum = orderItemRepository.sumQuantityByOrderCreatedAtAfter(dateDebut);
        List<TopProductUsageDTO> result = new ArrayList<>();
        for (Object[] row : rawTop5) {
            String productName = (String) row[0];
            long sumQuantity = ((Number) row[1]).longValue();
            double usagePercent = totalSum > 0 ? (sumQuantity * 100.0 / totalSum) : 0.0;
            result.add(new TopProductUsageDTO(productName, Math.round(usagePercent * 10) / 10.0));
        }
        return result;
    }

    @Override
    public List<LivraisonParJourDTO> getCommandesVsLivraisons() {
        return getLivraisonsParJour();
    }

    @Override
    public StockEtatGlobalDTO getStockEtatGlobal() {
        // Nombre total de produits
        long total = productRepository.count();
        // Produits sous le seuil minimum (stock > 0 et < seuil)
        long sousSeuil = productRepository.countLowStock();
        // Produits en rupture (stock = 0)
        long critique = productRepository.countByCurrentStock(0);
        // Produits avec stock normal (au-dessus du seuil)
        long normal = total - sousSeuil - critique;
        return new StockEtatGlobalDTO(normal, sousSeuil, critique);
    }

    /**
     * Par jour sur les 7 derniers jours : prévu à la date, livrées à la date prévue,
     * retard = prévues pour ce jour-là encore EN_ATTENTE (pas livrées / pas planifiées en tournée).
     * Utilisé pour le graphique Livraisons (Admin et RL).
     */
    @Override
    public List<LivraisonParJourDTO> getLivraisonsParJour() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        List<LivraisonParJourDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long nbPrevues = orderRepository.countByDeliveryDateExcludingCancelled(day, OrderStatus.ANNULEE);
            long nbLivreesALaDate = orderRepository.countByStatusAndDeliveryDate(OrderStatus.LIVREE, day);
            long nbRetard = orderRepository.countByStatusAndDeliveryDate(OrderStatus.EN_ATTENTE, day);
            result.add(new LivraisonParJourDTO(
                    day.format(formatter), nbPrevues, nbLivreesALaDate, nbRetard));
        }
        return result;
    }

    // ============================================================================
    // Méthodes Utilitaires
    // ============================================================================

    /**
     * Mappe une entité Claim vers le DTO de liste (une ligne du tableau des réclamations).
     */
    private ClaimListItemDTO mapToClaimListItemDTO(Claim c) {
        ClaimListItemDTO dto = new ClaimListItemDTO();
        dto.setClaimId(c.getId());
        dto.setOrderNumber(c.getOrder() != null ? c.getOrder().getOrderNumber() : null);
        // Nom du salarié (client) : prénom + nom de l'utilisateur lié à l'employé
        if (c.getEmployee() != null && c.getEmployee().getUser() != null) {
            Users u = c.getEmployee().getUser();
            dto.setEmployeeName(u.getFirstName() + " " + u.getLastName());
        } else { // sinon on met null
            dto.setEmployeeName(null);
        }
        // Si l'item de la commande et le produit existent : nom, image et quantité du produit concerné
        // sinon on met null
        if (c.getOrderItem() != null && c.getOrderItem().getProduct() != null) {
            dto.setProductName(c.getOrderItem().getProduct().getName());
            dto.setProductImage(c.getOrderItem().getProduct().getImage());
            dto.setQuantity(c.getOrderItem().getQuantity());
        } else {
            dto.setProductName(null);
            dto.setProductImage(null);
            dto.setQuantity(null);
        }
        // Type de problème : label du type de problème
        dto.setProblemTypeLabel(c.getProblemType() != null ? c.getProblemType().getName() : null);
        // Statut : label du statut
        dto.setStatus(c.getStatus() != null ? c.getStatus().getLabel() : null);
        // Date de création
        dto.setCreatedAt(c.getCreatedAt());
        // Décision : réintégration / remboursement si validé ; « Rejeté » si rejeté (évite colonne vide)
        if (c.getStatus() == ClaimStatus.REJETE) {
            dto.setDecisionLabel(ClaimStatus.REJETE.getLabel());
        } else {
            dto.setDecisionLabel(c.getDecisionType() != null ? c.getDecisionType().getLabel() : null);
        }
        dto.setRefundAmount(c.getRefundAmount());
        return dto;
    }

    /**
     * Mappe une entité Claim vers le DTO de détail (écran « Voir détails » d'une réclamation).
     */
    private ClaimDetailDTO mapToClaimDetailDTO(Claim c) {
        ClaimDetailDTO dto = new ClaimDetailDTO();
        dto.setClaimId(c.getId());
        dto.setOrderNumber(c.getOrder() != null ? c.getOrder().getOrderNumber() : null);
        dto.setStatus(c.getStatus() != null ? c.getStatus().getLabel() : null);
        dto.setCreatedAt(c.getCreatedAt());
        // Infos du salarié (nom et téléphone)
        if (c.getEmployee() != null && c.getEmployee().getUser() != null) {
            Users u = c.getEmployee().getUser();
            dto.setEmployeeName(u.getFirstName() + " " + u.getLastName());
            dto.setEmployeePhone(u.getPhone());
        }
        // Produit concerné : quantité commandée, sous-total, id / nom / image du produit(orderIem: un item de la commande)
        // sinon on met null
        if (c.getOrderItem() != null) {
            OrderItem oi = c.getOrderItem();
            dto.setQuantityOrdered(oi.getQuantity());
            dto.setSubtotalProduct(oi.getSubtotal());
            if (oi.getProduct() != null) {
                dto.setProductId(oi.getProduct().getId());
                dto.setProductName(oi.getProduct().getName());
                dto.setProductImage(oi.getProduct().getImage());
            }
        }
        // Type de problème : label du type de problème
        dto.setProblemTypeLabel(c.getProblemType() != null ? c.getProblemType().getName() : null);
        // Commentaire
        dto.setComment(c.getComment());
        // URLs des photos
        dto.setPhotoUrls(c.getPhotoUrls());
        // Décision : réintégration / remboursement si validé ; « Rejeté » si rejeté
        if (c.getStatus() == ClaimStatus.REJETE) {
            dto.setDecisionTypeLabel(ClaimStatus.REJETE.getLabel());
        } else {
            dto.setDecisionTypeLabel(c.getDecisionType() != null ? c.getDecisionType().getLabel() : null);
        }
        dto.setRefundAmount(c.getRefundAmount());
        dto.setRejectionReason(c.getRejectionReason());
        dto.setProcessedAt(c.getProcessedAt());
        if (c.getProcessedBy() != null) {
            String firstName = c.getProcessedBy().getFirstName() != null ? c.getProcessedBy().getFirstName().trim() : "";
            String lastName = c.getProcessedBy().getLastName() != null ? c.getProcessedBy().getLastName().trim() : "";
            dto.setProcessedByName((firstName + " " + lastName).trim());
        }
        return dto;
    }

    /**
     * Mappe une commande vers le DTO des commandes éligibles pour une tournée.
     * Inclut les infos client/adresse et les préférences de livraison du salarié (informatif pour le RL).
     * « Correspond aux préférences » = la date de la tournée (tourDeliveryDate) tombe un jour préféré par le salarié.
     */
    private EligibleOrderDTO mapToEligibleOrderDTO(Order order, LocalDate tourDeliveryDate) {
        // ——— Client et adresse ———
        String customerName = order.getEmployee() != null && order.getEmployee().getUser() != null
                ? order.getEmployee().getUser().getFirstName() + " " + order.getEmployee().getUser().getLastName()
                : "";
        Address addr = order.getEmployee() != null ? getPrimaryAddress(order.getEmployee()) : null;
        String formattedAddress = (addr != null && addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank())
                ? addr.getFormattedAddress() : null;

        // ——— Préférences de livraison (informatif uniquement : le RL peut créer la tournée même si pas de correspondance) ———
        EmployeeDeliveryPreference pref = order.getEmployee() != null ? order.getEmployee().getEmployeeDeliveryPreference() : null;
        boolean hasPreferences = (pref != null);
        // Jours préférés (ex: ["LUNDI", "VENDREDI"])
        List<String> preferredDays = (pref != null && pref.getPreferredDays() != null)
                ? new ArrayList<>(pref.getPreferredDays()) : null;
        // Créneau affiché (ex: "Matin (8h - 12h)")
        String preferredTimeSlot = (pref != null && pref.getPreferredTimeSlot() != null)
                ? pref.getPreferredTimeSlot().getDisplayName() : null;
        // Mode de livraison affiché (ex: "Domicile", "Bureau")
        String preferredDeliveryMode = (pref != null && pref.getDeliveryMode() != null)
                ? pref.getDeliveryMode().getDisplayName() : null;
        // true si la date de la tournée (celle choisie par le RL) tombe un jour préféré par le salarié
        Boolean matchesPreferences = null;
        if (hasPreferences && tourDeliveryDate != null && pref.getPreferredDays() != null && !pref.getPreferredDays().isEmpty()) {
            String dayOfWeekEn = tourDeliveryDate.getDayOfWeek().name();
            String dayOfWeekFr = dayOfWeekEnToFrench(dayOfWeekEn);
            matchesPreferences = pref.isAvailableOn(dayOfWeekFr);
        }

        // En retard : date de livraison prévue déjà dépassée (commande en attente)
        Integer daysOverdue = null;
        if (order.getDeliveryDate() != null && order.getDeliveryDate().isBefore(java.time.LocalDate.now())) {
            daysOverdue = (int) java.time.temporal.ChronoUnit.DAYS.between(order.getDeliveryDate(), java.time.LocalDate.now());
        }

        return new EligibleOrderDTO(
                order.getId(),
                order.getOrderNumber(),
                customerName,
                formattedAddress,
                preferredDays,
                preferredTimeSlot,
                preferredDeliveryMode,
                matchesPreferences,
                hasPreferences,
                daysOverdue
        );
    }

    /**
     * On commence avec une commande comme “graine” du lot → calcule le centre du lot → ajoute la commande restante la plus proche → recalcul du centre → répète jusqu’à remplir le lot.
     *
     * Important : les commandes sans lat/lng ne doivent pas disparaître. Elles sont mises dans des lots fallback (taille max = lotSize)
     * pour que le total des commandes éligibles corresponde au total affiché sur l'écran lots.
     */
    /** Jour de la semaine en anglais (MONDAY) → français (LUNDI) pour comparaison avec préférences stockées en français. */
    private static String dayOfWeekEnToFrench(String dayEn) {
        if (dayEn == null) return null;
        return switch (dayEn.toUpperCase()) {
            case "MONDAY" -> "LUNDI";
            case "TUESDAY" -> "MARDI";
            case "WEDNESDAY" -> "MERCREDI";
            case "THURSDAY" -> "JEUDI";
            case "FRIDAY" -> "VENDREDI";
            case "SATURDAY" -> "SAMEDI";
            case "SUNDAY" -> "DIMANCHE";
            default -> dayEn;
        };
    }

    private List<List<Order>> groupOrdersByProximity(List<Order> orders, int lotSize) {
        if (orders == null || orders.isEmpty() || lotSize <= 0) {
            return new ArrayList<>(); // Si pas de commandes ou lotSize <= 0, retourne vide
        }

        // Tri des commandes par ID pour un ordre stable
        // Exemple : sorted = [O1, O2, O3, O4, O5]
        List<Order> sorted = orders.stream()
                .sorted(Comparator.comparing(Order::getId))
                .toList();

        // Séparer :
        // - geoRemaining : commandes avec lat/lng (regroupement proximité)
        // - noGeo : commandes sans lat/lng (lots fallback)
        List<Order> geoRemaining = new ArrayList<>();
        List<Order> noGeo = new ArrayList<>();
        for (Order o : sorted) {
            Address addr = getPrimaryAddress(o.getEmployee());
            if (addr != null && addr.getLatitude() != null && addr.getLongitude() != null) {
                geoRemaining.add(o);
            } else {
                noGeo.add(o);
            }
        }

        List<List<Order>> lots = new ArrayList<>(); // variable qui va stocker les lots
        while (!geoRemaining.isEmpty()) { // Tant qu'il reste des commandes à traiter

            // --- Création d'un nouveau lot ---
            List<Order> lot = new ArrayList<>(); // nouveau lot vide
            lot.add(geoRemaining.remove(0)); // on prend la première commande comme "graine"
            // Exemple : lot = [O1], geoRemaining = [O2, O3, O4, O5]

            // --- Remplissage du lot ---
            // Tant que le lot n'est pas plein et qu'il reste des commandes
            while (lot.size() < lotSize && !geoRemaining.isEmpty()) {

                // --- Calcul du centre du lot ---
                // Centre = moyenne des latitudes et longitudes des commandes dans le lot
                double centerLat = lot.stream()
                        .mapToDouble(o -> getPrimaryAddress(o.getEmployee()).getLatitude().doubleValue())
                        .average().orElse(0);
                double centerLng = lot.stream()
                        .mapToDouble(o -> getPrimaryAddress(o.getEmployee()).getLongitude().doubleValue())
                        .average().orElse(0);
                // Exemple : si lot = [O1, O2]
                // centerLat = (O1.lat + O2.lat)/2
                // centerLng = (O1.lng + O2.lng)/2

                // --- Chercher la commande la plus proche du centre ---
                Order nearest = null;               // commande la plus proche
                double minDist = Double.MAX_VALUE;  // distance minimale initiale
                int nearestIndex = -1;              // index de la commande la plus proche dans 'geoRemaining'

                for (int i = 0; i < geoRemaining.size(); i++) {
                    Order c = geoRemaining.get(i); // commande courante
                    Address a = getPrimaryAddress(c.getEmployee());
                    double d = GeoUtil.calculateDistanceKm(
                            centerLat, centerLng,
                            a.getLatitude().doubleValue(), a.getLongitude().doubleValue());
                    if (d < minDist) { // Si plus proche que toutes les précédentes
                        minDist = d;      // mise à jour de la distance minimale
                        nearest = c;      // on garde la commande la plus proche
                        nearestIndex = i; // son index pour suppression après ajout
                    }
                }

                if (nearest == null || nearestIndex < 0) {
                    break;
                }

                lot.add(nearest);                   // Ajouter la commande la plus proche au lot
                geoRemaining.remove(nearestIndex);  // Retirer la commande de geoRemaining
            }

            lots.add(lot); // Ajouter le lot complet à la liste des lots
        }

        // Fallback : les commandes sans géolocalisation en lots simples (pour ne pas les perdre)
        if (!noGeo.isEmpty()) {
            for (int i = 0; i < noGeo.size(); i += lotSize) {
                int end = Math.min(i + lotSize, noGeo.size());
                lots.add(new ArrayList<>(noGeo.subList(i, end)));
            }
        }

        return lots;
    }

    /**
     * Retourne l'adresse principale de l'employé (livraison).
     * Utilisée pour lat/lng et formattedAddress des commandes éligibles.
     */
    private Address getPrimaryAddress(Employee employee) {
        if (employee == null || employee.getAddresses() == null || employee.getAddresses().isEmpty()) {
            return null;
        }
        return employee.getAddresses().stream()
                .filter(Address::isPrimary)
                .findFirst()
                .orElse(employee.getAddresses().get(0));
    }

    // ============================================================================
    // Méthodes utilitaires
    // ============================================================================
    /**
     * Mappe une entité DeliveryTour vers le DTO liste (N° Tour | Date | Chauffeur | Véhicule | Nb Cmd | Statut | id pour Actions).
     */
    private DeliveryTourListDTO mapToDeliveryTourListDTO(DeliveryTour deliveryTour) {
        DeliveryTourListDTO dto = new DeliveryTourListDTO();

        dto.setId(deliveryTour.getId());
        // 1. Informations tournée (N° Tour, Date)
        dto.setTourNumber(deliveryTour.getTourNumber());
        dto.setDeliveryDate(deliveryTour.getDeliveryDate());

        // 2. Chauffeur
        if (deliveryTour.getDriver() != null && deliveryTour.getDriver().getUser() != null) {
            dto.setDriverName( deliveryTour.getDriver().getUser().getFirstName() + " " +  deliveryTour.getDriver().getUser().getLastName());
        } else {
            dto.setDriverName("Non assigné");
        }

        // 3. Véhicule (format: "Type /Plaque)")
        if (deliveryTour.getVehicleTypePlate() != null){
            dto.setVehicle(deliveryTour.getVehicleTypePlate());
        }else {
            dto.setVehicle("Non spécifié");
        }

        // 4. Nombre de commandes
        dto.setOrderCount(deliveryTour.getOrders() != null ?
                deliveryTour.getOrders().size() : 0);

        // 5. Statut
        dto.setStatus(deliveryTour.getStatus());

        return dto;
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
        dto.setSupplierId(supplierOrder.getSupplier().getId());
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
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductCategory(item.getProduct().getCategory().getName());
        dto.setProductImage(item.getProduct().getImage());
        dto.setQuantite(item.getQuantity());
        return dto;
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
        dto.setProductsSummary(buildProductsSummary(supplierOrder.getItems())); // Construire le résumé des produits
        return dto;
    }


    /**
     * Construit le résumé des produits d'une commande (ex: "Riz (100), Huile (50)")
     *
     * @param items La liste des items (produits) de la commande
     * @return String contenant le résumé formaté des produits avec leurs quantités
     */
    private String buildProductsSummary(List<SupplierOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(item -> {
                    if (item == null) {
                        return "";
                    }
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    if (item.getProduct() == null) {
                        return "(" + qty + ")";
                    }
                    String name = item.getProduct().getName() != null ? item.getProduct().getName() : "?";
                    return name + " (" + qty + ")";
                })
                .filter(s -> s != null && !s.isEmpty())
                .collect(java.util.stream.Collectors.joining(", "));
    }


    /**
     * Concatène type + plaque pour {@link DeliveryTour#vehicleTypePlate} (même format que le parsing {@link #fillVehicleTypeAndPlate}).
     */
    private static String buildVehicleTypePlate(String vehicleType, String vehiclePlate) {
        String t = vehicleType != null ? vehicleType.trim() : "";
        String p = vehiclePlate != null ? vehiclePlate.trim() : "";
        if (t.isEmpty()) {
            return p.isEmpty() ? "Non spécifié" : p;
        }
        if (p.isEmpty()) {
            return t;
        }
        return t + " — " + p;
    }

    /**
     * Décompose le champ unique véhicule (ex. "Camion — DK-4521-A") en type et matricule.
     */
    private void fillVehicleTypeAndPlate(String vehicleTypePlate, DeliveryTourDetailsDTO dto) {
        if (vehicleTypePlate == null || vehicleTypePlate.isBlank()) {
            dto.setVehicleType(null);
            dto.setVehiclePlate(null);
            return;
        }
        String v = vehicleTypePlate.trim();
        String[] separators = {" — ", " – ", " - ", "—", "–", "-"};
        for (String sep : separators) {
            int idx = v.indexOf(sep);
            if (idx > 0) {
                dto.setVehicleType(v.substring(0, idx).trim());
                dto.setVehiclePlate(v.substring(idx + sep.length()).trim());
                return;
            }
        }
        dto.setVehicleType(v);
        dto.setVehiclePlate(null);
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






