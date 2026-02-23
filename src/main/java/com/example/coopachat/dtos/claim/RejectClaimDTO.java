package com.example.coopachat.dtos.claim;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour rejeter une réclamation : motif obligatoire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectClaimDTO {

    @NotBlank(message = "Le motif du rejet est obligatoire")
    private String rejectionReason;
}
