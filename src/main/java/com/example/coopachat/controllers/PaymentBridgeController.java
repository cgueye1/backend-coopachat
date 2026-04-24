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
        // Recuperation de la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        Users currentUser = null;
        try {
            currentUser = getCurrentUser();
        } catch (Exception e) {
            log.warn("Mode TEST : Accès au bridge de paiement sans authentification pour la commande ID {}", orderId);
        }

        // Si authentifié, on garde la vérification de sécurité
        if (currentUser != null) {
            Employee employee = employeeRepository.findByUser(currentUser)
                    .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
            if (order.getEmployee() == null || !order.getEmployee().getId().equals(employee.getId())) {
                throw new RuntimeException("Cette commande ne vous appartient pas");
            }
        } else {
            // Mode Public (Test) : On récupère l'utilisateur lié à l'employé de la commande
            if (order.getEmployee() != null && order.getEmployee().getUser() != null) {
                currentUser = order.getEmployee().getUser();
            } else {
                throw new RuntimeException("Utilisateur introuvable pour cette commande (mode public)");
            }
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setStatus(PaymentStatus.UNPAID);
        }

        if (payment.getTransactionReference() == null || payment.getTransactionReference().isBlank()) {
            payment.setTransactionReference(String.valueOf(System.currentTimeMillis()));
        }

        if (payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.PENDING);
        }

        paymentRepository.save(payment);

        BigDecimal subtotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal serviceFee = feeService.calculateTotalFees();
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(serviceFee);

        return new PaymentBridgeResponseDTO(
                payment.getTransactionReference(),
                agencyCode,
                token,
                serviceId,
                hostedScriptUrl,
                total,
                defaultCity,
                successRedirectUrl,
                failedRedirectUrl,
                currentUser.getEmail(),
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getPhone()
        );
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().equals("anonymousUser")) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}

