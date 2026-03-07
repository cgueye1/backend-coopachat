package com.example.coopachat.dtos.employee;

import com.example.coopachat.enums.EmployeeDeliveryIssueReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour signaler un problème de livraison par le salarié (client).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDeliveryIssueDTO {

    @NotNull(message = "La raison est obligatoire")
    private EmployeeDeliveryIssueReason reason;

    private String comment;
}
