package com.example.restaurant.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

    @Value("${app.url}")
    private String _appUrl;

    @Async
    public void sendActivationEmail(String to, String username, String token) {
        try
        {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("activationUrl",  token);

            String html = _templateEngine.process("emails/activation", context);

            MimeMessage message = _mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Qui la Carne - Confirm your account");
            helper.setText(html, true);

            _mailSender.send(message);
            log.info("Confirmation email sent to {}" + to);
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
    }

    @Async
    public void sendResetPasswordEmail(String to, String username, String token) {
        try
        {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetUrl",  token);
            String html = _templateEngine.process("emails/reset-password", context);

            MimeMessage message = _mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Qui la Carne - Reset your password");
            helper.setText(html, true);

            _mailSender.send(message);
            log.info("Reset password email sent to {}" + to);
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
    }
}
