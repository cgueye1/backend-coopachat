package com.example.coopachat.controllers;


import com.example.coopachat.dtos.Payment.InTouchCallbackRequest;
import com.example.coopachat.entities.Payment;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/intouch")
@RequiredArgsConstructor
@Slf4j
public class inTouchCallbackController {

    private final PaymentRepository paymentRepository;

    /**
     * Endpoint appelé par InTouch à la fin de la transaction.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody InTouchCallbackRequest request) {

        // Log pour suivre ce que InTouch envoie
        log.info("📥 InTouch callback reçu: partnerTx={}, guTx={}, status={}",
                request.getPartnerTransactionId(), // Notre référence générée
                request.getGuTransactionId(),      // référence InTouch
                request.getStatus());              // statut du paiement

        log.info("📥 InTouch callback reçu: partnerTx={}, guTx={}, status={}",
                request.getPartnerTransactionId(), request.getGuTransactionId(), request.getStatus());

        //  Validation: on ne peut rien faire sans Notre référence
        if (request.getPartnerTransactionId() == null || request.getPartnerTransactionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "partner_transaction_id est obligatoire"
            ));
        }

        // On cherche le paiement correspondant dans la base
        Payment payment = paymentRepository.findByTransactionReference(request.getPartnerTransactionId())
                .orElseThrow(() -> new RuntimeException(
                        "Paiement introuvable pour la référence: " + request.getPartnerTransactionId()));

        // Si déjà payé, on ne refait rien (évite les doublons si InTouch appelle 2 fois
        if (payment.getStatus() == PaymentStatus.PAID) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Paiement déjà confirmé"
            ));
        }

        //  On enregistre les infos venant d’InTouch
        payment.setGuTransactionId(request.getGuTransactionId());// // ID côté InTouch
        payment.setProviderMessage(request.getMessage());// // message (ex: succès ou erreur)

        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";// Normalisation du status (on évite les erreurs de casse)
        //Si paiement réussi
        if ("SUCCESSFUL".equals(status)) {
            payment.setStatus(PaymentStatus.PAID); // paiement confirmé
            payment.setPaidAt(LocalDateTime.now()); // date de paiement
        }
        // Sinon échec
        else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        //Sauvegarde en base
        paymentRepository.save(payment);

        // Réponse envoyée à InTouch
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Callback traité avec succès"
        ));
    }
}

