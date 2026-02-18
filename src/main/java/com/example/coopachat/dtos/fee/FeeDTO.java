package com.example.coopachat.dtos.fee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal amount;
    private Boolean isActive;
}
