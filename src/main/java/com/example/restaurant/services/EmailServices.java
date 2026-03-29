package com.example.restaurant.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServices {
    private final JavaMailSender _mailSender;
    private final TemplateEngine _templateEngine;

    @Value("${app.cors.allowed-origins}")
    private String _appUrl;

    @Async
    public void sendActivationEmail(String to, String username, String token) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("activationUrl", token);

        sendHtmlEmail(
                to,
                "Qui la Carne - Confirm your account",
                "emails/activation",
                context,
                "Confirmation"
        );
    }

    @Async
    public void sendResetPasswordEmail(String to, String username, String token) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("resetUrl", token);

        sendHtmlEmail(
                to,
                "Qui la Carne - Reset your password",
                "emails/reset-password",
                context,
                "Reset password");
    }

    @Async
    public void sendEmailChangeVerification(String to, String token) {
        Context context = new Context();
        context.setVariable("validationUrl", token);

        sendHtmlEmail(
                to,
                "Qui la Carne - Confirm your new email address",
                "emails/email-update",
                context,
                "Email change verification"
        );
    }

    @Async
    public void sendEmailSetBan(String to, String userName, String reason) {
        Context context = new Context();
        context.setVariable("username", userName);
        context.setVariable("reason", reason);

        sendHtmlEmail(
                to,
                "Qui la Carne - Account Suspension Notice",
                "emails/set_ban",
                context,
                "Create Ban"
        );
    }

    private void sendHtmlEmail(
            String to,
            String subject,
            String templateName,
            Context context,
            String actionName
    ) {
        try {
            String html = _templateEngine.process(templateName, context);

            MimeMessage message = _mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            _mailSender.send(message);
            log.info("{} email sent to {}", actionName, to);
        } catch (Exception ex) {
            log.error("Failed to send {} email to {}: {}", actionName, to, ex.getMessage(), ex);
        }
    }
}
