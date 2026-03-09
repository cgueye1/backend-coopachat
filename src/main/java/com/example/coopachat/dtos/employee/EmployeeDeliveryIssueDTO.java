package com.example.coopachat.dtos.employee;

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

    /**
     * ID de la raison (référentiel admin : employee-delivery-issue-reasons).
     */
    @NotNull(message = "La raison est obligatoire")
    private Long reasonId;

    private String comment;
}
