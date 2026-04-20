package com.example.coopachat.controllers;

import com.example.coopachat.dtos.Payment.PaymentBridgeResponseDTO;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.entities.Order;
import com.example.coopachat.entities.Payment;
import com.example.coopachat.entities.Users;
import com.example.coopachat.enums.PaymentStatus;
import com.example.coopachat.repositories.EmployeeRepository;
import com.example.coopachat.repositories.OrderRepository;
import com.example.coopachat.repositories.PaymentRepository;
import com.example.coopachat.repositories.UserRepository;
import com.example.coopachat.services.fee.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

//Endpoint pour la page Web TouchPay (sendPaymentInfos)
@RestController
@RequestMapping("/api/payments/bridge")
@RequiredArgsConstructor
@Slf4j
public class PaymentBridgeController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FeeService feeService;

    @Value("${touchpay.hosted.script-url:https://touchpay.gutouch.net/touchpayv2/script/touchpaynr/prod_touchpay-0.0.1.js}")
    private String hostedScriptUrl;//URL du script TouchPay
    @Value("${touchpay.agency-code:}")
    private String agencyCode;//Code de l'agence TouchPay 
    @Value("${touchpay.token:}")
    private String token;//Token TouchPay (securité pour la communication entre le backend et TouchPay)
    @Value("${touchpay.service-id:}")
    private String serviceId;//Service ID TouchPay
    @Value("${touchpay.hosted.success-redirect-url:}")
    private String successRedirectUrl;//URL de redirection en cas de succès
    @Value("${touchpay.hosted.failed-redirect-url:}")
    private String failedRedirectUrl;//URL de redirection en cas d'échec
    @Value("${touchpay.hosted.default-city:Dakar}")
    private String defaultCity;//Ville par défaut

    @GetMapping("/{orderId}")
    public PaymentBridgeResponseDTO getBridgePayload(@PathVariable Long orderId) {
        Users currentUser = getCurrentUser();//Utilisateur connecté

        //Recuperation de l'employé connecté
        Employee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        //Recuperation de la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        // Sécurité : le salarié ne peut récupérer que SES commandes
        if (order.getEmployee() == null || !order.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Cette commande ne vous appartient pas");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            // Normalement créé à la commande, mais on sécurise le cas.
            payment = new Payment();
            payment.setOrder(order);
            payment.setStatus(PaymentStatus.UNPAID);
        }

        // On s'assure d'avoir une référence (order_number côté TouchPay).
        if (payment.getTransactionReference() == null || payment.getTransactionReference().isBlank()) {
            payment.setTransactionReference(String.valueOf(System.currentTimeMillis()));
        }

        // Statut local: au moment où on ouvre la page TouchPay, on passe en PENDING
        if (payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.PENDING);
        }

        paymentRepository.save(payment);//Enregistrement de la transaction

        BigDecimal subtotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;//Sous-total de la commande
        BigDecimal serviceFee = feeService.calculateTotalFees();//Frais de service
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;//Si les frais de service sont nuls, on les set à 0
        BigDecimal total = subtotal.add(serviceFee);//Total de la commande

        //Retourne la réponse de la page Web TouchPay
        return new PaymentBridgeResponseDTO(
                payment.getTransactionReference(),//Référence de la transaction
                agencyCode,//Code de l'agence TouchPay
                token,//Token TouchPay
                serviceId,//Service ID TouchPay
                hostedScriptUrl,//URL du script TouchPay
                total,//Total de la commande
                defaultCity,//Ville par défaut
                successRedirectUrl,//URL de redirection en cas de succès
                failedRedirectUrl,//URL de redirection en cas d'échec
                currentUser.getEmail(),//Email de l'utilisateur
                currentUser.getFirstName(),//Prénom de l'utilisateur
                currentUser.getLastName(),//Nom de l'utilisateur
                currentUser.getPhone()//Téléphone de l'utilisateur
        );
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}

