package com.example.coopachat.dtos.delivery;

import com.example.coopachat.enums.DeliveryTourStatus;
import com.example.coopachat.enums.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryTourListDTO {

    private String tourNumber;
    private LocalDate deliveryDate;
    private TimeSlot timeSlot;
    private String driverName;
    private String vehicle;//(type/plaque)
    private String deliveryZone;
    private Integer orderCount;
    private DeliveryTourStatus status;

}
