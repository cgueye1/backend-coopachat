package com.example.coopachat.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private Integer totalArticles;          // "1 articles"
    private String deliveryOption;          // "Hebdomadaire"
    private LocalDate firstDeliveryDate;    // "06/09/2025"
    private String deliveryAddress;         // "Dakar, Point E"
    private BigDecimal total;               // "2 591 F CFA"
}
