
package com.example.coopachat.services.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔐 Code d'activation - " + appName);

            // Attacher le logo si disponible
            addLogoInline(helper);

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
            <title>Activez votre compte %s</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
            </style>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background-color: #f8fafc; line-height: 1.6;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <!-- Carte principale -->
                <div style="background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);">
                    
                    <!-- En-tête avec logo -->
                    <div style="background: #ffffff; padding: 30px 30px 15px 30px; text-align: center;">
                        <div style="margin-bottom: 10px;">
                            <img src="cid:logo" alt="%s Logo" style="max-width: 180px; height: auto; display: block; margin: 0 auto;">
                        </div>
                    </div>
                    
                    <!-- Contenu -->
                    <div style="padding: 0 30px 25px 30px;">
                        
                        <!-- Message d'accueil -->
                        <div style="margin-bottom: 20px;">
                            <h2 style="color: #1F2937; margin: 0 0 10px 0; font-size: 22px; font-weight: 600;">
                                Bonjour %s 👋
                            </h2>
                            <p style="color: #6B7280; margin: 0; font-size: 15px;">
                                Votre inscription sur <strong style="color: #F97316;">%s</strong> est presque terminée ! 
                                Utilisez le code ci-dessous pour activer votre compte.
                            </p>
                        </div>
                        
                        <!-- Code d'activation -->
                        <div style="background: linear-gradient(135deg, #3B82F6 0%%, #8B5CF6 100%%); padding: 25px; border-radius: 12px; text-align: center; margin: 25px 0;">
                            <p style="color: rgba(255, 255, 255, 0.9); margin: 0 0 12px 0; font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 1px;">
                                Code d'Activation
                            </p>
                            <div style="background: white; padding: 20px; border-radius: 10px; display: inline-block; margin: 8px 0;">
                                <div style="background: #F3F4F6; padding: 20px 30px; border-radius: 8px; display: inline-block;">
                                    <span style="font-size: 36px; font-weight: 700; color: #1F2937; font-family: 'Courier New', monospace; letter-spacing: 4px;">
                                        %s
                                    </span>
                                </div>
                            </div>
                          <!-- Durée d'expiration-->
                             <div style="margin: 16px 0 0 0; text-align: center;">
                                  <span style="color: #1F2937; font-size: 13px; font-weight: 500;">
                                       <span 
                                          style="margin-right: 6px; opacity: 0.9;">⏱️
                                       </span>
                                       Expire dans 15 minutes
                                  </span>
                             </div>
                        </div>
                        
                                               
                        <!-- Note de sécurité -->
                        <div style="background-color: #FEF3C7; padding: 16px; border-radius: 10px; margin: 20px 0; border: 1px solid #FCD34D;">
                            <div style="display: flex; gap: 10px; align-items: flex-start;">
                                <svg style="width: 18px; height: 18px; color: #D97706; flex-shrink: 0; margin-top: 2px;" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/>
                                </svg>
                                <div>
                                    <p style="color: #92400E; margin: 0 0 5px 0; font-size: 14px; font-weight: 600;">
                                        Sécurité importante
                                    </p>
                                    <p style="color: #92400E; margin: 0; font-size: 13px; line-height: 1.5;">
                                        Ne partagez jamais ce code. Aucun membre de l'équipe %s ne vous demandera votre code d'activation par email, téléphone ou message.
                                    </p>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Signature -->
                        <div style="padding-top: 18px; border-top: 1px solid #E5E7EB;">
                            <p style="color: #6B7280; margin: 0 0 5px 0; font-size: 14px;">
                                Bien à vous,
                            </p>
                            <p style="color: #F97316; margin: 0; font-size: 15px; font-weight: 600;">
                                L'équipe %s
                            </p>
                        </div>
                        
                    </div>
                    
                    <!-- Pied de page -->
                    <div style="background-color: #F9FAFB; padding: 18px 30px; text-align: center; border-top: 1px solid #E5E7EB;">
                        <p style="color: #9CA3AF; margin: 0 0 8px 0; font-size: 12px;">
                            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
                        </p>
                        <p style="color: #9CA3AF; margin: 0; font-size: 12px;">
                            © %d %s • Tous droits réservés
                        </p>
                    </div>
                    
                </div>
            </div>
        </body>
        </html>
        """,
                appName,
                appName,
                firstName,
                appName,
                code,
                appName,
                appName,
                java.time.Year.now().getValue(),
                appName
        );
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

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔐 Code OTP de vérification - " + appName);

            // Attacher le logo si disponible
            addLogoInline(helper);


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
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
            </style>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background-color: #f8fafc; line-height: 1.6;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <!-- Carte principale -->
                <div style="background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);">
                    
                    <!-- En-tête avec logo -->
                    <div style="background: #ffffff; padding: 30px 30px 15px 30px; text-align: center;">
                        <div style="margin-bottom: 10px;">
                            <img src="cid:logo" alt="%s Logo" style="max-width: 180px; height: auto; display: block; margin: 0 auto;">
                        </div>
                    </div>
                    
                    <!-- Contenu -->
                    <div style="padding: 0 30px 25px 30px;">
                        
                        <!-- Message d'accueil -->
                        <div style="margin-bottom: 20px;">
                            <h2 style="color: #1F2937; margin: 0 0 10px 0; font-size: 22px; font-weight: 600;">
                                Authentification à deux facteurs 🔒
                            </h2>
                            <p style="color: #6B7280; margin: 0; font-size: 15px;">
                                Bonjour <strong style="color: #F97316;">%s</strong>,<br>
                                Une connexion à votre compte <strong>administrateur</strong> a été demandée. 
                                Voici votre code OTP pour finaliser l'authentification.
                            </p>
                        </div>
                        
                        <!-- Code OTP -->
                        <div style="background: linear-gradient(135deg, #3B82F6 0%%, #8B5CF6 100%%); padding: 25px; border-radius: 12px; text-align: center; margin: 25px 0;">
                            <p style="color: rgba(255, 255, 255, 0.9); margin: 0 0 12px 0; font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 1px;">
                                Code OTP de Sécurité
                            </p>
                            <div style="background: white; padding: 20px; border-radius: 10px; display: inline-block; margin: 8px 0;">
                                <div style="background: #F3F4F6; padding: 20px 30px; border-radius: 8px; display: inline-block;">
                                    <span style="font-size: 36px; font-weight: 700; color: #1F2937; font-family: 'Courier New', monospace; letter-spacing: 4px;">
                                        %s
                                    </span>
                                </div>
                            </div>
                            <!-- Durée d'expiration -->
                            <div style="margin: 16px 0 0 0; text-align: center;">
                                <span style="color: #1F2937; font-size: 13px; font-weight: 500;">
                                    <span style="margin-right: 6px;">⏱️</span>
                                    Expire dans 15 minutes
                                </span>
                            </div>
                        </div>
                        
                                               
                        <!-- Note de sécurité -->
                        <div style="background-color: #FEF3C7; padding: 16px; border-radius: 10px; margin: 20px 0; border: 1px solid #FCD34D;">
                            <div style="display: flex; gap: 10px; align-items: flex-start;">
                                <div style="color: #D97706; font-size: 18px; flex-shrink: 0; margin-top: 2px;">⚠️</div>
                                <div>
                                    <p style="color: #92400E; margin: 0 0 5px 0; font-size: 14px; font-weight: 600;">
                                        Attention - Tentative de connexion détectée
                                    </p>
                                    <p style="color: #92400E; margin: 0; font-size: 13px; line-height: 1.5;">
                                        Si vous n'êtes pas à l'origine de cette demande, <strong>ignorez cet email</strong>.
                                    </p>
                                </div>
                            </div>
                        </div>
                        
                
                        
                        <!-- Signature -->
                        <div style="padding-top: 18px; border-top: 1px solid #E5E7EB;">
                            <p style="color: #6B7280; margin: 0 0 5px 0; font-size: 14px;">
                               Bien à vous,
                            </p>
                            <p style="color: #F97316; margin: 0; font-size: 15px; font-weight: 600;">
                                L'équipe Sécurité %s
                            </p>
                        </div>
                        
                    </div>
                    
                    <!-- Pied de page -->
                    <div style="background-color: #F9FAFB; padding: 18px 30px; text-align: center; border-top: 1px solid #E5E7EB;">
                        <p style="color: #9CA3AF; margin: 0 0 8px 0; font-size: 12px;">
                            Cet email a été envoyé automatiquement suite à une tentative de connexion.
                        </p>
                        <p style="color: #9CA3AF; margin: 0; font-size: 12px;">
                            © %d %s • Sécurité des administrateurs
                        </p>
                    </div>
                    
                </div>
            </div>
        </body>
        </html>
        """,
                appName,
                appName,
                firstName,
                code,
                appName,
                java.time.Year.now().getValue(),
                appName
        );
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

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🔑 Réinitialisation de votre mot de passe - " + appName);

            // Attacher le logo si disponible
            addLogoInline(helper);

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
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
            </style>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background-color: #f8fafc; line-height: 1.6;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <!-- Carte principale -->
                <div style="background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);">
                    
                       <!-- En-tête avec logo -->
                    <div style="background: #ffffff; padding: 30px 30px 15px 30px; text-align: center;">
                        <div style="margin-bottom: 10px;">
                            <img src="cid:logo" alt="%s Logo" style="max-width: 180px; height: auto; display: block; margin: 0 auto;">
                        </div>
                    </div>
                    
                    <!-- Contenu -->
                    <div style="padding: 0 30px 25px 30px;">
                        
                        <!-- Message -->
                        <div style="margin-bottom: 20px;">
                            <h2 style="color: #1F2937; margin: 0 0 10px 0; font-size: 22px; font-weight: 600;">
                                Réinitialisation de mot de passe
                            </h2>
                            <p style="color: #6B7280; margin: 0; font-size: 15px;">
                                Bonjour <strong style="color: #F97316;">%s</strong>,<br>
                                Nous avons reçu une demande pour réinitialiser le mot de passe de votre compte.
                            </p>
                        </div>
                        <!-- Instruction pour le bouton -->
                                            <div style="text-align: center; margin: 20px 0 10px 0;">
                                                <p style="color: #4B5563; margin: 0; font-size: 14px; font-weight: 500;">
                                                    Cliquez sur le bouton ci-dessous pour procéder à la réinitialisation :
                                                </p>
                                            </div>
                        
                        <!-- Bouton principal -->
                       <div style="text-align: center; margin: 30px 0;">
                                                  <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #F97316 0%%, #FB923C 100%%); color: #111827;; text-decoration: none; padding: 18px 45px; border-radius: 12px; font-size: 16px; font-weight: 700; box-shadow: 0 6px 20px rgba(249, 115, 22, 0.3); transition: all 0.3s; border: none; cursor: pointer;">
                                                      <span style="display: flex; align-items: center; justify-content: center; gap: 10px;">
                                                          <span style="font-size: 20px;">🔑</span>
                                                          <span>RÉINITIALISER MON MOT DE PASSE</span>
                                                      </span>
                                                  </a>
                                          
                                              </div>
                        
                       <!-- Durée d'expiration -->
                            <div style="margin: 16px 0 0 0; text-align: center;">
                                <span style="color: #1F2937; font-size: 13px; font-weight: 500;">
                                    <span style="margin-right: 6px;">⏱️</span>
                                    Expire dans 15 minutes
                                </span>
                            </div>
                        
         
                              <!-- Note de sécurité -->
                        <div style="background-color: #FEF3C7; padding: 16px; border-radius: 10px; margin: 20px 0; border: 1px solid #FCD34D;">
                            <div style="display: flex; gap: 10px; align-items: flex-start;">
                                <div style="color: #D97706; font-size: 18px; flex-shrink: 0; margin-top: 2px;">⚠️</div>
                                <div>
                                    <p style="color: #92400E; margin: 0 0 5px 0; font-size: 14px; font-weight: 600;">
                                        Vous n'êtes pas à l'origine de cette demande ?
                                    </p>
                                    <p style="color: #92400E; margin: 0; font-size: 13px; line-height: 1.5;">
                                        Ignorez simplement cet email. <strong>Votre mot de passe actuel reste inchangé.</strong> Pour plus de sécurité, nous vous recommandons de vérifier l'activité de votre compte. </strong>.
                                    </p>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Signature -->
                        <div style="padding-top: 20px; border-top: 1px solid #E5E7EB;">
                            <p style="color: #6B7280; margin: 0 0 6px 0; font-size: 14px;">
                                Restez en sécurité,
                            </p>
                            <p style="color: #F97316; margin: 0; font-size: 15px; font-weight: 600;">
                                L'équipe Support %s
                            </p>
                        </div>
                        
                    </div>
                    
                    <!-- Pied de page -->
                    <div style="background-color: #F9FAFB; padding: 18px 30px; text-align: center; border-top: 1px solid #E5E7EB;">
                        <p style="color: #9CA3AF; margin: 0 0 8px 0; font-size: 12px;">
                            Cet email a été envoyé suite à une demande de réinitialisation.
                        </p>
                        <p style="color: #9CA3AF; margin: 0; font-size: 12px;">
                            © %d %s • Support sécurité
                        </p>
                    </div>
                    
                </div>
            </div>
        </body>
        </html>
        """,
                appName,
                appName,
                firstName,
                resetUrl,
                appName,
                java.time.Year.now().getValue(),
                appName
        );
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

            // Configuration de l'email
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🎉 Invitation à rejoindre " + companyName + " - " + appName);

            // Attacher le logo si disponible
            addLogoInline(helper);

            // Génération du template HTML
            String emailBody = generateEmployeeInvitationEmailTemplate(firstName, code, commercialName, companyName);

            helper.setText(emailBody, true); // true = HTML
            mailSender.send(message);

            log.info("Code d'activation envoyé avec succès au salarié: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'invitation", e);
        }
    }

    /**
     * Génère le template HTML pour l'email d'invitation salarié
     *
     * @param firstName      Le prénom du salarié
     * @param code           Le code d'activation
     * @param commercialName Le nom du commercial
     * @param companyName    Le nom de l'entreprise
     * @return Le template HTML formaté
     */
    private String generateEmployeeInvitationEmailTemplate(String firstName, String code, String commercialName, String companyName) {
        return String.format("""
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Invitation Salarié - %s</title>
        <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
            
            /* Fallback pour les clients email */
            @media only screen and (max-width: 600px) {
                .container {
                    width: 100%% !important;
                    padding: 10px !important;
                }
                .button {
                    padding: 14px 30px !important;
                    font-size: 14px !important;
                }
            }
        </style>
    </head>
    <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background-color: #f8fafc; line-height: 1.6;">
        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
            <!-- Carte principale -->
            <div style="background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);">
                
                <!-- En-tête avec logo -->
                <div style="background: #ffffff; padding: 30px 30px 15px 30px; text-align: center; border-bottom: 1px solid #E5E7EB;">
                    <div style="margin-bottom: 10px;">
                        <img src="cid:logo" alt="%s Logo" style="max-width: 180px; height: auto; display: block; margin: 0 auto;">
                    </div>
                </div>
                
                <!-- Contenu -->
                <div style="padding: 0 30px 25px 30px;">
                    
                    <!-- Message de bienvenue -->
                    <div style="margin-bottom: 20px;">
                        <h2 style="color: #1F2937; margin: 0 0 10px 0; font-size: 22px; font-weight: 600;">
                            Bienvenue sur %s
                        </h2>
                        <p style="color: #6B7280; margin: 0; font-size: 15px;">
                            Bonjour <strong style="color: #F97316;">%s</strong>,<br>
                            Votre entreprise <strong>%s</strong> utilise désormais notre plateforme %s pour ses achats. 
                            Notre commercial <strong>%s</strong> a paramétré votre compte utilisateur.
                        </p>
                    </div>
                    
                  
                    
                    <!-- Instruction principale -->
                    <div style="text-align: center; margin: 30px 0;">
                        <p style="color: #1F2937; margin: 0 0 20px 0; font-size: 16px; font-weight: 600;">
                            Utilisez le code ci-dessous pour activer votre compte
                        </p>

                        <!-- Code d'activation -->
                        <div style="background: #F3F4F6; padding: 18px 30px; border-radius: 12px; display: inline-block;">
                            <span style="font-size: 32px; font-weight: 700; color: #1F2937; font-family: 'Courier New', monospace; letter-spacing: 6px;">
                                %s
                            </span>
                        </div>

                        <!-- Informations supplémentaires -->
                        <div style="margin-top: 15px;">
                            <p style="color: #6B7280; font-size: 13px; margin: 8px 0; font-weight: 500;">
                                <span style="margin-right: 6px;">⏱️</span>
                                Code valide pendant 15 minutes
                            </p>
                        </div>
                    </div>
                    
                             <!-- Note de sécurité -->
                        <div style="background-color: #FEF3C7; padding: 16px; border-radius: 10px; margin: 20px 0; border: 1px solid #FCD34D;">
                            <div style="display: flex; gap: 10px; align-items: flex-start;">
                                <div style="color: #D97706; font-size: 18px; flex-shrink: 0; margin-top: 2px;">⚠️</div>
                                <div>
                                    <p style="color: #92400E; margin: 0 0 5px 0; font-size: 14px; font-weight: 600;">
                                        Vous ne reconnaissez pas cette invitation ?
                                    </p>
                                     <p style="color: #991B1B; margin: 0; font-size: 14px; line-height: 1.5;">
                                    Ignorez simplement cet email. Si vous pensez qu'il s'agit d'une erreur, contactez votre responsable ou notre support.
                                </p>
                                </div>
                            </div>
                        </div>
                    
                 
               
                    <!-- Signature -->
                    <div style="padding-top: 20px; border-top: 1px solid #E5E7EB;">
                        <p style="color: #6B7280; margin: 0 0 6px 0; font-size: 14px;">
                            Bienvenue dans notre communauté,
                        </p>
                        <p style="color: #F97316; margin: 0; font-size: 15px; font-weight: 600;">
                            L'équipe Support %s
                        </p>
                    </div>
                    
                </div>
                
                <!-- Pied de page -->
                <div style="background-color: #F9FAFB; padding: 18px 30px; text-align: center; border-top: 1px solid #E5E7EB;">
                    <p style="color: #9CA3AF; margin: 0 0 8px 0; font-size: 12px;">
                        Cet email a été envoyé suite à une invitation d'un commercial.
                    </p>
                    <p style="color: #9CA3AF; margin: 0; font-size: 12px;">
                        © %d %s • Support salariés
                    </p>
                </div>
                
            </div>
        </div>
    </body>
    </html>
    """,
                appName,                    // %s - titre
                appName,                    // %s - alt du logo
                appName,                    // %s - nom de la plateforme
                firstName,                  // %s - prénom du salarié
                companyName,                // %s - nom de l'entreprise
                appName,                    // %s - nom plateforme (dans le message)
                commercialName,             // %s - nom du commercial

                code,                       // %s - code d'activation
                appName,                    // %s - nom dans signature
                java.time.Year.now().getValue(), // %d - année
                appName                     // %s - nom dans footer
        );
    }

    // ============================================================================
    // 📧 ENVOI D'EMAILS - ACTIVATION LIVREUR
    // ============================================================================

    @Override
    public void sendDriverActivationCode(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Attacher le logo si disponible
            addLogoInline(helper);

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("🚚 Code d'activation Livreur - " + appName);

            String emailBody = generateDriverActivationEmailTemplate(firstName, code);

            helper.setText(emailBody, true);
            mailSender.send(message);

            log.info("Code d'activation livreur envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code d'activation livreur à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email d'activation", e);
        }
    }

    /**
     * Génère le template HTML pour l'email d'activation livreur
     *
     * @param firstName Le prénom du livreur
     * @param code      Le code d'activation à 4 chiffres
     * @return Le template HTML formaté
     */
    private String generateDriverActivationEmailTemplate(String firstName, String code) {
        return String.format("""
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Code d'activation Livreur - %s</title>
    </head>
    <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
        <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f4f4;">
            <tr>
                <td style="padding: 10px 0;"> 
                    <table role="presentation" style="width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        
                        <!-- Header -->
                        <tr>
                            <td style="background: linear-gradient(135deg, #FF6B35 0%%, #F7931E 100%%); padding: 25px 20px; text-align: center; border-radius: 8px 8px 0 0;">
                                <img src="cid:logo" alt="%s Logo" style="max-width: 150px; height: auto; margin-bottom: 10px;">
                            </td>
                        </tr>
                        
                        <!-- Contenu principal -->
                        <tr>
                            <td style="padding: 25px 20px;"> 
                                <h2 style="color: #333333; margin: 0 0 15px 0; font-size: 22px; font-weight: 600;">
                                    Bonjour %s,
                                </h2>
                                
                                <p style="color: #666666; font-size: 15px; line-height: 1.5; margin: 0 0 15px 0;">
                                    Bienvenue dans l'équipe de livraison de <strong>%s</strong> ! 
                                    Pour activer votre compte livreur et commencer vos tournées, 
                                    utilisez le code d'activation ci-dessous.
                                </p>
                                
                                <!-- Code d'activation -->
                                <div style="background: linear-gradient(135deg, #FF6B35 0%%, #F7931E 100%%); padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
                                    <p style="color: #333333; margin: 0 0 8px 0; font-size: 13px; text-transform: uppercase; letter-spacing: 1px;">
                                        🔐 Votre code d'activation
                                    </p>
                                    <div style="background-color: #ffffff; padding: 15px; border-radius: 6px; display: inline-block; margin: 8px 0;">
                                        <span style="color: #FF6B35; font-size: 36px; font-weight: 700; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                            %s
                                        </span>
                                    </div>
                                    <p style="color: #333333; margin: 10px 0 0 0; font-size: 12px;">
                                        ⏱️ Ce code expire dans 15 minutes
                                    </p>
                                </div>
                                
                                <!-- Sécurité -->
                                <div style="background-color: #fff3cd; padding: 12px; border-radius: 6px; border-left: 4px solid #ffc107; margin: 20px 0;">
                                    <p style="color: #856404; margin: 0; font-size: 13px; line-height: 1.5;">
                                        <strong>🔒 Sécurité :</strong> Ne partagez jamais ce code. 
                                        L'équipe %s ne vous demandera jamais votre code par email ou téléphone.
                                    </p>
                                </div>
                                
                                <p style="color: #666666; font-size: 14px; line-height: 1.5; margin: 15px 0 0 0;">
                                    Si vous n'avez pas demandé ce code, ignorez cet email.
                                </p>
                                
                                <p style="color: #666666; font-size: 14px; line-height: 1.5; margin: 15px 0 0 0;">
                                    Bonne route ! 🚚<br>
                                    <strong style="color: #333333;">L'équipe %s</strong>
                                </p>
                            </td>
                        </tr>
                        
                        <!-- Footer -->
                        <tr>
                            <td style="background-color: #f8f9fa; padding: 20px; text-align: center; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                                <p style="color: #999999; font-size: 11px; margin: 0 0 8px 0;">
                                    Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                </p>
                                <p style="color: #999999; font-size: 11px; margin: 0;">
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
    """, appName, appName, firstName, appName, code, appName, appName,
                java.time.Year.now().getValue(), appName);
    }

    @Override
    public void sendTourProposalToDriver(String email, String tourNumber, LocalDate deliveryDate, String timeSlot, String driverName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("✅ Tournée Proposée - " + tourNumber);

            // Attacher le logo si disponible
            addLogoInline(helper);

            // Template HTML
            String emailBody = generateTourProposalTemplate(driverName, tourNumber,
                    deliveryDate, timeSlot);

            helper.setText(emailBody, true);
            mailSender.send(message);

            log.info("Notification tournée envoyée à: {}", email);

        } catch (Exception e) {
            log.error("Erreur envoi notification tournée à {}: {}", email, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer la notification", e);
        }
    }

    private String generateTourProposalTemplate(String driverName, String tourNumber,
                                                    LocalDate deliveryDate, String timeSlot) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Tournée Proposée</title>
        </head>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: 0 auto; background-color: white; 
                       border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="cid:logo" alt="Logo" style="max-width: 150px;">
                </div>
                
                <h2 style="color: #333; text-align: center;">🚚 Tournée Proposée</h2>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Bonjour <strong>%s</strong>,
                </p>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Votre tournée <strong>%s</strong> a été proposée par le responsable logistique.
                </p>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 6px; 
                           margin: 20px 0; border-left: 4px solid #28a745;">
                    <p style="margin: 5px 0; color: #333;">
                        <strong>📅 Date:</strong> %s
                    </p>
                    <p style="margin: 5px 0; color: #333;">
                        <strong>⏰ Créneau:</strong> %s
                    </p>
                    <p style="margin: 5px 0; color: #333;">
                        <strong>✅ Statut:</strong> Confirmée
                    </p>
                </div>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Connectez-vous à votre espace chauffeur pour consulter les détails 
                    des commandes et l'itinéraire.
                </p>
            
                
                <p style="color: #999; font-size: 14px; text-align: center; margin-top: 30px;">
                    Ceci est une notification automatique, merci de ne pas y répondre.<br>
                    © %d %s
                </p>
            </div>
        </body>
        </html>
        """,
                driverName,
                tourNumber,
                deliveryDate,
                timeSlot,
                java.time.Year.now().getValue(),
                appName
        );
    }

    @Override
    public void sendTourCancellationToDriver(String email, String tourNumber, String reason, String driverName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("❌ Tournée annulée - " + tourNumber);

            // Attacher le logo si disponible
            addLogoInline(helper);

            // Template HTML
            String emailBody = generateTourCancellationTemplate(driverName, tourNumber, reason);

            helper.setText(emailBody, true);
            mailSender.send(message);

            log.info("Notification d'annulation envoyée à: {}", email);

        } catch (Exception e) {
            log.error("Erreur envoi notification annulation à {}: {}", email, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer la notification d'annulation", e);
        }
    }

    private String generateTourCancellationTemplate(String driverName, String tourNumber, String reason) {
        return String.format("""
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Tournée Annulée</title>
        </head>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: 0 auto; background-color: white; 
                       border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="cid:logo" alt="Logo" style="max-width: 150px;">
                </div>
                
                <h2 style="color: #333; text-align: center;">❌ Tournée annulée</h2>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Bonjour <strong>%s</strong>,
                </p>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Nous vous informons que votre tournée <strong>%s</strong> 
                    a été annulée par le responsable logistique.
                </p>
                
                <div style="background-color: #fff3cd; padding: 15px; border-radius: 6px; 
                           margin: 20px 0; border-left: 4px solid #dc3545;">
                    <p style="margin: 5px 0; color: #856404;">
                        <strong>📋 Référence:</strong> %s
                    </p>
                    <p style="margin: 5px 0; color: #856404;">
                        <strong>📝 Motif d'annulation:</strong> %s
                    </p>
                    <p style="margin: 5px 0; color: #856404;">
                        <strong>❌ Statut:</strong> Annulée
                    </p>
                </div>
                
                <p style="color: #666; font-size: 16px; line-height: 1.5;">
                    Cette tournée a été retirée de votre planning de livraison.
                </p>
                                
                <p style="color: #999; font-size: 14px; text-align: center; margin-top: 30px;">
                    Ceci est une notification automatique, merci de ne pas y répondre.<br>
                    © %d %s
                </p>
            </div>
        </body>
        </html>
        """,
                driverName,
                tourNumber,
                tourNumber, // Répété pour le bloc info
                reason,
                java.time.Year.now().getValue(),
                appName
        );
    }

    // ============================================================================
    // 📧 NOTIFICATION SIGNALEMENT LIVREUR → RESPONSABLE LOGISTIQUE
    // ============================================================================

    @Override
    public void sendDriverReportToLogisticsManager(String rlEmail, String driverName, String reportTypeLabel, String comment, String orderNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(rlEmail);
            helper.setSubject("⚠️ Signalement livreur - " + reportTypeLabel + " - " + appName);

            addLogoInline(helper);

            String emailBody = generateDriverReportNotificationTemplate(driverName, reportTypeLabel, comment, orderNumber);
            helper.setText(emailBody, true);
            mailSender.send(message);

            log.info("Notification signalement livreur envoyée au RL: {}", rlEmail);
        } catch (Exception e) {
            log.error("Erreur envoi notification signalement à {}: {}", rlEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer la notification au responsable logistique", e);
        }
    }

    private String generateDriverReportNotificationTemplate(String driverName, String reportTypeLabel, String comment, String orderNumber) {
        String orderInfo = (orderNumber != null && !orderNumber.isBlank())
                ? "<p style=\"margin: 5px 0; color: #333;\"><strong>📦 Commande:</strong> " + orderNumber + "</p>"
                : "";
        String commentBlock = (comment != null && !comment.isBlank())
                ? "<div style=\"background-color: #f8f9fa; padding: 12px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #F97316;\">"
                + "<p style=\"margin: 0; color: #333; font-size: 14px;\">" + comment + "</p></div>"
                : "";
        return String.format("""
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Signalement livreur</title>
        </head>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: 0 auto; background-color: white;
                       border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="cid:logo" alt="Logo" style="max-width: 150px;">
                </div>
                <h2 style="color: #333; text-align: center;">⚠️ Signalement d'un livreur</h2>
                <p style="color: #666; font-size: 16px;">Un livreur a soumis un signalement depuis l'application.</p>
                <div style="background-color: #fff3cd; padding: 15px; border-radius: 6px;
                           margin: 20px 0; border-left: 4px solid #F97316;">
                    <p style="margin: 5px 0; color: #333;"><strong>👤 Livreur:</strong> %s</p>
                    <p style="margin: 5px 0; color: #333;"><strong>📋 Nature:</strong> %s</p>
                    %s
                </div>
                %s
                <p style="color: #999; font-size: 14px; text-align: center; margin-top: 30px;">
                    Notification automatique - %s<br>© %d %s
                </p>
            </div>
        </body>
        </html>
        """,
                driverName,
                reportTypeLabel,
                orderInfo,
                commentBlock,
                appName,
                java.time.Year.now().getValue(),
                appName
        );
    }

    private void addLogoInline(MimeMessageHelper helper) {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
            if (logoResource.exists() && logoResource.isReadable()) {
                helper.addInline("logo", logoResource);
            } else {
                log.warn("Logo email introuvable: static/images/logo.png");
            }
        } catch (Exception e) {
            log.warn("Impossible d'attacher le logo email: {}", e.getMessage());
        }
    }




}

