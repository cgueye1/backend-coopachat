
package com.example.coopachat.services.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails - format texte simple et lisible .
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    // ============================================================================
    // 📦 DEPENDENCIES
    // ============================================================================

    private final JavaMailSender mailSender;

    // ============================================================================
    // ⚙️ CONFIGURATION
    // ============================================================================

    @Value("${mail.from:admincoopachat@yopmail.com}")
    private String mailFrom;

    @Value("${mail.app.name:CoopAchat}")
    private String appName;

    @Value("${app.frontend.reset-password-url:http://localhost:4200/reset-password?token=}")
    private String resetPasswordUrl;

    // ============================================================================
    // 📧 ENVOI D'EMAILS - ACTIVATION DE COMPTE
    // ============================================================================

    /**
     * Envoie un code d'activation par email à un utilisateur
     */
    @Override
    public void sendActivationCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Code d'activation - " + appName);

            String body = String.format(
                "Bonjour %s,%n%nVotre code d'activation %s est : %s%n%nExpire dans 15 minutes.%n%nNe partagez jamais ce code. L'équipe %s ne vous demandera jamais votre code par email, téléphone ou message.%n%nL'équipe %s",
                firstName != null ? firstName : "", appName, code, appName, appName
            );
            helper.setText(body, false);
            mailSender.send(message);

            log.info("Code d'activation envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation à {}: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'activation", e);
        }
    }

    // ============================================================================
    // 📧 ENVOI D'EMAILS - AUTHENTIFICATION 2FA (ADMIN)
    // ============================================================================

    /**
     * Envoie un code OTP par email à un administrateur pour l'authentification 2FA
     */
    @Override
    public void sendOtpCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Code OTP de vérification - " + appName);

            String body = String.format(
                "Bonjour %s,%n%nVotre code OTP %s : %s%n%nExpire dans 15 minutes.%n%nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email.%n%nL'équipe Sécurité %s",
                firstName != null ? firstName : "", appName, code, appName
            );
            helper.setText(body, false);
            mailSender.send(message);

            log.info("Code OTP envoyé avec succès à l'administrateur: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code OTP à {}: {}", 
                    email, e.getMessage(), e);
            // Message utilisateur volontairement simple (les détails techniques restent dans les logs).
            throw new RuntimeException("Impossible d'envoyer le code de vérification pour le moment. Veuillez réessayer dans quelques instants.");
        }
    }

    // ============================================================================
    // 📧 ENVOI D'EMAILS - RÉINITIALISATION DE MOT DE PASSE
    // ============================================================================

    /**
     * Envoie un lien de réinitialisation de mot de passe par email
     */
    @Override
    public void sendPasswordResetLink(String email, String token, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Réinitialisation de mot de passe - " + appName);

            String resetUrl = resetPasswordUrl + token;
            String body = String.format(
                "Bonjour %s,%n%nPour réinitialiser votre mot de passe, cliquez ou copiez ce lien :%n%n%s%n%nExpire dans 15 minutes.%n%nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email. Votre mot de passe actuel reste inchangé.%n%nL'équipe Support %s",
                firstName != null ? firstName : "", resetUrl, appName
            );
            helper.setText(body, false);
            mailSender.send(message);

            log.info("Lien de réinitialisation de mot de passe envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien de réinitialisation à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }

    // ============================================================================
    // 📧 ENVOI D'EMAILS - INVITATION SALARIÉ
    // ============================================================================

    /**
     * Envoie un code d'activation par email à un salarié créé par un commercial
     */
    @Override
    public void sendEmployeeInvitation(String email, String code, String firstName, String commercialName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Invitation à rejoindre " + companyName + " - " + appName);

            String body = String.format(
                "Bonjour %s,%n%nVotre entreprise %s utilise désormais %s pour ses achats. Le commercial %s a paramétré votre compte.%n%nCode d'activation : %s%n%nCode valide pendant 15 minutes.%n%nSi vous ne reconnaissez pas cette invitation, ignorez cet email.%n%nL'équipe Support %s",
                firstName != null ? firstName : "", companyName, appName, commercialName, code, appName
            );
            helper.setText(body, false);
            mailSender.send(message);

            log.info("Code d'activation envoyé avec succès au salarié: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'invitation", e);
        }
    }

    // ============================================================================
    // 📧 ENVOI D'EMAILS - ACTIVATION LIVREUR
    // ============================================================================

    @Override
    public void sendDriverActivationCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Code d'activation Livreur - " + appName);

            String body = String.format(
                "Bonjour %s,%n%nBienvenue dans l'équipe de livraison %s. Votre code d'activation : %s%n%nExpire dans 15 minutes.%n%nNe partagez jamais ce code. Si vous n'avez pas demandé ce code, ignorez cet email.%n%nL'équipe %s",
                firstName != null ? firstName : "", appName, code, appName
            );
            helper.setText(body, false);
            mailSender.send(message);

            log.info("Code d'activation livreur envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation livreur à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'activation", e);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body != null ? body : "", false);
            mailSender.send(message);
            log.info("Email envoyé à: {} (sujet: {})", to, subject);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}: {}", to, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email", e);
        }
    }
}

