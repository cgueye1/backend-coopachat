package com.example.coopachat.dtos.DeliveryDriver;

import com.example.coopachat.enums.DriverReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour soumettre un signalement (formulaire "Signaler un problème" du livreur).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDriverReportDTO {

    @NotNull(message = "La nature du signalement est obligatoire")
    private DriverReportType reportType;

    private String comment;
}
