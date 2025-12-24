package com.example.coopachat.services.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implémentation du service d'envoi d'emails
 * Envoie des emails HTML professionnels pour l'activation de compte
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

    @Value("${app.frontend.activate-employee-url:http://localhost:4200/activate?token=}")
    private String activateEmployeeUrl;

    // ============================================================================
    // 📧 ENVOI D'EMAILS
    // ============================================================================

    /**
     * Envoie un code d'activation par email à un utilisateur
     */
    @Override
    public void sendActivationCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔐 Code d'activation - " + appName);

            // Génération du template HTML
            String emailBody = generateActivationEmailTemplate(firstName, code);

            helper.setText(emailBody, true); // true = HTML
            mailSender.send(message);

            log.info("Code d'activation envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation à {}: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'activation", e);
        }
    }

    // ============================================================================
    // 🎨 TEMPLATES HTML
    // ============================================================================

    /**
     * Génère le template HTML pour l'email d'activation
     *
     * @param firstName Le prénom de l'utilisateur
     * @param code      Le code d'activation à 6 chiffres
     * @return Le template HTML formaté
     */
    private String generateActivationEmailTemplate(String firstName, String code) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Code d'activation - %s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 20px 0;">
                            <table role="presentation" style="width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                
                                <!-- Header avec logo -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">
                                            🛒 %s
                                        </h1>
                                        <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">
                                            Plateforme E-commerce Coopérative
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Contenu principal -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px; font-weight: 600;">
                                            Bonjour %s,
                                        </h2>
                                        
                                        <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                            Bienvenue sur <strong>%s</strong> ! Pour activer votre compte et finaliser votre inscription, 
                                            veuillez utiliser le code d'activation ci-dessous.
                                        </p>
                                        
                                        <!-- Code d'activation en grand -->
                                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px; text-align: center; margin: 30px 0;">
                                            <p style="color: #ffffff; margin: 0 0 10px 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; opacity: 0.9;">
                                                Votre code d'activation
                                            </p>
                                            <div style="background-color: #ffffff; padding: 20px; border-radius: 8px; display: inline-block; margin: 10px 0;">
                                                <span style="color: #667eea; font-size: 36px; font-weight: 700; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                                    %s
                                                </span>
                                            </div>
                                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 12px; opacity: 0.8;">
                                                ⏱️ Ce code expire dans 15 minutes
                                            </p>
                                        </div>
                                        
                                        <!-- Instructions -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; margin: 30px 0;">
                                            <h3 style="color: #333333; margin: 0 0 15px 0; font-size: 18px; font-weight: 600;">
                                                📋 Instructions :
                                            </h3>
                                            <ol style="color: #666666; font-size: 14px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                                <li>Copiez le code d'activation ci-dessus</li>
                                                <li>Retournez sur la page d'activation de votre compte</li>
                                                <li>Entrez le code dans le champ prévu à cet effet</li>
                                                <li>Créez votre mot de passe pour finaliser votre inscription</li>
                                            </ol>
                                        </div>
                                        
                                        <!-- Avertissement de sécurité -->
                                        <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 30px 0;">
                                            <p style="color: #856404; margin: 0; font-size: 13px; line-height: 1.6;">
                                                <strong>🔒 Sécurité :</strong> Ne partagez jamais ce code avec qui que ce soit. 
                                                L'équipe %s ne vous demandera jamais votre code d'activation par email ou téléphone.
                                            </p>
                                        </div>
                                        
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0;">
                                            Si vous n'avez pas demandé ce code, vous pouvez ignorer cet email en toute sécurité.
                                        </p>
                                        
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                            Cordialement,<br>
                                            <strong style="color: #333333;">L'équipe %s</strong>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-radius: 0 0 10px 10px; border-top: 1px solid #e0e0e0;">
                                        <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                        <p style="color: #999999; font-size: 12px; margin: 0;">
                                            © %d %s - Tous droits réservés
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, appName, firstName, appName, code, appName, appName, java.time.Year.now().getValue(), appName);
    }

    /**
     * Envoie un code OTP par email à un administrateur pour l'authentification 2FA
     */
    @Override
    public void sendOtpCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔐 Code OTP de vérification - " + appName);

            // Génération du template HTML
            String emailBody = generateOtpEmailTemplate(firstName, code);

            helper.setText(emailBody, true); // true = HTML
            mailSender.send(message);

            log.info("Code OTP envoyé avec succès à l'administrateur: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code OTP à {}: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email OTP", e);
        }
    }

    /**
     * Génère le template HTML pour l'email OTP (2FA administrateur)
     *
     * @param firstName Le prénom de l'administrateur
     * @param code      Le code OTP à 6 chiffres
     * @return Le template HTML formaté
     */
    private String generateOtpEmailTemplate(String firstName, String code) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Code OTP de vérification - %s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 20px 0;">
                            <table role="presentation" style="width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                
                                <!-- Header avec logo -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">
                                            🛒 %s
                                        </h1>
                                        <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">
                                            Plateforme E-commerce Coopérative
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Contenu principal -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px; font-weight: 600;">
                                            Bonjour %s,
                                        </h2>
                                        
                                        <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                            Vous avez demandé à vous connecter à votre compte <strong>administrateur</strong> sur <strong>%s</strong>. 
                                            Pour finaliser votre connexion, veuillez utiliser le code OTP de vérification ci-dessous.
                                        </p>
                                        
                                        <!-- Code OTP en grand -->
                                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px; text-align: center; margin: 30px 0;">
                                            <p style="color: #ffffff; margin: 0 0 10px 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; opacity: 0.9;">
                                                Votre code OTP de vérification
                                            </p>
                                            <div style="background-color: #ffffff; padding: 20px; border-radius: 8px; display: inline-block; margin: 10px 0;">
                                                <span style="color: #667eea; font-size: 36px; font-weight: 700; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                                    %s
                                                </span>
                                            </div>
                                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 12px; opacity: 0.8;">
                                                ⏱️ Ce code expire dans 15 minutes
                                            </p>
                                        </div>
                                        
                                        <!-- Instructions -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; margin: 30px 0;">
                                            <h3 style="color: #333333; margin: 0 0 15px 0; font-size: 18px; font-weight: 600;">
                                                📋 Instructions :
                                            </h3>
                                            <ol style="color: #666666; font-size: 14px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                                <li>Copiez le code OTP ci-dessus</li>
                                                <li>Retournez sur la page de connexion</li>
                                                <li>Entrez le code dans le champ prévu à cet effet</li>
                                                <li>Votre connexion sera finalisée automatiquement</li>
                                            </ol>
                                        </div>
                                        
                                        <!-- Avertissement de sécurité -->
                                        <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 30px 0;">
                                            <p style="color: #856404; margin: 0; font-size: 13px; line-height: 1.6;">
                                                <strong>🔒 Sécurité :</strong> Ne partagez jamais ce code avec qui que ce soit. 
                                                L'équipe %s ne vous demandera jamais votre code OTP par email ou téléphone.
                                            </p>
                                        </div>
                                        
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0;">
                                            Si vous n'avez pas demandé à vous connecter, ignorez cet email et contactez immédiatement le support.
                                        </p>
                                        
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                            Cordialement,<br>
                                            <strong style="color: #333333;">L'équipe %s</strong>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-radius: 0 0 10px 10px; border-top: 1px solid #e0e0e0;">
                                        <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                        <p style="color: #999999; font-size: 12px; margin: 0;">
                                            © %d %s - Tous droits réservés
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, appName, firstName, appName, code, appName, appName, java.time.Year.now().getValue(), appName);
    }

    /**
     * Envoie un lien de réinitialisation de mot de passe par email
     */
    @Override
    public void sendPasswordResetLink(String email, String token, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔑 Réinitialisation de votre mot de passe - " + appName);

            // Génération du template HTML
            String emailBody = generatePasswordResetEmailTemplate(firstName, token);

            helper.setText(emailBody, true); // true = HTML
            mailSender.send(message);

            log.info("Lien de réinitialisation de mot de passe envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien de réinitialisation à {}: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }

    /**
     * Génère le template HTML pour l'email de réinitialisation de mot de passe
     *
     * @param firstName Le prénom de l'utilisateur
     * @param token     Le token unique de réinitialisation
     * @return Le template HTML formaté
     */
    private String generatePasswordResetEmailTemplate(String firstName, String token) {

        // URL complète avec le token
        String resetUrl = resetPasswordUrl + token;
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Réinitialisation de mot de passe - %s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 20px 0;">
                            <table role="presentation" style="width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                
                                <!-- Header avec logo -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">
                                            🛒 %s
                                        </h1>
                                        <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">
                                            Plateforme E-commerce Coopérative
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Contenu principal -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px; font-weight: 600;">
                                            Bonjour %s,
                                        </h2>
                                        
                                        <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                            Vous avez demandé à réinitialiser votre mot de passe sur <strong>%s</strong>. 
                                            Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe.
                                        </p>
                                        
                                        <!-- Bouton de réinitialisation -->
                                        <div style="text-align: center; margin: 40px 0;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.3);">
                                                🔑 Réinitialiser mon mot de passe
                                            </a>
                                        </div>
                                        
                                        <!-- Lien alternatif -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 30px 0;">
                                            <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 0 0 10px 0;">
                                                <strong>Le bouton ne fonctionne pas ?</strong>
                                            </p>
                                            <p style="color: #666666; font-size: 13px; line-height: 1.6; margin: 0; word-break: break-all;">
                                                Copiez et collez ce lien dans votre navigateur :<br>
                                                <a href="%s" style="color: #667eea; text-decoration: underline;">%s</a>
                                            </p>
                                        </div>
                                        
                                        <!-- Instructions -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; margin: 30px 0;">
                                            <h3 style="color: #333333; margin: 0 0 15px 0; font-size: 18px; font-weight: 600;">
                                                📋 Instructions :
                                            </h3>
                                            <ol style="color: #666666; font-size: 14px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                                <li>Cliquez sur le bouton "Réinitialiser mon mot de passe" ci-dessus</li>
                                                <li>Vous serez redirigé vers une page sécurisée</li>
                                                <li>Créez un nouveau mot de passe sécurisé</li>
                                                <li>Confirmez votre nouveau mot de passe</li>
                                            </ol>
                                        </div>
                                        
                                        <!-- Avertissement de sécurité -->
                                        <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 30px 0;">
                                            <p style="color: #856404; margin: 0 0 10px 0; font-size: 13px; line-height: 1.6;">
                                                <strong>🔒 Sécurité :</strong> Ce lien est valide pendant <strong>15 minutes</strong> uniquement.
                                            </p>
                                            <p style="color: #856404; margin: 0; font-size: 13px; line-height: 1.6;">
                                                Si vous n'avez pas demandé à réinitialiser votre mot de passe, ignorez cet email. 
                                                Votre mot de passe actuel restera inchangé.
                                            </p>
                                        </div>
                                        
                                                                              
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                            Cordialement,<br>
                                            <strong style="color: #333333;">L'équipe %s</strong>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-radius: 0 0 10px 10px; border-top: 1px solid #e0e0e0;">
                                        <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                        <p style="color: #999999; font-size: 12px; margin: 0;">
                                            © %d %s - Tous droits réservés
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, appName, firstName, appName, resetUrl, resetUrl, resetUrl, appName, java.time.Year.now().getValue(), appName);
    }

    /**
     * Envoie un lien d'invitation par email à un salarié créé par un commercial
     */
    @Override
    public void sendEmployeeInvitation(String email, String token, String firstName, String commercialName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🎉 Invitation à rejoindre " + companyName + " - " + appName);

            // Génération du template HTML
            String emailBody = generateEmployeeInvitationEmailTemplate(firstName, token, commercialName, companyName);

            helper.setText(emailBody, true); // true = HTML
            mailSender.send(message);

            log.info("Lien d'invitation envoyé avec succès au salarié: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien d'invitation à {}: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'invitation", e);
        }
    }

    /**
     * Génère le template HTML pour l'email d'invitation salarié
     *
     * @param firstName Le prénom du salarié
     * @param token Le token unique d'invitation
     * @param commercialName Le nom du commercial
     * @param companyName Le nom de l'entreprise
     * @return Le template HTML formaté
     */
    private String generateEmployeeInvitationEmailTemplate(String firstName, String token, String commercialName, String companyName) {

        // URL complète avec le token
        String activationUrl = activateEmployeeUrl + token;
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Invitation - %s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 20px 0;">
                            <table role="presentation" style="width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                
                                <!-- Header avec logo -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">
                                            🛒 %s
                                        </h1>
                                        <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">
                                            Plateforme E-commerce Coopérative
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Contenu principal -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px; font-weight: 600;">
                                            Bonjour %s,
                                        </h2>
                                        
                                        <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                            Vous avez été invité par <strong>%s</strong> à rejoindre <strong>%s</strong> sur la plateforme <strong>%s</strong>.
                                        </p>
                                        
                                        <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                            Pour activer votre compte et créer votre mot de passe, cliquez sur le bouton ci-dessous.
                                        </p>
                                        
                                        <!-- Bouton d'activation -->
                                        <div style="text-align: center; margin: 40px 0;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.3);">
                                                🎉 Activer mon compte
                                            </a>
                                        </div>
                                        
                                        <!-- Lien alternatif -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 30px 0;">
                                            <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 0 0 10px 0;">
                                                <strong>Le bouton ne fonctionne pas ?</strong>
                                            </p>
                                            <p style="color: #666666; font-size: 13px; line-height: 1.6; margin: 0; word-break: break-all;">
                                                Copiez et collez ce lien dans votre navigateur :<br>
                                                <a href="%s" style="color: #667eea; text-decoration: underline;">%s</a>
                                            </p>
                                        </div>
                                        
                                        <!-- Instructions -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; margin: 30px 0;">
                                            <h3 style="color: #333333; margin: 0 0 15px 0; font-size: 18px; font-weight: 600;">
                                                📋 Instructions :
                                            </h3>
                                            <ol style="color: #666666; font-size: 14px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                                <li>Cliquez sur le bouton "Activer mon compte" ci-dessus</li>
                                                <li>Vous serez redirigé vers une page sécurisée</li>
                                                <li>Créez votre mot de passe sécurisé</li>
                                                <li>Confirmez votre mot de passe</li>
                                                <li>Votre compte sera activé automatiquement</li>
                                            </ol>
                                        </div>
                                        
                                        <!-- Avertissement de sécurité -->
                                        <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 30px 0;">
                                            <p style="color: #856404; margin: 0 0 10px 0; font-size: 13px; line-height: 1.6;">
                                                <strong>🔒 Sécurité :</strong> Ce lien est valide pendant <strong>15 minutes</strong> uniquement.
                                            </p>
                                            <p style="color: #856404; margin: 0; font-size: 13px; line-height: 1.6;">
                                                Si vous n'avez pas reçu cette invitation, ignorez cet email.
                                            </p>
                                        </div>
                                        
                                        <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                            Cordialement,<br>
                                            <strong style="color: #333333;">L'équipe %s</strong>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-radius: 0 0 10px 10px; border-top: 1px solid #e0e0e0;">
                                        <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                        <p style="color: #999999; font-size: 12px; margin: 0;">
                                            © %d %s - Tous droits réservés
                                        </p>
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, appName, appName, firstName, commercialName, companyName, appName, activationUrl, activationUrl, activationUrl, appName, java.time.Year.now().getValue(), appName);
    }
}

