package com.example.coopachat.services.LogisticsManager;

import com.example.coopachat.dtos.DeliveryDriver.AvailableDriverDTO;
import com.example.coopachat.dtos.DeliveryDriver.CancelDeliveryTourDTO;
import com.example.coopachat.dtos.DeliveryDriver.RegisterDriverRequestDTO;
import com.example.coopachat.dtos.delivery.*;
import com.example.coopachat.dtos.order.*;
import com.example.coopachat.dtos.products.ProductPreviewDTO;
import com.example.coopachat.dtos.products.ProductStockListItemDTO;
import com.example.coopachat.dtos.products.ProductStockListResponseDTO;
import com.example.coopachat.dtos.products.StockStatsDTO;
import com.example.coopachat.dtos.supplierOrders.*;
import com.example.coopachat.dtos.suppliers.SupplierListItemDTO;
import com.example.coopachat.entities.*;
import com.example.coopachat.entities.util.GeoUtil;
import com.example.coopachat.enums.*;
import com.example.coopachat.repositories.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
    private final SupplierRepository supplierRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final ProductRepository productRepository;
    private final SupplierOrderItemRepository supplierOrderItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final DeliveryDriverRepository deliveryDriverRepository;
    private final DeliveryTourRepository deliveryTourRepository;
    private final EmailService emailService;

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

        deliveryDriverRepository.save(newDriver);

        log.info("Livreur créé avec succès par le Responsable Logistique: {}", logisticsManager.getEmail());
    }

    @Override
    public List<SupplierListItemDTO> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .filter(Supplier::getIsActive)
                .map(supplier -> new SupplierListItemDTO(supplier.getId(), supplier.getName()))
                .collect(Collectors.toList());
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
    // 📤 EXPORT DU SUIVI DES STOCKS
    // ============================================================================
    @Override
    @Transactional
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
    // 📦 GESTION DES COMMANDES SALARIÉS
    // ============================================================================
    @Override
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

                    return new OrderEmployeeListItemDTO(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getEmployee().getUser().getFirstName() + " "
                                    + order.getEmployee().getUser().getLastName(),
                            order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null,
                            display,
                            deliveryFrequency,
                            order.getStatus().getLabel()
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

        //Récupérons la commande
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // On prépare un DTO contenant  - les infos générales de la commande - la liste des produits associés à la commande
        return new OrderItemDetailsDTO(
                order.getOrderNumber(),
                order.getCreatedAt().toLocalDate(),
                order.getEmployee().getUser().getFirstName() + " " + order.getEmployee().getUser().getLastName(),
                order.getStatus().getLabel(),
                order.getItems().stream().map(item -> new ProductPreviewDTO(
                        item.getProduct().getName(),
                        item.getProduct().getImage(),
                        item.getProduct().getCategory().getName(),
                        item.getProduct().getCurrentStock()

                )).toList()
        );

    }

    @Override
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
            // Augmente la largeur de la colonne 'i' pour que tout son contenu soit visible
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

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
    // ============================================================================
    // 🚚 GESTION DES TOURNÉES DE LIVRAISON
    // ============================================================================

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
                .map(this::mapToEligibleOrderDTO)
                .toList();
    }

    @Override
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
        // - deliveryDate = date demandée
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
                            .map(this::mapToEligibleOrderDTO)
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
    public List<AvailableDriverDTO> getAvailableDrivers() {

        // 1. VÉRIFICATION DES DROITS
        Users user = getCurrentUser();
        if (user.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut consulter les chauffeurs disponibles");
        }

        // 2. Tous les livreurs actifs sont éligibles
        return deliveryDriverRepository.findAll().stream()
                .filter(driver -> driver.getUser() != null && driver.getUser().getIsActive())
                .map(driver -> {
                    String fullName = driver.getUser().getFirstName() + " " + driver.getUser().getLastName();
                    return new AvailableDriverDTO(driver.getId(), fullName);
                })
                .toList();
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

        // Générer numéro unique
        String tourNumber = "PL-" + LocalDate.now().getYear() + "-" +
                String.format("%03d", deliveryTourRepository.count() + 1);
        tour.setTourNumber(tourNumber);

        // Informations de base
        tour.setDeliveryDate(dto.getDeliveryDate());
        tour.setDriver(driver);
        tour.setVehicleTypePlate(dto.getVehicleType());
        tour.setCreatedBy(currentUser);
        tour.setUpdatedBy(currentUser);
        tour.setNotes(dto.getNotes());
        // Tournée directement assignée au livreur (pas de brouillon PLANIFIEE)
        tour.setStatus(DeliveryTourStatus.ASSIGNEE);

        // 5. SAUVEGARDE
        DeliveryTour savedTour = deliveryTourRepository.save(tour);

        // 6. ASSIGNATION DES COMMANDES + passage en VALIDEE (RL valide en les mettant dans la tournée)
        for (Order order : orders) {
            order.setDeliveryTour(savedTour);
            order.setStatus(OrderStatus.VALIDEE);
            order.setValidatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        log.info("Tournée {} créée par {} avec {} commandes (chauffeur: {})",
                tourNumber, currentUser.getEmail(), orders.size(),
                driver.getUser().getFirstName());

    }

    @Override
    public DeliveryTourDetailsDTO getDeliveryTourDetails(Long tourId) {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut récupérer les détails d'une tournée");
        }

        // Récupérer la tournée avec ses relations
        DeliveryTour deliveryTour = deliveryTourRepository.findById(tourId).orElseThrow(()->new RuntimeException("Tournée introuvable"));

        DeliveryTourDetailsDTO dto = new DeliveryTourDetailsDTO();
        dto.setTourNumber(deliveryTour.getTourNumber());
        dto.setDeliveryDate(deliveryTour.getDeliveryDate());
        dto.setStatus(deliveryTour.getStatus());

        // Chauffeur
        if (deliveryTour.getDriver() != null) {
            dto.setDriverName(deliveryTour.getDriver().getUser().getFirstName()+ " "+ deliveryTour.getDriver().getUser().getLastName());
            dto.setDriverPhone(deliveryTour.getDriver().getUser().getPhone());
        }

        // Véhicule
        dto.setVehicleType(deliveryTour.getVehicleTypePlate());

        // Commandes
        dto.setOrderCount(deliveryTour.getOrders() != null ?
                deliveryTour.getOrders().size() : 0);


        return dto;

    }

    @Override
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

        // 1. Vérification droits
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Accès refusé");
        }

        // 2. Récupération tournée
        DeliveryTour deliveryTour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new EntityNotFoundException("Tournée non trouvée"));

        // 3. Vérification statut modifiable (PLANIFIEE ou ASSIGNEE)
        if (!deliveryTour.canBeModified()) {
            throw new IllegalStateException("Modification impossible : tournée déjà en cours ou terminée");
        }

        // 4. Mise à jour véhicule
        if (dto.getVehicleInfo() != null) {
            deliveryTour.setVehicleTypePlate(dto.getVehicleInfo());
        }

        // 5. Mise à jour notes
        if (dto.getNotes() != null) {
            deliveryTour.setNotes(dto.getNotes());
        }

        // 6. Mise à jour statut (si changée ET différente de l'actuelle)
        if (dto.getStatus() != null && dto.getStatus() != deliveryTour.getStatus()) {
            deliveryTour.setStatus(dto.getStatus());
        }

        // 7. Sauvegarde
        deliveryTourRepository.save(deliveryTour);
        log.info("Tournée {} mise à jour par {}", tourId, currentUser.getEmail());
    }

    @Override
    @Transactional
    public void cancelDeliveryTour(Long tourId, CancelDeliveryTourDTO dto) {

        // 1. VÉRIFICATION DES DROITS
        Users currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.LOGISTICS_MANAGER) {
            throw new RuntimeException("Seul un responsable logistique peut annuler une tournée");
        }

        // 2. RÉCUPÉRATION TOURNÉE
        DeliveryTour tour = deliveryTourRepository.findById(tourId)
                .orElseThrow(() -> new EntityNotFoundException("Tournée non trouvée"));

        // Vérifier statut annulable
        if (tour.getStatus() != DeliveryTourStatus.PLANIFIEE &&
                tour.getStatus() != DeliveryTourStatus.ASSIGNEE) {
            throw new IllegalStateException(
                    "Seules les tournées PLANIFIEE ou ASSIGNEE peuvent être annulées"
            );
        }

        // Sauvegarder ancien statut
        DeliveryTourStatus oldStatus = tour.getStatus();

        // Annuler
        tour.setStatus(DeliveryTourStatus.ANNULEE);
        tour.setCancelledAt(LocalDateTime.now());
        tour.setCancelledBy(currentUser);
        tour.setCancellationReason(dto.getReason());

        String driverUserEmail = tour.getDriver() != null ? tour.getDriver().getUser().getEmail() : null;
        String driverUserName = tour.getDriver() != null ? tour.getDriver().getUser().getFirstName() + " " + tour.getDriver().getUser().getLastName() : null;

        // Notifier le livreur si la tournée était assignée
        if (oldStatus == DeliveryTourStatus.ASSIGNEE && driverUserEmail != null) {
            emailService.sendTourCancellationToDriver(driverUserEmail, tour.getTourNumber(), tour.getCancellationReason(), driverUserName);
        }
        deliveryTourRepository.save(tour);
        log.info("Tournée {} annulée par {}", tourId, currentUser.getEmail());
    }

    @Override
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

            // Augmente la largeur de la colonne 'i' pour que tout son contenu soit visible
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

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

        // 2. COMPTAGE PAR STATUT
        long planned = deliveryTourRepository.countByStatus(DeliveryTourStatus.PLANIFIEE);
        long assigned = deliveryTourRepository.countByStatus(DeliveryTourStatus.ASSIGNEE);
        long inProgress = deliveryTourRepository.countByStatus(DeliveryTourStatus.EN_COURS);
        long completed = deliveryTourRepository.countByStatus(DeliveryTourStatus.TERMINEE);
        long cancelled = deliveryTourRepository.countByStatus(DeliveryTourStatus.ANNULEE);

        // 3. RETOUR DU DTO
        return new DeliveryTourStatsDTO(
                planned,
                assigned,
                inProgress,
                completed,
                cancelled
        );
    }


    /** Mappe une commande vers le DTO (orderId, orderNumber, customerName, formattedAddress). */
    private EligibleOrderDTO mapToEligibleOrderDTO(Order order) {
        String customerName = order.getEmployee() != null && order.getEmployee().getUser() != null
                ? order.getEmployee().getUser().getFirstName() + " " + order.getEmployee().getUser().getLastName()
                : "";
        Address addr = getPrimaryAddress(order.getEmployee());
        String formattedAddress = (addr != null && addr.getFormattedAddress() != null && !addr.getFormattedAddress().isBlank())
                ? addr.getFormattedAddress() : null;
        return new EligibleOrderDTO(
                order.getId(),
                order.getOrderNumber(),
                customerName,
                formattedAddress
        );
    }

    /**
     *
     * On commence avec une commande comme “graine” du lot → calcule le centre du lot → ajoute la commande restante la plus proche → recalcul du centre → répète jusqu’à remplir le lot.
     * Les commandes sans lat/lng sont ignorées (adresse obligatoire à la commande prévue plus tard).
     *
     * Exemple :
     *   Orders = [O1, O2, O3, O4, O5]
     *   LotSize = 3
     *   Après regroupement :
     *      Lot 1 = [O1, O2, O3]
     *      Lot 2 = [O4, O5]
     */
    private List<List<Order>> groupOrdersByProximity(List<Order> orders, int lotSize) {
        if (orders == null || orders.isEmpty() || lotSize <= 0) {
            return new ArrayList<>(); // Si pas de commandes ou lotSize <= 0, retourne vide
        }

        // Tri des commandes par ID pour un ordre stable
        // Exemple : sorted = [O1, O2, O3, O4, O5]
        List<Order> sorted = orders.stream()
                .sorted(Comparator.comparing(Order::getId))
                .toList();

        // Filtrer uniquement les commandes avec adresse principale et lat/lng
        List<Order> remaining = new ArrayList<>(); // variable qui va garder ces commandes valides
        for (Order o : sorted) {
            Address addr = getPrimaryAddress(o.getEmployee());
            if (addr != null && addr.getLatitude() != null && addr.getLongitude() != null) {
                remaining.add(o);
            }
        }
        // Exemple : remaining = [O1, O2, O3, O4, O5] si toutes ont lat/lng

        List<List<Order>> lots = new ArrayList<>(); // variable qui va stocker les lots
        while (!remaining.isEmpty()) { // Tant qu'il reste des commandes à traiter

            // --- Création d'un nouveau lot ---
            List<Order> lot = new ArrayList<>(); // nouveau lot vide
            lot.add(remaining.remove(0)); // on prend la première commande comme "graine"
            // Exemple : lot = [O1], remaining = [O2, O3, O4, O5]

            // --- Remplissage du lot ---
            // Tant que le lot n'est pas plein et qu'il reste des commandes
            while (lot.size() < lotSize && !remaining.isEmpty()) {

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
                int nearestIndex = -1;              // index de la commande la plus proche dans 'remaining'

                for (int i = 0; i < remaining.size(); i++) {
                    Order c = remaining.get(i); // commande courante
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

                lot.add(nearest);                 // Ajouter la commande la plus proche au lot
                remaining.remove(nearestIndex);   // Retirer la commande de remaining
                // Exemple : lot = [O1, O2], remaining = [O3, O4, O5] → après ajout O3 :
                // lot = [O1, O2, O3], remaining = [O4, O5]
            }

            lots.add(lot); // Ajouter le lot complet à la liste des lots
            // Exemple après 1er lot : lots = [[O1, O2, O3]], remaining = [O4, O5]
        }

        // Exemple final pour lotSize = 3 :
        // lots = [
        //   [O1, O2, O3],
        //   [O4, O5]
        // ]
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
    private DeliveryTourListDTO mapToDeliveryTourListDTO(DeliveryTour deliveryTour) {
        DeliveryTourListDTO dto = new DeliveryTourListDTO();

        // 1. Informations tournée
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
        //on va parcourir la liste des items et on va construire une chaîne de caractères avec le nom du produit et sa quantité et on va joindre les chaînes de caractères avec une virgule
        return items.stream()
                .map(item -> item.getProduct().getName() + " (" + item.getQuantity() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
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






