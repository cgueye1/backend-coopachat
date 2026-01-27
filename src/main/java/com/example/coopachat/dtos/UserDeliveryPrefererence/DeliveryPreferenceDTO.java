package com.example.coopachat.dtos.UserDeliveryPrefererence;


import com.example.coopachat.enums.DeliveryMode;
import com.example.coopachat.enums.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPreferenceDTO {
    private Long id; // ID de la préférence
    private Set<String> preferredDays; // ["MONDAY", "WEDNESDAY", "FRIDAY"]
    private TimeSlot preferredTimeSlot; // MORNING, AFTERNOON, ALL_DAY
    private DeliveryMode deliveryMode; // OFFICE, HOME
}