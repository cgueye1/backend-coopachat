
package com.example.coopachat.services.auth;

import com.example.coopachat.enums.PasswordResetChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Service d'envoi d'emails — texte brut et, pour la réinitialisation, variante HTML avec lien cliquable.
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

    @Value("${app.frontend.reset-password-url:https://coopachat.innovimpactdev.cloud/create-password?token=}")
    private String resetPasswordUrl;

    @Value("${app.frontend.reset-password-url-mobile:https://coopachat.innovimpactdev.cloud/reset-password?token=}")
    private String resetPasswordUrlMobile;

    @Value("${app.frontend.activation-url:https://coopachat.innovimpactdev.cloud/create-password?token=}")
    private String activationUrl;

    @Value("${app.frontend.activation-url-mobile:https://coopachat.innovimpactdev.cloud/set-password?token=}")
    private String activationUrlMobile;

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

    @Override
    public void sendActivationLink(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Activation de votre compte - " + appName);

            String webLink = activationUrl + code + "&email=" + email;

            String body = String.format(
                "Bonjour %s,%n%nVotre compte %s a été créé. Pour définir votre mot de passe et activer votre compte, cliquez sur le lien suivant : %n%s%n%nCe lien expire dans 15 minutes.%n%nL'équipe %s",
                firstName != null ? firstName : "", appName, webLink, appName
            );

            String safeName = HtmlUtils.htmlEscape(firstName != null ? firstName : "");
            String safeApp = HtmlUtils.htmlEscape(appName);
            String safeWeb = HtmlUtils.htmlEscape(webLink);

            String htmlBody = String.format(
                "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                        "<p>Bonjour %s,</p>" +
                        "<p>Votre compte <strong>%s</strong> a été créé par un administrateur.</p>" +
                        "<p>Identifiant de connexion : <strong>%s</strong></p>" +
                        "<p>Pour définir votre mot de passe et activer votre compte, cliquez sur le bouton ci-dessous :</p>" +
                        "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#F68647;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Activer mon compte</a></p>" +
                        "<p style=\"font-size:13px;color:#555;\">Si le bouton ne fonctionne pas, copiez cette adresse dans votre navigateur :</p>" +
                        "<p style=\"word-break:break-all;font-size:11px;\">%s</p>" +
                        "<p>Ce lien expire dans <strong>15 minutes</strong>.</p>" +
                        "<p>L'équipe %s</p>" +
                        "</body></html>",
                safeName, safeApp, HtmlUtils.htmlEscape(email), safeWeb, safeWeb, safeApp);

            helper.setText(body, htmlBody);
            mailSender.send(message);

            log.info("Lien d'activation envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien d'activation à {}: {}", 
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
    public void sendPasswordResetLink(String email, String token, String firstName, PasswordResetChannel channel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Réinitialisation de mot de passe - " + appName);

            // Canal effectif : si l'appelant n'a pas passé de canal (null), on considère que c'est une demande "navigateur" (WEB).
            PasswordResetChannel resolved = channel != null ? channel : PasswordResetChannel.WEB;

            // Construction de l'URL complète du lien dans l'email :
            // - MOBILE → préfixe deep link  + token UUID collé à la fin ;
            // - WEB     → URL https du front (ex. .../create-password?token=) + token.
            //resetUrl = resetPasswordUrlMobile + token si resolved = PasswordResetChannel.MOBILE sinon resetPasswordUrl + token(le web )
            String resetUrl = buildResetUrl(resolved, token);

            // Texte du mail : même information, formulations différentes selon le canal (app vs navigateur).
            String body = resolved == PasswordResetChannel.MOBILE
                    ? String.format(
                    "Bonjour %s,%n%nPour réinitialiser votre mot de passe, ouvrez ce lien depuis l'application %s :%n%n%s%n%nExpire dans 15 minutes.%n%nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email.%n%nL'équipe Support %s",
                    firstName != null ? firstName : "", appName, resetUrl, appName)
                    : String.format(
                    "Bonjour %s,%n%nPour réinitialiser votre mot de passe, cliquez ou copiez ce lien :%n%n%s%n%nExpire dans 15 minutes.%n%nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email. Votre mot de passe actuel reste inchangé.%n%nL'équipe Support %s",
                    firstName != null ? firstName : "", resetUrl, appName);

            String safeName = HtmlUtils.htmlEscape(firstName != null ? firstName : "");
            String safeApp = HtmlUtils.htmlEscape(appName);
            String hrefUrl = HtmlUtils.htmlEscape(resetUrl);

            String htmlBody = resolved == PasswordResetChannel.MOBILE
                    ? String.format(
                    "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                            "<p>Bonjour %s,</p>" +
                            "<p>Pour réinitialiser votre mot de passe depuis l'application <strong>%s</strong>, utilisez le lien ci-dessous :</p>" +
                            "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#ea580c;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Ouvrir dans l'application</a></p>" +
                            "<p style=\"word-break:break-all;font-size:13px;color:#555;\">%s</p>" +
                            "<p>Ce lien expire dans <strong>15 minutes</strong>.</p>" +
                            "<p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>" +
                            "<p>L'équipe Support %s</p>" +
                            "</body></html>",
                    safeName, safeApp, hrefUrl, hrefUrl, safeApp)
                    : String.format(
                    "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                            "<p>Bonjour %s,</p>" +
                            "<p>Pour réinitialiser votre mot de passe, cliquez sur le bouton ci-dessous :</p>" +
                            "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#ea580c;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Réinitialiser mon mot de passe</a></p>" +
                            "<p style=\"font-size:13px;color:#555;\">Si le bouton ne fonctionne pas, copiez cette adresse dans votre navigateur :</p>" +
                            "<p style=\"word-break:break-all;font-size:13px;\">%s</p>" +
                            "<p>Ce lien expire dans <strong>15 minutes</strong>.</p>" +
                            "<p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email. Votre mot de passe actuel reste inchangé.</p>" +
                            "<p>L'équipe Support %s</p>" +
                            "</body></html>",
                    safeName, hrefUrl, hrefUrl, safeApp);

            helper.setText(body, htmlBody);
            mailSender.send(message);

            log.info("Lien de réinitialisation de mot de passe envoyé avec succès à: {}", email);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien de réinitialisation à {}: {}",
                    email, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
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

   

    @Override
    public void sendDriverActivationLink(String email, String code, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Activation de votre compte livreur - " + appName);

            String mobileLink = activationUrlMobile + code + "&email=" + email;
            String safeName = HtmlUtils.htmlEscape(firstName != null ? firstName : "Livreur");
            String safeApp = HtmlUtils.htmlEscape(appName);
            String safeMobile = HtmlUtils.htmlEscape(mobileLink);

            String htmlBody = String.format(
                "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                "<p>Bonjour %s,</p>" +
                "<p>Vous avez été ajouté en tant que livreur sur la plateforme <strong>%s</strong>.</p>" +
                "<p>Identifiant de connexion : <strong>%s</strong></p>" +
                "<p>Pour commencer à utiliser votre compte, vous devez définir votre mot de passe depuis l'application mobile :</p>" +
                "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#2B3674;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Ouvrir dans l'application</a></p>" +
                "<p style=\"font-size:13px;color:#555;\">Si le bouton ne fonctionne pas, copiez cette adresse sur votre téléphone :</p>" +
                "<p style=\"word-break:break-all;font-size:13px;\">%s</p>" +
                "<p>Cordialement,<br>L'équipe Support %s</p>" +
                "</body></html>",
                safeName, safeApp, HtmlUtils.htmlEscape(email), safeMobile, safeMobile, safeApp);

            helper.setText("", htmlBody);
            mailSender.send(message);

            log.info("Lien d'activation livreur envoyé avec succès à: {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien d'activation livreur à {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendEmployeeActivationLink(String email, String code, String firstName, String commercialName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Invitation à rejoindre " + companyName + " - " + appName);

            String mobileLink = activationUrlMobile + code + "&email=" + email;
            String safeName = HtmlUtils.htmlEscape(firstName != null ? firstName : "");
            String safeCompany = HtmlUtils.htmlEscape(companyName);
            String safeCommercial = HtmlUtils.htmlEscape(commercialName);
            String safeApp = HtmlUtils.htmlEscape(appName);
            String safeMobile = HtmlUtils.htmlEscape(mobileLink);

            String htmlBody = String.format(
                "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                "<p>Bonjour %s,</p>" +
                "<p>Votre entreprise <strong>%s</strong> utilise désormais <strong>%s</strong> pour ses achats.</p>" +
                "<p>Le commercial <strong>%s</strong> a paramétré votre compte salarié.</p>" +
                "<p>Identifiant de connexion : <strong>%s</strong></p>" +
                "<p>Pour finaliser votre inscription et accéder à vos avantages, veuillez définir votre mot de passe depuis l'application mobile :</p>" +
                "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#2B3674;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Ouvrir dans l'application</a></p>" +
                "<p style=\"word-break:break-all;font-size:13px;\">%s</p>" +
                "<p>Cordialement,<br>L'équipe Support %s</p>" +
                "</body></html>",
                safeName, safeCompany, safeApp, safeCommercial, HtmlUtils.htmlEscape(email), safeMobile, safeMobile, safeApp);

            helper.setText("", htmlBody);
            mailSender.send(message);

            log.info("Lien d'activation salarié envoyé avec succès à: {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien d'activation salarié à {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendCompanyActivationLink(String email, String code, String contactName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Bienvenue sur " + appName + " - " + companyName);

            String webLink = activationUrl + code + "&email=" + email;
            String safeName = HtmlUtils.htmlEscape(contactName != null ? contactName : "Représentant");
            String safeCompany = HtmlUtils.htmlEscape(companyName);
            String safeApp = HtmlUtils.htmlEscape(appName);
            String safeWeb = HtmlUtils.htmlEscape(webLink);

            String htmlBody = String.format(
                "<html><body style=\"font-family:sans-serif;font-size:15px;line-height:1.5;color:#333;\">" +
                "<p>Bonjour %s,</p>" +
                "<p>Bienvenue sur <strong>%s</strong> ! Votre espace entreprise pour <strong>%s</strong> a été créé.</p>" +
                "<p>Identifiant de connexion : <strong>%s</strong></p>" +
                "<p>Pour commencer à gérer votre entreprise et vos salariés, veuillez définir votre mot de passe :</p>" +
                "<p style=\"margin:24px 0;\"><a href=\"%s\" style=\"display:inline-block;padding:12px 24px;background-color:#ea580c;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;\">Activer l'Espace Entreprise</a></p>" +
                "<p style=\"word-break:break-all;font-size:13px;\">%s</p>" +
                "<p>Cordialement,<br>L'équipe Support %s</p>" +
                "</body></html>",
                safeName, safeApp, safeCompany, HtmlUtils.htmlEscape(email), safeWeb, safeWeb, safeApp);

            helper.setText("", htmlBody);
            mailSender.send(message);

            log.info("Lien d'activation entreprise envoyé avec succès à: {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du lien d'activation entreprise à {}: {}", email, e.getMessage());
        }
    }

      private String buildResetUrl(PasswordResetChannel channel, String token) {
        String effectiveBaseUrl = channel == PasswordResetChannel.MOBILE
                ? resetPasswordUrlMobile
                : resetPasswordUrl;

        if (!StringUtils.hasText(effectiveBaseUrl)) {
            log.error("URL de reset {} absente de la configuration !", channel);
            throw new RuntimeException("Configuration manquante pour le lien de réinitialisation");
        }

        String finalUrl = effectiveBaseUrl + token;
        log.info("Lien de reset généré pour {}: {}", channel, finalUrl);
        return finalUrl;
    }
}

