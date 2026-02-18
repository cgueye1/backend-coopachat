package com.example.coopachat.dtos.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentTimingDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String name;
    private String description;
}
