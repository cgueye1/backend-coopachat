package com.example.coopachat.dtos.DeliveryDriver;

import com.example.coopachat.enums.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDriverPreferenceDTO {
    private Long id;
    private Set<String> preferredDays;
    private TimeSlot preferredTimeSlot;
}