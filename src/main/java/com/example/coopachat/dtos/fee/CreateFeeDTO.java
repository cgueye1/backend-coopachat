package com.example.coopachat.dtos.fee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeeDTO {

    @NotBlank(message = "Le nom du frais est obligatoire")
    private String name;

    private String description;

    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal amount;
}
