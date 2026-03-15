package com.example.coopachat.dtos.promotions;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la création d'une promotion (réductions en % sur des produits).
 * Au moins un produit avec une réduction doit être fourni.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionDTO {

    @NotBlank(message = "Le nom de la promotion est obligatoire")
    private String name;

    @NotNull(message = "La date de début est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endDate;

    @NotEmpty(message = "Au moins un produit avec réduction est obligatoire")
    @Valid
    private List<ProductReductionItemDTO> productItems;
}
