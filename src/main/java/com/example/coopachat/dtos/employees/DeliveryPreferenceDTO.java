package com.example.coopachat.dtos.employees;


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
    private Long id;
    /** Un jour par élément Ex: ["MONDAY", "TUESDAY", "WEDNESDAY"] */
    private Set<String> preferredDays;
    private TimeSlot preferredTimeSlot; // MORNING, AFTERNOON, ALL_DAY
    private DeliveryMode deliveryMode;   // OFFICE, HOME
}