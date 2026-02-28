package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryTourListDTO {

    private Long id;           // Pour les actions frontend (détails, modifier, annuler)
    private String tourNumber; // N° Tour
    private LocalDate deliveryDate;
    private String driverName;
    private String vehicle;//(type/plaque)
    private Integer orderCount;
    private DeliveryTourStatus status;

}
