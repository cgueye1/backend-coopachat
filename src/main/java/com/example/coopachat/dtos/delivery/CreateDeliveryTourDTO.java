package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.TimeSlot;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// DTO pour créer une tournée
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeliveryTourDTO {

    @NotNull(message = "La date de livraison est obligatoire")
    @FutureOrPresent(message = "La date doit être aujourd'hui ou dans le futur")
    private LocalDate deliveryDate;

    @NotNull(message = "Le créneau horaire est obligatoire")
    private TimeSlot timeSlot;

    @NotNull(message = "La zone de livraison est obligatoire")
    private Long deliveryZoneId;

    @NotNull(message = "Le chauffeur est obligatoire")
    private Long driverId;

    @NotBlank(message = "La plaque d'immatriculation est obligatoire")
    @Pattern(regexp = "^[A-Z]{2}-\\d{4,5}-[A-Z]{2}$",//2 lettres majuscules - 4 ou 5 chiffres -2 lettres majuscules
            message = "Format de plaque invalide. Exemple: AA-12345-AB")
    private String vehiclePlate;

    @NotNull(message = "Au moins une commande est requise")
    @Size(min = 1, message = "Au moins une commande est requise")
    private List<Long> orderIds;

    private String notes;
}