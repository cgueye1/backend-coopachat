package com.example.coopachat.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur temporaire pour gérer les redirections de paiement TouchPay.
 * Affiche des messages simples de succès ou d'échec en attendant les vraies pages frontend.
 */
@RestController
public class PaymentRedirectController {

    @GetMapping("/payment/success")
    public String paymentSuccess() {
        return "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px;'>" +
               "<h1 style='color: #28a745;'>Paiement réussi !</h1>" +
               "<p>Merci pour votre achat. Votre commande est en cours de traitement.</p>" +
               "<button onclick='window.close()' style='padding: 10px 20px; background: #2a538b; color: white; border: none; border-radius: 5px; cursor: pointer;'>Retour à l'application</button>" +
               "</body></html>";
    }

    @GetMapping("/payment/failed")
    public String paymentFailed() {
        return "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px;'>" +
               "<h1 style='color: #dc3545;'>Paiement échoué</h1>" +
               "<p>Désolé, une erreur est survenue lors du paiement. Veuillez réessayer.</p>" +
               "<button onclick='window.close()' style='padding: 10px 20px; background: #2a538b; color: white; border: none; border-radius: 5px; cursor: pointer;'>Réessayer</button>" +
               "</body></html>";
    }
}
