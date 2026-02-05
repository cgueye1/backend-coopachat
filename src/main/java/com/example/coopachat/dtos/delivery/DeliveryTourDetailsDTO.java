package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.TimeSlot;
import lombok.Data;
import java.time.LocalDate;

// DTO détails tournée de livraison
@Data
public class DeliveryTourDetailsDTO {

    // === INFORMATIONS TOURNÉE ===
    private String tourNumber;           // TOUR-2024-001
    private LocalDate deliveryDate;      // 15/01/2024
    private TimeSlot timeSlot;           // MORNING (Matin)
    private DeliveryTourStatus status;   // PLANIFIEE

    // === CHAUFFEUR ===
    private String driverName;           // Jean Dupont
    private String driverPhone;          // +221 77 123 45 67

    // === VÉHICULE ===
    private String vehicleType;          // Camionnette
    private String vehiclePlate;         // ABC-123 (optionnel)

    // === COMMANDES ===
    private Integer orderCount;          // 5

}