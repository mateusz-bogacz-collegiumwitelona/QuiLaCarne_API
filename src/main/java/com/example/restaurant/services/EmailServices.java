package com.example.restaurant.services;

import com.example.restaurant.dto.domain.EmailDomain;
import com.example.restaurant.queue.EmailQueueProducer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServices {
  private final EmailQueueProducer _emailQueue;

  @Value("${app.cors.allowed-origins}")
  private String _appUrl;

  @Async
  public void sendActivationEmail(String to, String username, String token) {
    validateInputs(to, username, token);

    Map<String, Object> variables =
        Map.of(
            "username", username,
            "activationUrl", token);

    enqueueHtmlEmail(to, "Qui la Carne - Confirm your account", "emails/activation", variables);
    log.info("Activation email sent to {}", to);
  }

  @Async
  public void sendResetPasswordEmail(String to, String username, String token) {
    Map<String, Object> variables =
        Map.of(
            "username", username,
            "resetUrl", token);

    enqueueHtmlEmail(to, "Qui la Carne - Reset your password", "emails/reset-password", variables);
    log.info("Reset email sent to {}", to);
  }

  @Async
  public void sendEmailChangeVerification(String to, String token) {
    Map<String, Object> variables = Map.of("validationUrl", token);

    enqueueHtmlEmail(
        to, "Qui la Carne - Confirm your new email address", "emails/email-update", variables);
    log.info("Email change verification email sent to {}", to);
  }

  @Async
  public void sendEmailSetBan(String to, String userName, String reason) {
    validateInputs(to, userName, reason);

    Map<String, Object> variables =
        Map.of(
            "username", userName,
            "reason", reason);

    enqueueHtmlEmail(to, "Qui la Carne - Account Suspension Notice", "emails/set_ban", variables);
    log.info("Email set ban email sent to {}", to);
  }

  private void validateInputs(String to, String... params) {
    if (to == null || to.isBlank() || !to.contains("@")) {
      throw new IllegalArgumentException("A valid destination email is required");
    }
    for (String param : params) {
      if (param == null || param.isBlank()) {
        throw new IllegalArgumentException("Required email context variable is missing or blank");
      }
    }
  }

  private void enqueueHtmlEmail(
      String to, String subject, String templateName, Map<String, Object> variables) {
    EmailDomain job = new EmailDomain(to, subject, templateName, variables);
    _emailQueue.enqueueEmail(job);
  }
}
