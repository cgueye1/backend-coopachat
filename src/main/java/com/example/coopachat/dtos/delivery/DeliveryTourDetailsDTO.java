package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeliveryTourDetailsDTO {

    private Long id;
    private String tourNumber;
    private LocalDate deliveryDate;
    private DeliveryTourStatus status;

    // === CHAUFFEUR ===
    private String driverName;           // Jean Dupont
    private String driverPhone;          // +221 77 123 45 67

    // === VÉHICULE ===
    private String vehicleType;          // Camionnette
    private String vehiclePlate;         // ABC-123 (optionnel)

    // === COMMANDES ===
    private Integer orderCount;          // 5
    /** Liste des commandes de la tournée (ordre, numéro, salarié, adresse). */
    private List<OrderInTourDTO> orders = new ArrayList<>();

    /** Note / commentaire sur la tournée (optionnel). */
    private String notes;

}